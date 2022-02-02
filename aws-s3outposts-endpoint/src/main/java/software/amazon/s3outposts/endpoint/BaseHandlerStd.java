package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.EndpointAccessType;
import software.amazon.awssdk.services.s3outposts.model.S3OutpostsException;
import software.amazon.awssdk.services.s3outposts.model.S3OutpostsRequest;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    // Default Access Type
    protected static final String DEFAULT_ACCESS_TYPE = EndpointAccessType.PRIVATE.toString();
    // Maximum number of retries to stabilize an operation
    protected static final Integer NUMBER_OF_STABILIZATION_RETRIES = 100;
    // Each Stabilization attempt will be called with a delay of 15s
    // We set timeout to 30m (max allowed stabilization time)
    protected static final Delay STABILIZATION_DELAY = Constant.of()
            .timeout(Duration.ofMinutes(30L))
            .delay(Duration.ofSeconds(15L))
            .build();

    // Error messages (returned to the customer)
    protected static final String INVALID_INPUT = "OutpostId, SecurityGroupId, SubnetId are required parameters.";
    protected static final String ENDPOINT_ARN_REQD = "Endpoint ARN is required.";
    protected static final String ENDPOINT_ARN_NOT_FOUND = "Endpoint with provided ARN not found.";
    protected static final String INVALID_ACCESS_TYPE = "AccessType is invalid.";
    protected static final String MAX_RETRY_ATTEMPTS = "Maximum number of Stabilization attempts reached. Returning SUCCESS.";

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger);


    /**
     * Common error handling function. Used by all handlers of the Endpoint resource.
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/S3OutpostsException.html
     *
     * @param request
     * @param exception
     * @param proxyClient
     * @param resourceModel
     * @param callbackContext
     * @return
     */
    public ProgressEvent<ResourceModel, CallbackContext> handleError(
            final S3OutpostsRequest request,
            final Exception exception,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final ResourceModel resourceModel,
            final CallbackContext callbackContext
    ) throws Exception {

        try {
            throw exception;
        } catch (S3OutpostsException e) {

            switch (e.statusCode()) {
                case 400:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, e.getMessage());
                case 403:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.AccessDenied, e.getMessage());
                case 404:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
                case 409:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ResourceConflict, e.getMessage());
                case 500:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ServiceInternalError, e.getMessage());
                default:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());
            }

        } catch (SdkException e) {

            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());

        }

    }

    /**
     * Calls the API listEndpoints.
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/ListEndpointsRequest.html
     * Gets called from the ListHandler and the ReadHandler.
     */
    protected ProgressEvent<ResourceModel, CallbackContext> listEndpoints(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3OutpostsClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        // The ResourceModel contains the NavyId as OutpostId when running on EC2 outposts and not actual Outposts
        // OutpostId should be set when:
        //      Create -> stabilizedOnCreate -> List
        //      ContractTest: Create -> List
        //      ContractTest: Create -> Read

        final boolean ec2OutpostIdPresent;
        final String outpostId;

        if (model != null && model.getArn() != null) {
            logger.log(String.format("%s::Read::listEndpoints, arn: %s", ResourceModel.TYPE_NAME, model.getArn()));
            outpostId = EndpointArnFields.splitArn(model.getArn()).outpostId;
            ec2OutpostIdPresent = !outpostId.equals("ec2") && !outpostId.startsWith("op-");
        } else {
            outpostId = null;
            ec2OutpostIdPresent = false;
        }

        if (request.getNextToken() != null) {
            logger.log(String.format("%s::List::listEndpoints - Account: %s, NextToken: %s",
                    ResourceModel.TYPE_NAME, request.getAwsAccountId(), request.getNextToken()));
        } else {
            logger.log(String.format("%s::Read/List::listEndpoints - Account: %s",
                    ResourceModel.TYPE_NAME, request.getAwsAccountId()));
        }

        return proxy.initiate("AWS-S3Outposts-Endpoint::Read/List::ListEndpoints", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToSdkListEndpointsRequest(request.getNextToken())
                )
                .makeServiceCall((listEndpointsRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(listEndpointsRequest,
                                s3ControlProxyClient.client()::listEndpoints)
                )
                .handleError(this::handleError)
                .done((listEndpointsResponse) ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                // We have to keep the model intact in the ProgressEvent object for the sake of the ReadHandler.
                                // When a user calls the ReadHandler, they provide the Arn in the model, and we don't want to lose this information.
                                .resourceModel(model)
                                .resourceModels(
                                        listEndpointsResponse.endpoints()
                                                .stream()
                                                .map(endpoint -> {
                                                    if (ec2OutpostIdPresent) {
                                                        return Translator.translateFromSdkEc2Endpoint(endpoint, outpostId);
                                                    } else {
                                                        return Translator.translateFromSdkEndpoint(endpoint);
                                                    }
                                                })
                                                .collect(Collectors.toList())
                                )
                                .nextToken(listEndpointsResponse.nextToken())
                                .status(OperationStatus.IN_PROGRESS)
                                .build()

                );

    }

    /**
     * In this routine, we traverse the List<ResourceModel> returned by calling `listEndpoints`.
     * While traversing, we search for an Arn match, using the Arn provided by the caller in the model.
     * Once we get an Arn match, we populate the model and return.
     *
     * NOTE: Endpoint resource does not support a GetEndpoint API call. That's the reason why we have to call
     * the ListEndpoints API and use its output to populate the model in the ReadHandler.
     */
    protected ProgressEvent<ResourceModel, CallbackContext> getEndpoint(
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        final ResourceModel requestResourceModel = progress.getResourceModel();
        final String arn = requestResourceModel.getArn();
        final List<ResourceModel> modelList = progress.getResourceModels();
        final CallbackContext context = progress.getCallbackContext();

        logger.log(String.format("%s::Read::getEndpoint - Finding endpoint with ARN: %s", ResourceModel.TYPE_NAME, arn));

        // Find first element in List<ResourceModel> which contains the provided Arn.
        // NOTE: Since the Arn is unique, we don't expect more than 1 element in List<ResourceModel> with the Arn.
        Optional<ResourceModel> modelOpt = modelList.stream()
                .filter(resourceModel -> resourceModel.getArn().equals(arn))
                .findFirst();

        // Check if model of type Optional is not empty
        if (modelOpt.isPresent()) {
            ResourceModel model = modelOpt.get();

            // Update the model in the ProgressEvent object
            progress.setResourceModel(model);
            return ProgressEvent.progress(model, context);
        } else {
            return ProgressEvent.failed(requestResourceModel, context, HandlerErrorCode.NotFound, ENDPOINT_ARN_NOT_FOUND);
        }
    }


}
