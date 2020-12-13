package software.amazon.s3outposts.bucket;

import com.amazonaws.util.StringUtils;
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
        if (StringUtils.isNullOrEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

//        logger.log(String.format("ReadHandler - Arn: %s \n", model.getArn()));
//        logger.log(String.format("Timestamp1: %s",System.currentTimeMillis()));

        // Initiate the callGraph and get the callContext
        return proxy.initiate("AWS-S3Outposts-Bucket::Read", proxyClient, model, callbackContext)
                // Form GetBucketRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                // Issue call getBucket
                .makeServiceCall((getBucketRequest, s3ControlProxyClient) -> {
//                    logger.log(String.format("Timestamp2: %s",System.currentTimeMillis()));
//                    logger.log(String.format("ReadHandler -  AccountId: %s, ARN: %s", getBucketRequest.accountId(), getBucketRequest.bucket()));
                    return s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketRequest, s3ControlProxyClient.client()::getBucket);
                })
                .handleError((getBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext) -> {
//                    logger.log(String.format("Timestamp3: %s",System.currentTimeMillis()));
                    logger.log(String.format("%s - ReadHandler - Error Type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                    return handleError(getBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext);
//                    return handleException(exception, logger);
                })
                .done(getBucketResponse -> {
                    logger.log(String.format("Timestamp3: %s", System.currentTimeMillis()));
                    return ProgressEvent.defaultSuccessHandler(
                            // We send the Arn since the GetBucketResponse doesn't contain the Arn
                            Translator.translateFromReadResponse(getBucketResponse, model.getArn())
                    );
                });
    }
}
