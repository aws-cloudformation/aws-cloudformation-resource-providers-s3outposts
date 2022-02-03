package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.CreateEndpointResponse;
import software.amazon.awssdk.services.s3outposts.model.EndpointAccessType;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.*;

public class CreateHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        CallbackContext currentContext = callbackContext.stabilizationRetriesRemaining == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(NUMBER_OF_STABILIZATION_RETRIES).build() :
                callbackContext;

        // Expecting the customer to provide outpostId, subnetId, securityGroupId
        if (model == null || StringUtils.isEmpty(model.getOutpostId()) ||
                StringUtils.isEmpty(model.getSecurityGroupId()) || StringUtils.isEmpty(model.getSubnetId()))
            return ProgressEvent.failed(model, currentContext, HandlerErrorCode.InvalidRequest, INVALID_INPUT);

        // If AccessType is not set, set it the to default value
        if (model.getAccessType() == null) {
            logger.log("AccessType not provided. Using default AccessType \"Private\"");
            model.setAccessType(DEFAULT_ACCESS_TYPE);
        }

        // Check if AccessType is valid by the SDK
        EndpointAccessType endpointAccessType = EndpointAccessType.fromValue(model.getAccessType());
        if (endpointAccessType.toString().equals("null")) {
            return ProgressEvent.failed(model, currentContext, HandlerErrorCode.InvalidRequest, INVALID_ACCESS_TYPE);
        }

        return ProgressEvent.progress(model, currentContext)
                .then(progress -> createEndpoint(proxy, proxyClient, request, progress, logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));

    }

    /**
     * Calls the API createEndpoint
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> createEndpoint(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3OutpostsClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Create::CreateEndpoint for outpost %s with subnet: %s, securityGroup: %s, accessType: %s, customerOwnedIpv4Pool: %s\n",
                ResourceModel.TYPE_NAME, model.getOutpostId(), model.getSubnetId(), model.getSecurityGroupId(), model.getAccessType(), model.getCustomerOwnedIpv4Pool()));

        return proxy.initiate("AWS-S3Outposts-Endpoint::Create", proxyClient, model, callbackContext)
                // Translate CFN request to SDK compatible DeleteEndpointRequest
                .translateToServiceRequest(Translator::translateToSdkCreateEndpointRequest)
                .backoffDelay(STABILIZATION_DELAY)
                .makeServiceCall(((createEndpointRequest, s3OutpostsProxyClient) ->
                        s3OutpostsProxyClient.injectCredentialsAndInvokeV2(createEndpointRequest, s3OutpostsProxyClient.client()::createEndpoint)))
                // Loop over the stabilize method until created endpoint is actually Available
                .stabilize((createEndpointRequest, createEndpointResponse, s3OutpostsProxyClient, resourceModel, cbContext) ->
                    stabilizedOnCreate(proxyClient, resourceModel, request, createEndpointResponse, cbContext, proxy)
                )
                .handleError(this::handleError)
                .progress();
    }

    /**
     * Handler stabilize operation to wait till resource reaches terminal state (Available).
     * We do this by calling ListEndpoint -> GetEndpoint -> Check the Endpoint status.
     *
     * @param proxyClient The aws service client to make the call
     * @param resourceModel Resource model
     * @param request Request
     * @param createEndpointResponse Response for Create Endpoint Request
     * @param cbContext Callback Context
     * @param proxy AmazonWebServicesClientProxy
     * @return boolean state of stabilized or not
     */
    protected boolean stabilizedOnCreate(
            final ProxyClient<S3OutpostsClient> proxyClient,
            final ResourceModel resourceModel,
            final ResourceHandlerRequest<ResourceModel> request,
            final CreateEndpointResponse createEndpointResponse,
            final CallbackContext cbContext,
            final AmazonWebServicesClientProxy proxy) {

        String clientRequestToken = request.getClientRequestToken();

        if (!StringUtils.isEmpty(createEndpointResponse.endpointArn())) {
            String endpointArn = createEndpointResponse.endpointArn();
            // Replace the 'ec2' with OutpostID to display a valid ARN while logging
            String finalEndpointArn = endpointArn.replaceFirst("/ec2/", String.format("/%s/", resourceModel.getOutpostId()));
            logger.log(String.format("[ClientRequestToken: %s] Stabilizing create operation for Endpoint ARN: %s.",
                    clientRequestToken, finalEndpointArn));
            final EndpointArnFields endpointArnFields = EndpointArnFields.splitArn(finalEndpointArn);

            // If retries remaining are > 0 , then reduce the remaining retries by 1
            // Else if 0 retries are left, log timeout and return SUCCESS instead
            if (cbContext.getStabilizationRetriesRemaining() > 0) {
                cbContext.setStabilizationRetriesRemaining(cbContext.getStabilizationRetriesRemaining() - 1);
            } else {
                logger.log(MAX_RETRY_ATTEMPTS);
                logger.log(String.format("[ClientRequestToken: %s] Endpoint ARN: %s",
                        clientRequestToken, finalEndpointArn));
                return true;
            }

            resourceModel.setArn(finalEndpointArn);
            resourceModel.setId(endpointArnFields.endpointId);
            ProgressEvent<ResourceModel, CallbackContext> readResponse = (ProgressEvent.progress(resourceModel, new CallbackContext()))
                    .then(progress -> listEndpoints(proxy, proxyClient, request, progress, logger))
                    .then(progress -> getEndpoint(progress, logger));

            String status = readResponse.getResourceModel().getStatus();

            switch (status) {
                case "Pending":
                    logger.log(String.format("[ClientRequestToken: %s] Endpoint ARN: %s is in Pending State.",
                            clientRequestToken, finalEndpointArn));
                    break;

                case "Available":
                    logger.log(String.format("[ClientRequestToken: %s] %s::Create Endpoint ARN: %s is Available.",
                            ResourceModel.TYPE_NAME, clientRequestToken, finalEndpointArn));
                    cbContext.setStabilized(true);
                    break;
                // If status is anything other than Pending or Available, means that the create failed for another reason.
                default:
                    logger.log(String.format("[ClientRequestToken: %s] Resource %s reached unexpected status %s during stabilization.",
                            clientRequestToken, finalEndpointArn, status));
                    throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, finalEndpointArn);
            }

        }
        return cbContext.stabilized;
    }
}
