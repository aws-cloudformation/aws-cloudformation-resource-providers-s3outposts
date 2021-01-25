package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.utils.StringUtils;
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
        if (model == null || StringUtils.isEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        logger.log(String.format("%s::ReadHandler called for arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        return (ProgressEvent.progress(model, callbackContext))
                .then(progress -> getBucket(proxy, proxyClient, request, progress, logger))
                .then(progress -> getBucketTagging(proxy, proxyClient, request, progress, logger))
                .then(progress -> getBucketLifecycleConfiguration(proxy, proxyClient, request, progress, logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));

    }

    /**
     * Calls the API getBucket
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> getBucket(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();
        logger.log(String.format("%s::Read::getBucket - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        // Initiate the callGraph and get the callContext
        return proxy.initiate("AWS-S3Outposts-Bucket::Read::GetBucket", proxyClient, model, callbackContext)
                // Form GetBucketRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                // Issue call getBucket
                .makeServiceCall((getBucketRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketRequest, s3ControlProxyClient.client()::getBucket)
                )
                .handleError((getBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext) -> {
                    logger.log(String.format("%s - ReadHandler - Error Type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                    return handleError(getBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext);
                })
                .done(getBucketResponse -> {
                    // We send the Arn since the GetBucketResponse doesn't contain the Arn
                    final ResourceModel getBucketResponseModel = Translator.translateFromReadResponse(getBucketResponse, model.getArn());
                    return ProgressEvent.progress(getBucketResponseModel, callbackContext);
                });

    }

    /**
     * Calls the API getBucketTagging
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> getBucketTagging(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();
        logger.log(String.format("%s::Read::getBucketTagging - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        return proxy.initiate("AWS-S3Outposts-Bucket::Read::GetBucketTagging", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToSdkGetBucketTaggingRequest(resourceModel, request.getAwsAccountId())
                )
                .makeServiceCall((getBucketTaggingRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketTaggingRequest, s3ControlProxyClient.client()::getBucketTagging)
                )
                .handleError((getBucketTaggingRequest, exception, client, resourceModel, cbContext) -> {
                    // It is ok for a CFN Bucket Resource to have no tags. We do not have to fail the CFN Bucket::Read operation for this.
                    if (exception instanceof S3ControlException && StringUtils.equals(((S3ControlException) exception).awsErrorDetails().errorCode(), NO_SUCH_TAGSET)) {
                        logger.log(String.format("%s::Read::GetBucketTagging - NoSuchTagSet - Message: %s \n", ResourceModel.TYPE_NAME, exception.getMessage()));
                        return ProgressEvent.progress(resourceModel, cbContext);
                    } else {
                        logger.log(String.format("%s::Read::GetBucketTagging failed with exception type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                        return handleError(getBucketTaggingRequest, exception, client, resourceModel, cbContext);
                    }
                })
                .done(getBucketTaggingResponse -> {
                    final ResourceModel getBucketTaggingResponseModel = Translator.translateFromSdkGetBucketTaggingResponse(getBucketTaggingResponse, model);
                    return ProgressEvent.progress(getBucketTaggingResponseModel, callbackContext);
                });

    }

    /**
     * Calls the API getBucketLifecycleConfiguration
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> getBucketLifecycleConfiguration(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger
    ) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Read::getLifecycleConfiguration - arn: %s", ResourceModel.TYPE_NAME, model.getArn()));

        return proxy.initiate("AWS-S3Outposts-Bucket::Read::GetBucketLifecycleConfiguration", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToSdkGetBucketLifecycleConfigurationRequest(resourceModel, request.getAwsAccountId())
                )
                .makeServiceCall((getBucketLifecycleConfigurationRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketLifecycleConfigurationRequest, s3ControlProxyClient.client()::getBucketLifecycleConfiguration)
                )
                .handleError((getBucketLifecycleConfigurationRequest, exception, client, resourceModel, cbContext) -> {
                    // It is ok for a CFN Bucket Resource to have no lifecycle configuration. We do not have to fail the CFN Bucket::Read operation for this.
                    if (exception instanceof S3ControlException && ((S3ControlException) exception).statusCode() == 404) {
                        logger.log(String.format("%s::Read::GetBucketLifecycleConfiguration - NoSuchLifecycleConfiguration - Message: %s \n", ResourceModel.TYPE_NAME, exception.getMessage()));
                        return ProgressEvent.progress(resourceModel, cbContext);
                    } else {
                        logger.log(String.format("%s::Read::GetBucketLifecycleConfiguration failed with exception type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                        return handleError(getBucketLifecycleConfigurationRequest, exception, client, resourceModel, cbContext);
                    }
                })
                .done(getBucketLifecycleConfigurationResponse -> {
                    final ResourceModel getBucketLifecycleConfigurationResponseModel = Translator.translateFromSdkGetBucketLifecycleConfigurationResponse(getBucketLifecycleConfigurationResponse, model);
                    return ProgressEvent.progress(getBucketLifecycleConfigurationResponseModel, callbackContext);
                });

    }

}
