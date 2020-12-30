package software.amazon.s3outposts.bucket;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.s3control.S3ControlClient;
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

        // Initiate the callGraph and get the callContext
        return proxy.initiate("AWS-S3Outposts-Bucket::Delete", proxyClient, model, callbackContext)
                // Form DeleteBucketRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToDeleteRequest(resourceModel, request.getAwsAccountId()))
                // Issue call deleteBucket
                .makeServiceCall((deleteBucketRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteBucketRequest, s3ControlProxyClient.client()::deleteBucket)
                )
                .handleError((deleteBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext) -> {
                    logger.log(String.format("%s - DeleteHandler - Error type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                    return handleError(deleteBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext);
                })
                .done(deleteBucketResponse -> ProgressEvent.defaultSuccessHandler(null));
    }
}
