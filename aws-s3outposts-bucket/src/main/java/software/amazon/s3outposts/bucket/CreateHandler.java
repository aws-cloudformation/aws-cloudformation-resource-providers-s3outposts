package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.*;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // Expecting customer to provide 2 parameters: BucketName and OutpostId.
        // JavaSDK: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#createBucket-software.amazon.awssdk.services.s3control.model.CreateBucketRequest-
        // We will not generate a BucketName for S3Outposts buckets if the user does not provide a name because:
        // - There is no guarantee that the generated name will meet the regex requirements for our bucket name.
        // - BucketName is a `required` field in our JSON model (aws-s3outposts-bucket.json). If the user does not provide
        //   a BucketName, the framework will fail the request prior to it reaching the handler.
        if (StringUtils.isEmpty(model.getBucketName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_NAME_REQD);
        }

        if (StringUtils.isEmpty(model.getOutpostId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, OUTPOSTID_REQD);
        }

        logger.log(String.format("%s::Create bucket name: %s", ResourceModel.TYPE_NAME, model.getBucketName()));

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> createBucket(proxy, proxyClient, request, progress, logger))
                .then(progress -> BaseHandlerStd.propagate(progress, logger))
                .then(progress -> putBucketTagging(proxy, proxyClient, request, progress, logger))
                .then(progress -> putLifecycleConfiguration(proxy, proxyClient, request, progress, logger))
                .then(progress ->
                        new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
                );

    }

    /**
     * Calls the API createBucket
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> createBucket(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Create::CreateBucket - name %s \n", ResourceModel.TYPE_NAME, model.getBucketName()));

        return proxy.initiate("AWS-S3Outposts-Bucket::Create", proxyClient, model, callbackContext)
                // Form CreateBucketRequest
                .translateToServiceRequest(Translator::translateToCreateRequest)
                // Issue call createBucket
                .makeServiceCall((createBucketRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(createBucketRequest, s3ControlProxyClient.client()::createBucket)
                )
                .stabilize((createBucketRequest, createBucketResponse, s3ControlProxyClient, resourceModel, cbContext) -> {
                    if (createBucketResponse.bucketArn() != null) {
                        // AN TODO: [P0]: Remove the following line of code before making the resource public.
                        String arn = createBucketResponse.bucketArn().replaceFirst("/ec2/", String.format("/%s/", resourceModel.getOutpostId()));
                        resourceModel.setArn(arn);
                        logger.log(String.format("CreateHandler - Modified ARN: %s", resourceModel.getArn()));
                        cbContext.setStabilized(true);
                        return true;
                    }
                    return false;
                })
                .handleError(this::handleError)
                .progress();

    }

}
