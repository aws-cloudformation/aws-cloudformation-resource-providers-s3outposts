package software.amazon.s3outposts.bucketpolicy;

import software.amazon.awssdk.services.s3control.S3ControlClient;
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

        // Bucket ARN is required for getting the BucketPolicy.
        // We intentionally don't return an error when the PolicyDocument is passed, since when the ReadHandler is called
        // after a successful CREATE or UPDATE operation, it always contains the PolicyDocument.
        if (model.getBucket() == null || StringUtils.isEmpty(model.getBucket())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        return proxy.initiate("AWS-S3Outposts-BucketPolicy::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((getBucketPolicyRequest, s3ControlProxyClient) -> {
                    return s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketPolicyRequest, s3ControlProxyClient.client()::getBucketPolicy);
                })
                .handleError(this::handleError)
                .done((getBucketPolicyResponse) ->
                        ProgressEvent.defaultSuccessHandler(
                                Translator.translateFromReadResponse(getBucketPolicyResponse, model)
                        )
                );

    }
}
