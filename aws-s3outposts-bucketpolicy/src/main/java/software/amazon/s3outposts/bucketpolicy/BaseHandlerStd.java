package software.amazon.s3outposts.bucketpolicy;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    // Error Codes
    protected static final String ACCESS_DENIED_ERROR_CODE = "AccessDenied";
    protected static final String NO_SUCH_BUCKET_ERROR_CODE = "NoSuchBucket";
    protected static final String NO_SUCH_BUCKET_POLICY_ERROR_CODE = "NoSuchBucketPolicy";
    protected static final String MALFORMED_POLICY_ERROR_CODE = "MalformedPolicy";

    // Error Messages (will be returned to the customer)
    protected static final String BUCKET_ARN_REQD = "Bucket ARN is required.";
    protected static final String POLICY_DOC_REQD = "Policy Document is required.";
    protected static final String BUCKET_POLICY_EXISTS = "Bucket Policy already exists.";
    protected static final String BUCKET_POLICY_MISSING = "Bucket Policy does not exist.";
    protected static final String POLICY_NOT_EMPTY = "Bucket Policy must be empty.";

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
        BaseHandlerException ex = null;
        try {
            throw exception;
        } catch (BadRequestException | InvalidRequestException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, e.getMessage());
        } catch (InternalServiceException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ServiceInternalError, e.getMessage());
        } catch (NotFoundException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
        } catch (S3ControlException e) {
            switch (e.awsErrorDetails().errorCode()) {
                case MALFORMED_POLICY_ERROR_CODE: // 400
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, e.getMessage());
                case ACCESS_DENIED_ERROR_CODE: // 403
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.AccessDenied, e.getMessage());
                case NO_SUCH_BUCKET_POLICY_ERROR_CODE: // 404
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
                default:
                    ex = new CfnGeneralServiceException(e);
                    return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
            }
        } catch (Exception e) {
            ex = new CfnGeneralServiceException(e);
            return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
        }
    }

}
