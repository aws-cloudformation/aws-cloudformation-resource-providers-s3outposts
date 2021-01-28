package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.*;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // Expecting 2 inputs from the customer: endpointId, outpostId
        if (model == null || StringUtils.isEmpty(model.getArn()))
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, ENDPOINT_ARN_REQD);

        logger.log(String.format("%s::Delete - arn %s", ResourceModel.TYPE_NAME, model.getArn()));

        return proxy.initiate("AWS-S3Outposts-Endpoint::Delete", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToSdkDeleteEndpointRequest)
                .makeServiceCall((deleteEndpointRequest, s3OutpostsProxyClient) ->
                        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/DeleteEndpointRequest.html
                        s3OutpostsProxyClient.injectCredentialsAndInvokeV2(deleteEndpointRequest, s3OutpostsProxyClient.client()::deleteEndpoint))
                .handleError(this::handleError)
                .done(deleteEndpointResponse ->
                        ProgressEvent.defaultSuccessHandler(null));

    }
}
