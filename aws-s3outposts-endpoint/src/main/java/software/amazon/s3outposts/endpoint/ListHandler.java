package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.cloudformation.proxy.*;

public class ListHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> listEndpoints(proxy, proxyClient, request, progress, logger))
                .then(progress -> {
                    progress.setStatus(OperationStatus.SUCCESS);
                    return progress;
                });

    }

}
