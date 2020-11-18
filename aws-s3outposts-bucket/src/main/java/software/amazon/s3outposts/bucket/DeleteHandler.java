package software.amazon.s3outposts.bucket;

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

        // Initiate the callGraph and get the callContext
        return proxy.initiate("AWS-S3Outposts-Bucket::Delete", proxyClient, model, callbackContext)
                // Form DeleteBucketRequest
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                // Issue call deleteBucket
                .makeServiceCall((deleteBucketRequest, proxyInvocation) ->
                        proxyInvocation.injectCredentialsAndInvokeV2(deleteBucketRequest, proxyInvocation.client()::deleteBucket)
                )
                .handleError((deleteBucketRequest, exception, proxyInvocation, resourceModel, cbContext) -> {
                    logger.log(String.format("Error type: %s", exception.getClass().getCanonicalName()));
                    return handleException(exception, logger);
                })
                .done(deleteBucketResponse -> ProgressEvent.defaultSuccessHandler(null));
    }
}
