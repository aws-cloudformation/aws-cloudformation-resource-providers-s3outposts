package software.amazon.s3outposts.bucketpolicy;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.*;

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

        // Verify that the user has provided both the Bucket (ARN) and the PolicyDocument
        if (StringUtils.isEmpty(newModel.getBucket())) {
            return ProgressEvent.failed(newModel, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        if (CollectionUtils.isNullOrEmpty(newModel.getPolicyDocument())) {
            return ProgressEvent.failed(newModel, callbackContext, HandlerErrorCode.InvalidRequest, POLICY_DOC_REQD);
        }

        // AN TODO: [P2]: Assigning a new bucket to the same CFN stack is (I think) an allowed operation. It results in
        // an Update and Replace. The previous bucket and bucketPolicy should stay intact in the DDB.
        // Test this and make sure this works. Not including the corresponding code from S3:
        // Ref: https://tiny.amazon.com/40949biy

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> preExistenceCheckForUpdate(proxy, proxyClient, progress, request))
                .then(progress -> updateBucketPolicy(proxy, proxyClient, progress, request))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * We will update a policy only if there was one before. Else, the customer must call create.
     *
     * @param proxy
     * @param proxyClient
     * @param progressEvent
     * @param request
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> preExistenceCheckForUpdate(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            ResourceHandlerRequest<ResourceModel> request
    ) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        return proxy.initiate("AWS-S3Outposts-BucketPolicy::Update::PreExistenceCheck", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((getBucketPolicyRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketPolicyRequest, s3ControlProxyClient.client()::getBucketPolicy))
                .handleError((getBucketPolicyRequest, exception, client, resourceModel, cbContext) -> {
                    if (exception instanceof S3ControlException &&
                            StringUtils.equals(((S3ControlException) exception).awsErrorDetails().errorCode(), NO_SUCH_BUCKET_POLICY_ERROR_CODE)) {
                        return ProgressEvent.failed(resourceModel, cbContext, HandlerErrorCode.NotFound, BUCKET_POLICY_MISSING);
                    }
                    return handleError(getBucketPolicyRequest, exception, client, resourceModel, cbContext);
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
    private ProgressEvent<ResourceModel, CallbackContext> updateBucketPolicy(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<S3ControlClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ResourceHandlerRequest<ResourceModel> request
    ) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        return proxy.initiate("AWS-S3-BucketPolicy::Update", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToPutRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall(((putBucketPolicyRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(putBucketPolicyRequest, s3ControlProxyClient.client()::putBucketPolicy)))
                .handleError(this::handleError)
                .progress();

    }

}
