package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
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
                .then(progress -> updateTags(proxy, proxyClient, request, progress, logger, previousModel))
                .then(progress -> updateBucketLifecycleConfiguration(proxy, proxyClient, request, progress, logger, previousModel))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
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

        // AN: TODO: [P0]: Evaluate whether or not CFN can update SystemTags. If yes, add code to support this use-case.

        // No change to the tags
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
