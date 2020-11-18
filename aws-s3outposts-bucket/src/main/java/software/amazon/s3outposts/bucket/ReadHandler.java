package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.proxy.*;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // Expecting customer to only provide the Arn.
        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#getBucket-software.amazon.awssdk.services.s3control.model.GetBucketRequest-

        // Initiate the callGraph and get the callContext
        return proxy.initiate("AWS-S3Outposts-Bucket::Read", proxyClient, model, callbackContext)
                // Form GetBucketRequest
                .translateToServiceRequest(Translator::translateToReadRequest)
                // Issue call getBucket
                .makeServiceCall((getBucketRequest, s3ControlClientProxyClient) ->
                        s3ControlClientProxyClient.injectCredentialsAndInvokeV2(getBucketRequest, s3ControlClientProxyClient.client()::getBucket)
                )
                .handleError((getBucketRequest, exception, proxyInvocation, resourceModel, cbContext) -> {
                    logger.log(String.format("Error Type: %s", exception.getClass().getCanonicalName()));
                    return handleException(exception, logger);
                })
                .done(getBucketResponse -> ProgressEvent.defaultSuccessHandler(
                        // We send the Arn since the GetBucketResponse doesn't contain the Arn
                        Translator.translateFromReadResponse(getBucketResponse, model.getArn())
                ));
    }
}
