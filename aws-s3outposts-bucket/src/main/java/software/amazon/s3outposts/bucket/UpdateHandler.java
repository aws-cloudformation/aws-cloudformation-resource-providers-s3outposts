package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
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
        final ResourceModel newModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();

        // Expecting the customer to always provide the ARN along with other changes.
        if (newModel.getArn() == null) {
//            logger.log(String.format("Update handler requires ARN."));
            return ProgressEvent.failed(newModel, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
//            return ProgressEvent.defaultFailureHandler(
//                    new CfnInvalidRequestException(ResourceModel.TYPE_NAME),
//                    HandlerErrorCode.InvalidRequest
//            );
        }

        return ProgressEvent.progress(newModel, callbackContext)
//                // Check if resource with the ARN exists, Read call will fail if it doesn't
//                .then(progress -> readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger))
//                // Make sure the BucketName is the same
//                .then(progress -> {
//                    if (newModel.getBucketName() == null ||
//                            Objects.equals(newModel.getBucketName(), prevModel.getBucketName())) {
//                        return progress;
//                    }
//                    logger.log(String.format("Update bucket with BucketName: %s \n", newModel.getBucketName()));
//                    return ProgressEvent.defaultFailureHandler(
//                            new CfnNotUpdatableException(ResourceModel.TYPE_NAME, newModel.getBucketName()),
//                            HandlerErrorCode.NotUpdatable
//                    );
////                    return ProgressEvent.defaultFailureHandler(
////                            new CfnNotFoundException(ResourceModel.TYPE_NAME, newModel.getBucketName()),
////                            HandlerErrorCode.NotFound
////                    );
//                })
//                // Make sure the OutpostId is the same
//                .then(progress -> {
//                    if (newModel.getOutpostId() == null ||
//                            Objects.equals(newModel.getOutpostId(), prevModel.getOutpostId())) {
//                        return progress;
//                    }
//                    logger.log(String.format("Update bucket with OutpostId: %s \n", newModel.getOutpostId()));
//                    return ProgressEvent.defaultFailureHandler(
//                            new CfnNotUpdatableException(ResourceModel.TYPE_NAME, newModel.getOutpostId()),
//                            HandlerErrorCode.NotUpdatable
//                    );
////                    return ProgressEvent.defaultFailureHandler(
////                            new CfnNotFoundException(ResourceModel.TYPE_NAME, newModel.getBucketName()),
////                            HandlerErrorCode.NotFound
////                    );
//                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
//                .then(progress -> ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState()));
    }

}
