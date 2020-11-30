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
        if (model.getArn() != null) {
            logger.log(String.format("ReadHandler - Arn: %s \n", model.getArn()));
        } else {
            logger.log("ReadHandler - Arn is null");
        }

        // Initiate the callGraph and get the callContext
        return proxy.initiate("AWS-S3Outposts-Bucket::Read", proxyClient, model, callbackContext)
                // Form GetBucketRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                // Issue call getBucket
                .makeServiceCall((getBucketRequest, s3ControlClientProxyClient) -> {
                    logger.log(String.format("ReadHandler -  AccountId: %s, ARN: %s", getBucketRequest.accountId(), getBucketRequest.bucket()));
                    return s3ControlClientProxyClient.injectCredentialsAndInvokeV2(getBucketRequest, s3ControlClientProxyClient.client()::getBucket);
                })
                .handleError((getBucketRequest, exception, proxyInvocation, resourceModel, cbContext) -> {
                    logger.log(String.format("ReadHandler - Error Type: %s", exception.getClass().getCanonicalName()));
                    return handleException(exception, logger);
                })
                .done(getBucketResponse -> ProgressEvent.defaultSuccessHandler(
                        // We send the Arn since the GetBucketResponse doesn't contain the Arn
                        Translator.translateFromReadResponse(getBucketResponse, model.getArn())
                ));
    }
}
