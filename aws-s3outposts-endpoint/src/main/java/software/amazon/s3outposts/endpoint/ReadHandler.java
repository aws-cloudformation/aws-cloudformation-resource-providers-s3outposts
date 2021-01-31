package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.*;

import java.util.List;
import java.util.Optional;

public class ReadHandler extends BaseHandlerStd {

    private Logger logger;

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // Expecting customer to only provide Arn
        if (model == null || StringUtils.isEmpty(model.getArn())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, ENDPOINT_ARN_REQD);
        }

        logger.log(String.format("%s::Read - ARN: %s", ResourceModel.TYPE_NAME, model.getArn()));

        return (ProgressEvent.progress(model, callbackContext))
                .then(progress -> listEndpoints(proxy, proxyClient, request, progress, logger))
                .then(progress -> getEndpoint(proxy, proxyClient, request, progress, logger))
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));

    }

    /**
     * In this routine, we traverse the List<ResourceModel> returned by calling `listEndpoints`.
     * While traversing, we search for an Arn match, using the Arn provided by the caller in the model.
     * Once we get an Arn match, we populate the model and return.
     * NOTE: Endpoint resource does not support a GetEndpoint API call. That's the reason why we have to call
     * the ListEndpoints API and use its output to populate the model in the ReadHandler.
     */
    private ProgressEvent<ResourceModel, CallbackContext> getEndpoint(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3OutpostsClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        ResourceModel model = progress.getResourceModel();
        final String arn = model.getArn();
        final List<ResourceModel> modelList = progress.getResourceModels();
        final CallbackContext context = progress.getCallbackContext();

        logger.log(String.format("%s::Read::getEndpoint - Finding endpoint with ARN: %s", ResourceModel.TYPE_NAME, arn));

        // Find first element in List<ResourceModel> which contains the provided Arn.
        // NOTE: Since the Arn is unique, we don't expect more than 1 element in List<ResourceModel> with the Arn.
        Optional<ResourceModel> modelOpt = modelList.stream()
                .filter(resourceModel -> resourceModel.getArn().equals(arn))
                .findFirst();

        // Check if model of type Optional is not empty
        if (modelOpt.isPresent()) {
            model = modelOpt.get();
        } else {
            return ProgressEvent.failed(model, context, HandlerErrorCode.NotFound, ENDPOINT_ARN_NOT_FOUND);
        }

        // Update the model in the ProgressEvent object
        progress.setResourceModel(model);
        return ProgressEvent.progress(model, context);

    }
}
