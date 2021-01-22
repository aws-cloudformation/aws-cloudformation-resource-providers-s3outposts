package software.amazon.s3outposts.accesspoint;

import com.amazonaws.util.StringUtils;
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

        // Expecting the customer to provide 3 parameters: Bucket (arn), Name (AccessPoint name) and VpcConfiguration.
        if (model == null || StringUtils.isNullOrEmpty(model.getBucket())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, BUCKET_ARN_REQD);
        }

        if (StringUtils.isNullOrEmpty(model.getName())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, ACCESSPOINT_NAME_REQD);
        }

        if (model.getVpcConfiguration() == null || StringUtils.isNullOrEmpty(model.getVpcConfiguration().getVpcId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, VPC_CONFIGURATION_REQD);
        }

        logger.log(String.format("%s::CreateHandler called for bucketArn: %s, with name: %s, with Vpc: %s \n",
                ResourceModel.TYPE_NAME, model.getBucket(), model.getName(), model.getVpcConfiguration().getVpcId()));

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> createAccessPoint(proxy, proxyClient, request, progress, logger))
                .then(progress -> BaseHandlerStd.propagate(progress, logger))
                .then(progress -> putAccessPointPolicy(proxy, proxyClient, request, progress, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Calls the API createAccessPoint
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#createAccessPoint-software.amazon.awssdk.services.s3control.model.CreateAccessPointRequest-
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> createAccessPoint(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Create::createAccessPoint for bucket: %s \n", ResourceModel.TYPE_NAME, model.getBucket()));

        return proxy.initiate("AWS-S3Outposts-AccessPoint::Create::CreateAccessPoint", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToCreateAPRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall(((createAccessPointRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(createAccessPointRequest, s3ControlProxyClient.client()::createAccessPoint)))
                .stabilize((createAccessPointRequest, createAccessPointResponse, s3ControlProxyClient, resourceModel, cbContext) -> {
                    if (createAccessPointResponse.accessPointArn() != null) {
                        // AN TODO: [P0]: Remove the following block of code before making the resource public.
                        // Start: Code Block
                        // Get outpostId from bucket arn and replace `ec2` with outpostId in accesspoint arn.
                        final BucketArnFields arnFields = BucketArnFields.splitArn(model.getBucket());
                        String accessPointArn = createAccessPointResponse.accessPointArn()
                                .replaceFirst("/ec2/", String.format("/%s/", arnFields.outpostId));
                        logger.log(String.format("%s::Create::createAccessPoint - AccessPoint ARN: %s \n", ResourceModel.TYPE_NAME, accessPointArn));
                        // End: Code Block
                        resourceModel.setArn(accessPointArn);
                        cbContext.setStabilized(true);
                        return true;
                    }
                    return false;
                })
                .handleError(this::handleError)
                .progress();

    }

    /**
     * Calls the API putAccessPointPolicy
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#putAccessPointPolicy-software.amazon.awssdk.services.s3control.model.PutAccessPointPolicyRequest-
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> putAccessPointPolicy(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Create::putAccessPointPolicy for accesspoint: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        if (model.getPolicy() != null) {

            return proxy.initiate("AWS-S3Outposts-AccessPoint::Create::PutAccessPointPolicy", proxyClient, model, callbackContext)
                    .translateToServiceRequest(resourceModel ->
                            Translator.translateToPutAPPolicyRequest(resourceModel, request.getAwsAccountId()))
                    .makeServiceCall(((putAccessPointPolicyRequest, s3ControlProxyClient) ->
                            s3ControlProxyClient.injectCredentialsAndInvokeV2(putAccessPointPolicyRequest, s3ControlProxyClient.client()::putAccessPointPolicy)))
                    .handleError(this::handleError)
                    .progress();

        } else {

            return ProgressEvent.progress(model, callbackContext);

        }

    }

}
