package software.amazon.s3outposts.accesspoint;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.s3control.S3ControlClient;
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
        final ResourceModel oldModel = request.getPreviousResourceState();

        // Expecting the customer to always provide the AccessPoint ARN along with other changes.
        if (newModel == null || StringUtils.isNullOrEmpty(newModel.getArn())) {
            return ProgressEvent.failed(newModel, callbackContext, HandlerErrorCode.InvalidRequest, ACCESSPOINT_ARN_REQD);
        }

        logger.log(String.format("AccessPoint::UpdateHandler called for arn: %s \n", newModel.getArn()));

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress -> putAccessPointPolicy(proxy, proxyClient, request, progress.getResourceModel(), progress.getCallbackContext(), oldModel, logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, progress.getCallbackContext(), proxyClient, logger));

    }

    /**
     * Calls the API putAccessPointPolicy
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#putAccessPointPolicy-software.amazon.awssdk.services.s3control.model.PutAccessPointPolicyRequest-
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param model
     * @param callbackContext
     * @param oldModel
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> putAccessPointPolicy(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ResourceModel model,
            CallbackContext callbackContext,
            ResourceModel oldModel,
            Logger logger) {

        if (model.getPolicy() != null) {

            return proxy.initiate("AWS-S3Outposts-AccessPoint::Update::PutAccessPointPolicy", proxyClient, model, callbackContext)
                    .translateToServiceRequest(resourceModel ->
                            Translator.translateToPutAPPolicyRequest(resourceModel, request.getAwsAccountId()))
                    .makeServiceCall(((putAPPolicyRequest, s3ControlProxyClient) ->
                            s3ControlProxyClient.injectCredentialsAndInvokeV2(putAPPolicyRequest, s3ControlProxyClient.client()::putAccessPointPolicy)))
                    .handleError(this::handleError)
                    .progress();

        } else {

            return proxy.initiate("AWS-S3Outposts-AccessPoint::Update::DeleteAccessPointPolicy", proxyClient, model, callbackContext)
                    .translateToServiceRequest(resourceModel ->
                            Translator.translateToDeleteAPPolicyRequest(resourceModel, request.getAwsAccountId()))
                    .makeServiceCall(((deleteAPPolicyRequest, s3ControlProxyClient) ->
                            s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteAPPolicyRequest, s3ControlProxyClient.client()::deleteAccessPointPolicy)))
                    .handleError(this::handleError)
                    .progress();

        }

    }

}
