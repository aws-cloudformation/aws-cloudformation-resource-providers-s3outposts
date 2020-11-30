package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
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

    protected ProgressEvent<ResourceModel, CallbackContext> handleException(Exception exception, Logger logger) throws Exception {
        try {
            throw exception;
        } catch (BadRequestException | InvalidRequestException e) {
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.InvalidRequest);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.AlreadyExists);
        } catch (InternalServiceException e) {
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.ServiceInternalError);
        } catch (InvalidNextTokenException e) {
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.GeneralServiceException);
        } catch (NotFoundException e) {
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
        } catch (TooManyRequestsException e) {
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.Throttling);
        } catch (TooManyTagsException e) {
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.ServiceLimitExceeded);
        } catch (S3ControlException e) {
            switch (e.statusCode()) {
                case 400:
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.InvalidRequest);
                case 403:
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.AccessDenied);
                case 404:
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.NotFound);
                case 409:
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ResourceConflict);
                case 500:
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.ServiceInternalError);
                case 503:
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.Throttling);
                default:
                    return ProgressEvent.defaultFailureHandler(exception, HandlerErrorCode.GeneralServiceException);
            }
        } catch (SdkException e) {
            logger.log(String.format("Got error of type %s", e.getClass().getCanonicalName()));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
        }
    }
}
