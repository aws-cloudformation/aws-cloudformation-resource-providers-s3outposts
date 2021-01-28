package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.S3OutpostsException;
import software.amazon.awssdk.services.s3outposts.model.S3OutpostsRequest;
import software.amazon.cloudformation.proxy.*;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    // Error messages (returned to the customer)
    protected static final String INVALID_INPUT = "OutpostId, SecurityGroupId, SubnetId are required parameters.";
    protected static final String ENDPOINT_ARN_REQD = "Endpoint ARN is required.";

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger);


    /**
     * Common error handling function. Used by all handlers of the Endpoint resource.
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/S3OutpostsException.html
     *
     * @param request
     * @param exception
     * @param proxyClient
     * @param resourceModel
     * @param callbackContext
     * @return
     */
    public ProgressEvent<ResourceModel, CallbackContext> handleError(
            final S3OutpostsRequest request,
            final Exception exception,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final ResourceModel resourceModel,
            final CallbackContext callbackContext
    ) throws Exception {

        try {
            throw exception;
        } catch (S3OutpostsException e) {

            switch (e.statusCode()) {
                case 400:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, e.getMessage());
                case 403:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.AccessDenied, e.getMessage());
                case 404:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
                case 409:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ResourceConflict, e.getMessage());
                case 500:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ServiceInternalError, e.getMessage());
                default:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());
            }

        } catch (SdkException e) {

            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());

        }

    }


}
