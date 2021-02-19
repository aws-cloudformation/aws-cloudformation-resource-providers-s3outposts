package software.amazon.s3outposts.bucketpolicy;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.utils.StringUtils;
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

        if (model == null || StringUtils.isEmpty(model.getBucket())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> preExistenceCheckForDelete(proxy, proxyClient, progress, request))
                .then(progress -> deleteBucketPolicy(proxy, proxyClient, progress, request));

    }

    /**
     * We will delete a policy only if it exists.
     *
     * @param proxy
     * @param proxyClient
     * @param progressEvent
     * @param request
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> preExistenceCheckForDelete(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<S3ControlClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ResourceHandlerRequest<ResourceModel> request
    ) {

        ResourceModel model = progressEvent.getResourceModel();
        CallbackContext context = progressEvent.getCallbackContext();

        logger.log(String.format("%s::Delete::preExistenceCheck - Bucket %s", ResourceModel.TYPE_NAME, model.getBucket()));

        return proxy.initiate("AWS-S3Outposts-BucketPolicy::Delete::PreExistenceCheck", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((getBucketPolicyRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketPolicyRequest, s3ControlProxyClient.client()::getBucketPolicy))
                .handleError((getBucketPolicyRequest, exception, client, resourceModel, cbContext) -> {
                    if (exception instanceof S3ControlException &&
                            StringUtils.equals(((S3ControlException) exception).awsErrorDetails().errorCode(), NO_SUCH_BUCKET_POLICY_ERROR_CODE)) {
                        return ProgressEvent.failed(resourceModel, cbContext, HandlerErrorCode.NotFound, BUCKET_POLICY_MISSING);
                    }
                    throw exception;
                })
                .progress();

    }

    /**
     * @param proxy
     * @param proxyClient
     * @param progressEvent
     * @param request
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> deleteBucketPolicy(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<S3ControlClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ResourceHandlerRequest<ResourceModel> request
    ) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        logger.log(String.format("%s::Delete::deleteBucketPolicy - Bucket %s", ResourceModel.TYPE_NAME, model.getBucket()));

        return proxy.initiate("AWS-S3Outposts-BucketPolicy::Delete", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToDeleteRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall(((deleteBucketPolicyRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteBucketPolicyRequest, s3ControlProxyClient.client()::deleteBucketPolicy)))
                .handleError(this::handleError)
                .done(deleteBucketResponse -> ProgressEvent.defaultSuccessHandler(null));

    }

}
