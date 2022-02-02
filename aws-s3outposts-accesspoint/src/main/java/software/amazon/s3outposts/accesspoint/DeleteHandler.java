package software.amazon.s3outposts.accesspoint;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
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
        if (callbackContext.getStabilizationCount() == 0) {
            callbackContext.setStabilizationCount(MAX_STABILIZATION_RETRIES);
        }
        // Expecting customer to only provide Arn
        if (model == null || StringUtils.isNullOrEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, ACCESSPOINT_ARN_REQD);
        }

        logger.log(String.format("%s::DeleteHandler called for arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> deleteAccessPoint(proxy, proxyClient, request, progress, logger))
                .then(progress -> BaseHandlerStd.propagate(progress, logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    /**
     * Calls the API deleteAccessPoint
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progressEvent
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> deleteAccessPoint(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<S3ControlClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final Logger logger) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        logger.log(String.format("%s::Delete::deleteAccessPoint called for arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        return proxy.initiate("AWS-S3Outposts-AccessPoint::Delete", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToDeleteAPRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((deleteAccessPointRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteAccessPointRequest, s3ControlProxyClient.client()::deleteAccessPoint)
                )
                .stabilize((deleteAccessPointRequest, deleteAccessPointResponse, s3ControlProxyClient, resourceModel, cbContext) -> {
                    if(cbContext.stabilizationCount == 0) {
                        logger.log(MAX_RETRY_ATTEMPTS);
                        return true;
                    }
                    logger.log(String.format("Stabilization retries remaining: %d", cbContext.getStabilizationCount()));
                    ProgressEvent<ResourceModel, CallbackContext> prog = getAccessPoint(proxy, proxyClient, request, resourceModel, cbContext, logger);
                    if(prog.isFailed() && prog.getErrorCode().equals(HandlerErrorCode.NotFound)) {
                        logger.log(String.format("AccessPoint ARN: %s is Deleting", resourceModel.getArn()));
                        cbContext.setStabilized(true);
                    } else {
                        logger.log(String.format("AccessPoint ARN: %s is not yet Deleting", resourceModel.getArn()));
                        cbContext.setStabilized(false);
                    }
                    cbContext.stabilizationCount--;
                    return cbContext.stabilized;
                })
                .handleError((deleteAccessPointRequest, exception, s3ControlProxyClient, resourceModel, cbContext) -> {
                    if (exception instanceof S3ControlException && ((S3ControlException) exception).statusCode() == 400 &&
                            ((S3ControlException) exception).awsErrorDetails().errorCode().equals(INVALID_REQUEST) &&
                            ((S3ControlException) exception).awsErrorDetails().errorMessage().equals(INVALID_ACCESSPOINT_STATE)) {
                        logger.log(String.format("%s::Delete::handleRequest - Error Msg: %s",
                                ResourceModel.TYPE_NAME, ((S3ControlException) exception).awsErrorDetails().errorMessage()));
                        return ProgressEvent.defaultInProgressHandler(cbContext, 20, resourceModel);
                    }
                    logger.log(String.format("%s::Delete::handleRequest - Error type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                    return handleError(deleteAccessPointRequest, exception, s3ControlProxyClient, resourceModel, cbContext);

                })
                .progress();
    }

}
