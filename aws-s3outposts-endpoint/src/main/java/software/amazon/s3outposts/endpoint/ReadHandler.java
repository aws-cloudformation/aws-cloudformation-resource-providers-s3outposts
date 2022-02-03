package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.*;

public class ReadHandler extends BaseHandlerStd {

    private Logger logger;

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        ResourceModel model = request.getDesiredResourceState();
        // Expecting customer to only provide Arn
        if (model == null || StringUtils.isEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, ENDPOINT_ARN_REQD);
        }

        logger.log(String.format("%s::Read - ARN: %s", ResourceModel.TYPE_NAME, model.getArn()));

        return (ProgressEvent.progress(model, callbackContext))
                .then(progress -> listEndpoints(proxy, proxyClient, request, progress, logger))
                .then(progress -> getEndpoint(progress, logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

}
