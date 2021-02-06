package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.S3OutpostsException;
import software.amazon.awssdk.services.s3outposts.model.S3OutpostsRequest;
import software.amazon.cloudformation.proxy.*;

import java.util.stream.Collectors;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    // Error messages (returned to the customer)
    protected static final String INVALID_INPUT = "OutpostId, SecurityGroupId, SubnetId are required parameters.";
    protected static final String ENDPOINT_ARN_REQD = "Endpoint ARN is required.";
    protected static final String ENDPOINT_ARN_NOT_FOUND = "Endpoint with provided ARN not found.";

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

        if (model != null && model.getArn() != null) {
            // true only when we call `listEndpoints` from the ReadHandler
            logger.log(String.format("%s::Read::listEndpoints, arn: %s", ResourceModel.TYPE_NAME, model.getArn()));
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
                                                .map(endpoint ->
                                                        Translator.translateFromSdkEndpoint(endpoint)
                                                )
                                                .collect(Collectors.toList())
                                )
                                .nextToken(listEndpointsResponse.nextToken())
                                .status(OperationStatus.IN_PROGRESS)
                                .build()

                );

    }

}
