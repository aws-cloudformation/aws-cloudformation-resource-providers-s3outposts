package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.proxy.*;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel newModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();

        // Expecting the customer to always provide the ARN along with other changes.
        if (newModel.getArn() == null) {
            return ProgressEvent.failed(newModel, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

}
