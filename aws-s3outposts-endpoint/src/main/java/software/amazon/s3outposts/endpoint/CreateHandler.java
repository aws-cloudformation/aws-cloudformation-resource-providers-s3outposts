package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.utils.StringUtils;
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

        // Expecting the customer to provide outpostId, subnetId, securityGroupId
        if (model == null || StringUtils.isEmpty(model.getOutpostId()) ||
                StringUtils.isEmpty(model.getSecurityGroupId()) || StringUtils.isEmpty(model.getSubnetId()))
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, INVALID_INPUT);

        return ProgressEvent.progress(model, callbackContext)
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

        logger.log(String.format("%s::Create::CreateEndpoint for outpost %s with subnet: %s, securityGroup: %s \n",
                ResourceModel.TYPE_NAME, model.getOutpostId(), model.getSubnetId(), model.getSecurityGroupId()));

        return proxy.initiate("AWS-S3Outposts-Endpoint::Create", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToSdkCreateEndpointRequest)
                .makeServiceCall(((createEndpointRequest, s3OutpostsProxyClient) ->
                        s3OutpostsProxyClient.injectCredentialsAndInvokeV2(createEndpointRequest, s3OutpostsProxyClient.client()::createEndpoint)))
                .stabilize((createEndpointRequest, createEndpointResponse, s3OutpostsProxyClient, resourceModel, cbContext) -> {
                    if (!StringUtils.isEmpty(createEndpointResponse.endpointArn())) {
                        String endpointArn = createEndpointResponse.endpointArn().replaceFirst("/ec2/", String.format("/%s/", resourceModel.getOutpostId()));
                        resourceModel.setArn(endpointArn);
                        logger.log(String.format("%s::Create - Endpoint ARN: %s", ResourceModel.TYPE_NAME, resourceModel.getArn()));
                        final EndpointArnFields endpointArnFields = EndpointArnFields.splitArn(endpointArn);
                        resourceModel.setId(endpointArnFields.endpointId);
                        cbContext.setStabilized(true);
                    }
                    return cbContext.stabilized;
                })
                .handleError(this::handleError)
                .progress();

    }
}
