package software.amazon.s3outposts.bucketpolicy;

import software.amazon.awssdk.services.s3control.S3ControlClient;
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

        // AN TODO: [P3]: Add `preExistenceCheckForDelete`
        // Based on the results https://w.amazon.com/bin/view/Seaport/Design/CP/S3ControlAPIs/BucketPolicy/#HDeleteBucketPolicy,
        // we return 200 OK when the customer calls `DeleteBucketPolicy` and no policy is tied to the bucket.
        // While not necessary to add a preExistenceCheck, we may consider adding it in the future for completeness sake OR
        // if some unexpected behavior is seen in the field.

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> deleteBucketPolicy(proxy, proxyClient, progress, request));

    }

    /**
     *
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

        return proxy.initiate("AWS-S3Outposts-BucketPolicy::Delete", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToDeleteRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall(((deleteBucketPolicyRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteBucketPolicyRequest, s3ControlProxyClient.client()::deleteBucketPolicy)))
                .handleError(this::handleError)
//                AN TODO: [P3]: Although not necessary, we can add a stabilization check to make sure the policy is deleted.
//                Ref: https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-codeartifact/blob/9d5e545c8e5f570ef4980f38df79ff8722360d7f/aws-codeartifact-domain/src/main/java/software/amazon/codeartifact/domain/DeleteHandler.java#L57
//                .stabilize()
                .done(deleteBucketResponse -> ProgressEvent.defaultSuccessHandler(null));

    }

}
