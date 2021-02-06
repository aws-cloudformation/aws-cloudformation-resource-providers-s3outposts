package software.amazon.s3outposts.bucket;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.cloudformation.proxy.*;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // Expecting customer to only provide the Arn
        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#deleteBucket-software.amazon.awssdk.services.s3control.model.DeleteBucketRequest-
        if (StringUtils.isNullOrEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> deleteBucket(proxy, proxyClient, request, progress, logger));

    }

    /**
     * Calls the API deleteBucket
     */
    private ProgressEvent<ResourceModel, CallbackContext> deleteBucket(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<S3ControlClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final Logger logger
    ) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        logger.log(String.format("%s::Delete::deleteBucket called for arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        return proxy.initiate("AWS-S3Outposts-Bucket::Delete", proxyClient, model, context)
                // Form DeleteBucketRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToDeleteRequest(resourceModel, request.getAwsAccountId()))
                // Issue call deleteBucket
                .makeServiceCall((deleteBucketRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteBucketRequest, s3ControlProxyClient.client()::deleteBucket)
                )
                .handleError((deleteBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext) -> {
                    if (exception instanceof S3ControlException && ((S3ControlException) exception).statusCode() == 409 &&
                            ((S3ControlException) exception).awsErrorDetails().errorCode().equals(INVALID_BUCKET_STATE)) {
                        logger.log(String.format("%s::Delete::handleRequest - Error Msg: %s",
                                ResourceModel.TYPE_NAME, ((S3ControlException) exception).awsErrorDetails().errorMessage()));
                        return ProgressEvent.defaultInProgressHandler(cbContext, 20, resourceModel);
                    }
                    logger.log(String.format("%s::Delete::handleRequest - Error type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                    return handleError(deleteBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext);
                })
                .done(deleteBucketResponse -> ProgressEvent.defaultSuccessHandler(null));
    }
}
