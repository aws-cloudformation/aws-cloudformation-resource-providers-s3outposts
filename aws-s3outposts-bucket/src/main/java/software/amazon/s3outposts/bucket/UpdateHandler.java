package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.utils.StringUtils;
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
        final ResourceModel previousModel = request.getPreviousResourceState();

        // Expecting the customer to always provide the ARN along with other changes.
        if (newModel == null || StringUtils.isEmpty(newModel.getArn())) {
            return ProgressEvent.failed(newModel, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        logger.log(String.format("%s::UpdateHandler called for arn: %s \n", ResourceModel.TYPE_NAME, newModel.getArn()));

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress -> preExistenceCheckForUpdate(proxy, proxyClient, request, progress, logger))
                .then(progress -> updateTags(proxy, proxyClient, request, progress, logger, previousModel))
                .then(progress -> updateBucketLifecycleConfiguration(proxy, proxyClient, request, progress, logger, previousModel))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * We will check if a bucket exists prior to calling `updateTags` and `updateBucketLifecycleConfiguration`.
     *
     * @param proxy
     * @param proxyClient
     * @param progress
     * @param request
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> preExistenceCheckForUpdate(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext context = progress.getCallbackContext();
        logger.log(String.format("%s::Update::preExistenceCheckForUpdate - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        return proxy.initiate("AWS-S3Outposts-Bucket::Update::PreExistenceCheck", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((getBucketRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketRequest, s3ControlProxyClient.client()::getBucket))
                .handleError((getBucketRequest, exception, client, resourceModel, cbContext) -> {
                    if (exception instanceof S3ControlException &&
                            ((S3ControlException) exception).statusCode() == 404) {
                        return ProgressEvent.failed(resourceModel, cbContext, HandlerErrorCode.NotFound, BUCKET_DOES_NOT_EXIST);
                    }
                    return handleError(getBucketRequest, exception, client, resourceModel, cbContext);
                })
                .progress();
    }

    /**
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @param previousModel
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateTags(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger,
            ResourceModel previousModel) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Update::updateTags - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        // No change to the tags
        if (Objects.equals(model.getTags(), previousModel.getTags()))
            return ProgressEvent.progress(model, callbackContext);

        if (model.getTags() == null) {

            // Call DeleteBucketTagging
            return proxy.initiate("AWS-S3Outposts-Bucket::Update::DeleteBucketTagging", proxyClient, model, callbackContext)
                    .translateToServiceRequest(resourceModel ->
                            Translator.translateToSdkDeleteBucketTaggingRequest(resourceModel, request.getAwsAccountId())
                    )
                    .makeServiceCall((deleteBucketTaggingRequest, s3ControlProxyClient) ->
                            s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteBucketTaggingRequest, s3ControlProxyClient.client()::deleteBucketTagging)
                    )

                    .handleError(this::handleError)
                    .progress();

        } else {

            // Call PutBucketTagging
            return putBucketTagging(proxy, proxyClient, request, progress, logger);

        }

    }

    /**
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @param previousModel
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateBucketLifecycleConfiguration(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger,
            ResourceModel previousModel) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Update::updateBucketLifecycleConfiguration - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        if (Objects.equals(model.getLifecycleConfiguration(), previousModel.getLifecycleConfiguration()))
            return ProgressEvent.progress(model, callbackContext);

        if ((model.getLifecycleConfiguration() == null) || (model.getLifecycleConfiguration().getRules() == null) ||
                (model.getLifecycleConfiguration().getRules().isEmpty())) {

            // Call DeleteBucketLifecycleConfiguration
            return proxy.initiate("AWS-S3Outposts-Bucket::Update::DeleteBucketLifecycleConfiguration", proxyClient, model, callbackContext)
                    .translateToServiceRequest(resourceModel ->
                            Translator.translateToSdkDeleteBucketLifecycleConfigurationRequest(resourceModel, request.getAwsAccountId())
                    )
                    .makeServiceCall((deleteBucketLifecycleConfigurationRequest, s3ControlProxyClient) ->
                            s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteBucketLifecycleConfigurationRequest, s3ControlProxyClient.client()::deleteBucketLifecycleConfiguration)
                    )
                    .handleError(this::handleError)
                    .progress();

        } else {

            // Call PutBucketLifecycleConfiguration
            return putLifecycleConfiguration(proxy, proxyClient, request, progress, logger);

        }

    }

}
