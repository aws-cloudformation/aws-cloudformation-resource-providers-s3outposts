package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
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

        // Expecting customer to provide 2 parameters: bucketName and outpostId.
        // JavaSDK: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#createBucket-software.amazon.awssdk.services.s3control.model.CreateBucketRequest-

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> {
                    logger.log(String.format("CreateHandler -  BucketName: %s, OutpostId: %s", model.getBucketName(), model.getOutpostId()));
                    // Initiate callGraph and get the callContext
                    return proxy.initiate("AWS-S3Outposts-Bucket::Create", proxyClient, model, callbackContext)
                            // Form CreateBucketRequest
                            .translateToServiceRequest(Translator::translateToCreateRequest)
                            // Issue call createBucket
                            .makeServiceCall((createBucketRequest, s3ControlClientProxyClient) ->
                                    s3ControlClientProxyClient.injectCredentialsAndInvokeV2(createBucketRequest, s3ControlClientProxyClient.client()::createBucket)
                            )
//                            .stabilize((createBucketRequest, createBucketResponse, s3ControlClientProxyClient, resourceModel, cbContext) -> {
//                                logger.log(String.format("Check for stabilization after CreateBucket - %s", callbackContext.isPropagated()));
//                                if (callbackContext.isPropagated()) return callbackContext.isPropagated();
//                                callbackContext.setPropagated(true);
//                                resourceModel.setArn(createBucketResponse.bucketArn());
//                                return true;
//                            })
                            .handleError((createBucketRequest, exception, s3ControlClientProxy, resourceModel, cbContext) -> {
                                logger.log(String.format("CreateHandler - Error Type: %s", exception.getClass().getCanonicalName()));
                                return handleException(exception, logger);
                            })
                            .done((createBucketResponse) -> {
                                logger.log(String.format("CreateHandler - Bucket created with ARN: %s", createBucketResponse.bucketArn()));
                                // Set the primary identifier i.e. Arn
                                model.setArn(createBucketResponse.bucketArn());
                                return ProgressEvent.progress(model, progress.getCallbackContext());
//                                return ProgressEvent.defaultInProgressHandler(progress.getCallbackContext(), CALLBACK_DELAY_SECONDS, model);
                            });
                })
                .then(progress -> BaseHandlerStd.propagate(progress, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
