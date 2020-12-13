package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.*;
import software.amazon.cloudformation.resource.IdentifierUtils;


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
        // If a BucketName is not provided, we will generate a BucketName for the customer.
        // However, the OutpostId is required.
        if (StringUtils.isEmpty(model.getOutpostId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, OUTPOSTID_REQD);
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> proxy.initiate("AWS-S3Outposts-Bucket::Create", proxyClient, model, callbackContext)
                        // Form CreateBucketRequest
                        .translateToServiceRequest(resourceModel -> {
                            // Generate bucket name if empty
                            if (StringUtils.isEmpty(resourceModel.getBucketName())) {
                                final String bucketName = IdentifierUtils.generateResourceIdentifier(
                                        request.getLogicalResourceIdentifier(),
                                        request.getClientRequestToken()).toLowerCase();
                                logger.log(String.format("Generated bucket name: %s", bucketName));
                                resourceModel.setBucketName(bucketName);
                            }
                            return Translator.translateToCreateRequest(resourceModel);
                        })
                        // Issue call createBucket
                        .makeServiceCall((createBucketRequest, s3ControlProxyClient) ->
                                s3ControlProxyClient.injectCredentialsAndInvokeV2(createBucketRequest, s3ControlProxyClient.client()::createBucket)
                        )
                        .stabilize((createBucketRequest, createBucketResponse, s3ControlProxyClient, resourceModel, cbContext) -> {
                            if (createBucketResponse.bucketArn() != null) {
                                // AN TODO: [P0]: Remove the following line of code before making the resource public.
                                String arn = createBucketResponse.bucketArn().replaceFirst("ec2", resourceModel.getOutpostId());
                                resourceModel.setArn(arn);
                                logger.log(String.format("CreateHandler - Modified ARN: %s", resourceModel.getArn()));
                                cbContext.setStabilized(true);
                                return true;
                            }
                            return false;
                        })
                        .handleError((createBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext) -> {
                            logger.log(String.format("%s - CreateHandler - Error Type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                            return handleError(createBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext);
                        })
                        .progress()
                )
                .then(progress -> BaseHandlerStd.propagate(progress, logger))
                .then(progress -> {
                    return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
                });
    }
}
