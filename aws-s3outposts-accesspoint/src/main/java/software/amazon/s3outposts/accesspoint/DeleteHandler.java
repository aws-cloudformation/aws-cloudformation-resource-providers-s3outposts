package software.amazon.s3outposts.accesspoint;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.s3control.S3ControlClient;
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

        // Expecting customer to only provide Arn
        if (model == null || StringUtils.isNullOrEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, ACCESSPOINT_ARN_REQD);
        }

        logger.log(String.format("Access:DeleteHandler called for arn: %s \n", model.getArn()));

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> deleteAccessPoint(proxy, proxyClient, request, progress, logger));
    }

    private ProgressEvent<ResourceModel, CallbackContext> deleteAccessPoint(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<S3ControlClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final Logger logger) {

        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext context = progressEvent.getCallbackContext();

        return proxy.initiate("AWS-S3Outposts-AccessPoint::Delete", proxyClient, model, context)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToDeleteRequest(resourceModel, request.getAwsAccountId()))
                .makeServiceCall((deleteAccessPointRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(deleteAccessPointRequest, s3ControlProxyClient.client()::deleteAccessPoint)
                )
                .handleError(this::handleError)
                .done(deleteAccessPointResponse -> ProgressEvent.defaultSuccessHandler(null));
    }

}
