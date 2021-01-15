package software.amazon.s3outposts.accesspoint;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
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

        // Expecting customer to only provide Arn
        if (model == null || StringUtils.isEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, ACCESSPOINT_ARN_REQD);
        }
        logger.log(String.format("AccessPoint::ReadHandler called for arn: %s \n", model.getArn()));

        return (ProgressEvent.progress(model, callbackContext))
                .then(progress -> getAccessPoint(proxy, proxyClient, request, progress.getResourceModel(), progress.getCallbackContext(), logger))
                .then(progress -> getAccessPointPolicy(proxy, proxyClient, request, progress.getResourceModel(), progress.getCallbackContext(), logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));

    }


    /**
     * Calls the API getAccessPoint
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#getAccessPoint-software.amazon.awssdk.services.s3control.model.GetAccessPointRequest-
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param model
     * @param callbackContext
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> getAccessPoint(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ResourceModel model,
            CallbackContext callbackContext,
            Logger logger) {

        logger.log(String.format("AccessPoint::Read::GetAccessPoint - arn: %s \n", model.getArn()));
        return proxy.initiate("AWS-S3Outposts-AccessPoint::Read::GetAccessPoint", proxyClient, model, callbackContext)
                // Form GetAccessPointRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToGetAPRequest(resourceModel, request.getAwsAccountId()))
                // Issue call getAccessPoint
                .makeServiceCall((getAPRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getAPRequest, s3ControlProxyClient.client()::getAccessPoint)
                )
                .handleError(this::handleError)
                .done(getAPResponse -> {
                    final ResourceModel getAPResponseModel = Translator.translateFromGetAPResponse(getAPResponse, model);
                    return ProgressEvent.progress(getAPResponseModel, callbackContext);
                });

    }

    /**
     * Calls the API getAccessPointPolicy
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#getAccessPointPolicy-software.amazon.awssdk.services.s3control.model.GetAccessPointPolicyRequest-
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param model
     * @param callbackContext
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> getAccessPointPolicy(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ResourceModel model,
            CallbackContext callbackContext,
            Logger logger) {

        logger.log(String.format("AccessPoint::Read::GetAccessPointPolicy - arn: %s \n", model.getArn()));
        return proxy.initiate("AWS-S3Outposts-AccessPoint::Read::GetAccessPointPolicy", proxyClient, model, callbackContext)
                // Form GetAccessPointPolicyRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToGetAPPolicyRequest(resourceModel, request.getAwsAccountId()))
                // Issue call getAccessPoint
                .makeServiceCall((getAPPolicyRequest, s3ControlProxyClient) -> {
                    return s3ControlProxyClient.injectCredentialsAndInvokeV2(getAPPolicyRequest, s3ControlProxyClient.client()::getAccessPointPolicy);
                })
                .handleError((getAPPolicyRequest, exception, client, resourceModel, cbContext) -> {
                    // It is ok to not have an AccessPointPolicy for a CFN AccessPoint resource. We do not have to fail the CFN AccessPoint::Read operation for this.
                    if (exception instanceof S3ControlException && StringUtils.equals(((S3ControlException) exception).awsErrorDetails().errorCode(), NO_SUCH_ACCESSPOINT_POLICY)) {
                        logger.log(String.format("NoSuchAccessPointPolicy, Message: %s \n", exception.getMessage()));
                        return ProgressEvent.progress(resourceModel, cbContext);
                    } else {
                        logger.log(String.format("API getAccessPointPolicy failed with exception: %s", exception.getMessage()));
                        return handleError(getAPPolicyRequest, exception, client, resourceModel, cbContext);
                    }
                })
                .done(getAPPolicyResponse -> {
                    try {
                        final ResourceModel getAPPolicyResponseModel = Translator.translateFromGetAPPolicyResponse(getAPPolicyResponse, model);
                        return ProgressEvent.progress(getAPPolicyResponseModel, callbackContext);
                    } catch (Exception exception) {
                        logger.log("Failed to translate from GetAccessPointPolicyResponse");
                        return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.GeneralServiceException, exception.getMessage());
                    }
                });

    }

}
