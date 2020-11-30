package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.util.Objects;

public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel new_model = request.getDesiredResourceState();
        final ResourceModel prev_model = request.getPreviousResourceState();

        // Expecting the customer to always provide the ARN along with other changes.
        logger.log(String.format("Update bucket with ARN: %s", new_model.getArn()));

        return ProgressEvent.progress(new_model, callbackContext)
//                // Check if resource with the ARN exists, Read call will fail if it doesn't
//                .then(progress -> readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger))
                // Make sure the BucketName is the same
                .then(progress -> {
                    if (new_model.getBucketName() == null ||
                            Objects.equals(new_model.getBucketName(), prev_model.getBucketName())) {
                        return progress;
                    }
                    logger.log(String.format("Update bucket with BucketName: %s \n", new_model.getBucketName()));
//                    return ProgressEvent.defaultFailureHandler(
//                            new CfnNotUpdatableException(ResourceModel.TYPE_NAME, new_model.getBucketName()),
//                            HandlerErrorCode.NotUpdatable
//                    );
                    return ProgressEvent.defaultFailureHandler(
                            new CfnNotFoundException(ResourceModel.TYPE_NAME, new_model.getBucketName()),
                            HandlerErrorCode.NotFound
                    );
                })
                // Make sure the OutpostId is the same
                .then(progress -> {
                    if (new_model.getOutpostId() == null ||
                            Objects.equals(new_model.getOutpostId(), prev_model.getOutpostId())) {
                        return progress;
                    }
                    logger.log(String.format("Update bucket with OutpostId: %s \n", new_model.getOutpostId()));
//                    return ProgressEvent.defaultFailureHandler(
//                            new CfnNotUpdatableException(ResourceModel.TYPE_NAME, new_model.getOutpostId()),
//                            HandlerErrorCode.NotUpdatable
//                    );
                    return ProgressEvent.defaultFailureHandler(
                            new CfnNotFoundException(ResourceModel.TYPE_NAME, new_model.getBucketName()),
                            HandlerErrorCode.NotFound
                    );
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
//                .then(progress -> ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState()));
    }

}
