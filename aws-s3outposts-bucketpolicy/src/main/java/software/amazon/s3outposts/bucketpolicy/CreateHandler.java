package software.amazon.s3outposts.bucketpolicy;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.*;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    /**
     * @param proxy
     * @param request
     * @param callbackContext
     * @param proxyClient
     * @param logger
     * @return
     */
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // Verify that the user has provided both the Bucket (ARN) and the PolicyDocument
        if (StringUtils.isEmpty(model.getBucket())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        if (CollectionUtils.isNullOrEmpty(model.getPolicyDocument())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, POLICY_DOC_REQD);
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> preExistenceCheckForCreate(proxy, proxyClient, progress, request))
                .then(progress -> createBucketPolicy(proxy, proxyClient, progress, request))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));

    }

    /**
     * For the Create to succeed, there should be no prior bucket policy associated with the bucket.
     *
     * @param proxy
     * @param proxyClient
     * @param progressEvent
     * @param request
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> preExistenceCheckForCreate(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            ResourceHandlerRequest<ResourceModel> request
    ) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        return proxy.initiate("AWS-S3Outposts-BucketPolicy::Create::PreExistenceCheck", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((getBucketPolicyRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketPolicyRequest, s3ControlProxyClient.client()::getBucketPolicy))
                .handleError((getBucketPolicyRequest, exception, client, resourceModel, cbContext) -> {
                    if (exception instanceof S3ControlException &&
                            StringUtils.equals(((S3ControlException) exception).awsErrorDetails().errorCode(), NO_SUCH_BUCKET_POLICY_ERROR_CODE)) {
                        return ProgressEvent.progress(model, cbContext);
                    }
                    throw exception;
                })
                .done((getBucketPolicyRequest, getBucketPolicyResponse, client, resourceModel, cbContext) -> {
                    if (!StringUtils.isEmpty(getBucketPolicyResponse.policy())) {
                        return ProgressEvent.failed(resourceModel, cbContext, HandlerErrorCode.AlreadyExists, BUCKET_POLICY_EXISTS);
                    }
                    return ProgressEvent.progress(model, context);
                });

    }

    /**
     * @param proxy
     * @param proxyClient
     * @param progressEvent
     * @param request
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> createBucketPolicy(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<S3ControlClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ResourceHandlerRequest<ResourceModel> request

    ) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        return proxy.initiate("AWS-S3Outposts-BucketPolicy::Create", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToPutRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((putBucketPolicyRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(putBucketPolicyRequest, s3ControlProxyClient.client()::putBucketPolicy))
                .handleError(this::handleError)
                .progress();

    }

}
