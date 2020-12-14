package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    // Error messages (returned to customer)
    protected static final String BUCKET_ARN_REQD = "Bucket ARN is required.";
    protected static final String OUTPOSTID_REQD = "OutpostId is required.";

    // Constants
    protected static final int CALLBACK_DELAY_SECONDS = 20;

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
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger);

    /**
     * Common error handling function. It is used by all handlers.
     *
     * @param request
     * @param exception
     * @param proxyClient
     * @param resourceModel
     * @param callbackContext
     * @return FAILED ProgressEvent
     * @throws Exception
     */
    public ProgressEvent<ResourceModel, CallbackContext> handleError(
            final S3ControlRequest request,
            final Exception exception,
            final ProxyClient<S3ControlClient> proxyClient,
            final ResourceModel resourceModel,
            final CallbackContext callbackContext
    ) throws Exception {
        try {
            throw exception;
        } catch (BadRequestException | InvalidRequestException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, e.getMessage());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.AlreadyExists, e.getMessage());
        } catch (InternalServiceException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ServiceInternalError, e.getMessage());
        } catch (InvalidNextTokenException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());
        } catch (NotFoundException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
        } catch (TooManyRequestsException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.Throttling, e.getMessage());
        } catch (TooManyTagsException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ServiceLimitExceeded, e.getMessage());
        } catch (S3ControlException e) {
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
                case 503:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.Throttling, e.getMessage());
                default:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());
            }
        } catch (SdkException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());
        }
    }

    /**
     * Adds a delay of 40s to allow the Bucket state to transition from "Associated" to "Active".
     *
     * @param progressEvent
     * @param logger
     * @return
     */
    protected static ProgressEvent<ResourceModel, CallbackContext> propagate(final ProgressEvent<ResourceModel,
            CallbackContext> progressEvent, Logger logger) {
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        logger.log(String.format("Waiting for propagation delay.. Count: %d, Propagated: %s",
                callbackContext.forcedDelayCount, callbackContext.isPropagated()));
        if (callbackContext.isPropagated()) return progressEvent;
        callbackContext.forcedDelayCount++;
        if (callbackContext.forcedDelayCount == 2) {
            callbackContext.setPropagated(true);
        }
        return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, progressEvent.getResourceModel());
    }
}
