package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    // Error messages (returned to customer)
    protected static final String BUCKET_ARN_REQD = "Bucket ARN is required.";
    protected static final String BUCKET_NAME_REQD = "Bucket Name is required.";
    protected static final String OUTPOSTID_REQD = "OutpostId is required.";
    protected static final String BUCKET_DOES_NOT_EXIST = "Bucket does not exist.";

    // Error Codes
    protected static final String NO_SUCH_TAGSET = "NoSuchTagSet";
    protected static final String NO_SUCH_LIFECYCLE_CONFIGURATION = "NoSuchLifecycleConfiguration";
    protected static final String INVALID_BUCKET_STATE = "InvalidBucketState";

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

    /**
     * Calls the API getBucket
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    protected ProgressEvent<ResourceModel, CallbackContext> getBucket(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();
        logger.log(String.format("%s::Read::getBucket - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        // Initiate the callGraph and get the callContext
        return proxy.initiate("AWS-S3Outposts-Bucket::Read::GetBucket", proxyClient, model, callbackContext)
                // Form GetBucketRequest
                .translateToServiceRequest(resourceModel -> Translator.translateToReadRequest(resourceModel, request.getAwsAccountId()))
                // Issue call getBucket
                .makeServiceCall((getBucketRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(getBucketRequest, s3ControlProxyClient.client()::getBucket)
                )
                .handleError((getBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext) -> {
                    logger.log(String.format("%s - ReadHandler - Error Type: %s", ResourceModel.TYPE_NAME, exception.getClass().getCanonicalName()));
                    return handleError(getBucketRequest, exception, s3ControlProxyClient, resourceModel, cbContext);
                })
                .done(getBucketResponse -> {
                    // We send the Arn since the GetBucketResponse doesn't contain the Arn
                    final ResourceModel getBucketResponseModel = Translator.translateFromReadResponse(getBucketResponse, model.getArn());
                    return ProgressEvent.progress(getBucketResponseModel, callbackContext);
                });

    }

    /**
     * Calls the API putBucketTagging
     * While creating a bucket we will call this routine even if the user does not provide any resource tags.
     * This is because CFN may want to set system tags.
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    protected ProgressEvent<ResourceModel, CallbackContext> putBucketTagging(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Create/Update::putBucketTagging - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        if (request.getDesiredResourceTags() == null && request.getSystemTags() == null)
            return ProgressEvent.progress(model, callbackContext);

        if (request.getDesiredResourceTags() != null) {
            logger.log(String.format("%s::Create/Update::putBucketTagging - Sending resource tags \n", ResourceModel.TYPE_NAME));
            request.getDesiredResourceTags().entrySet().forEach(entry -> {
                logger.log(String.format("{Key: %s, Value: %s} ", entry.getKey(), entry.getValue()));
            });
        }

        if (request.getSystemTags() != null) {
            logger.log(String.format("%s::Create/Update::putBucketTagging - Sending system tags \n", ResourceModel.TYPE_NAME));
            request.getSystemTags().entrySet().forEach(entry -> {
                logger.log(String.format("{Key: %s, Value: %s}", entry.getKey(), entry.getValue()));
            });
        }

        return proxy.initiate("AWS-S3Outposts-Bucket::Create/Update::putBucketTagging", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToSdkPutBucketTaggingRequest(resourceModel,
                                request.getDesiredResourceTags(), request.getSystemTags(), request.getAwsAccountId())
                )
                .makeServiceCall((putBucketTaggingRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(putBucketTaggingRequest,
                                s3ControlProxyClient.client()::putBucketTagging)
                )
                .handleError(this::handleError)
                .progress();

    }

    /**
     * Calls the API putBucketLifecycleConfiguration
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    protected ProgressEvent<ResourceModel, CallbackContext> putLifecycleConfiguration(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        logger.log(String.format("%s::Create/Update::putLifecycleConfiguration - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));

        if (model.getLifecycleConfiguration() == null)
            return ProgressEvent.progress(model, callbackContext);


        return proxy.initiate("AWS-S3Outposts-Bucket::Create::PutBucketLifecycleConfiguration", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToSdkPutBucketLifecycleConfigurationRequest(resourceModel, request.getAwsAccountId())
                )
                .makeServiceCall((putBucketLifecycleConfigurationRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(putBucketLifecycleConfigurationRequest,
                                s3ControlProxyClient.client()::putBucketLifecycleConfiguration)
                )
                .handleError(this::handleError)
                .progress();


    }

}
