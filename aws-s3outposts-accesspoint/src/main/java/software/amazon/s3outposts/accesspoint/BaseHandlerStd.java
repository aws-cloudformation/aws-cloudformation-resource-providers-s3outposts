package software.amazon.s3outposts.accesspoint;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.*;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    // Error Codes
    protected static final String ACCESSPOINT_ALREADY_OWNED_BY_YOU = "AccessPointAlreadyOwnedByYou";
    protected static final String INVALID_ACCESSPOINT = "InvalidAccessPoint";
    protected static final String NO_SUCH_ACCESSPOINT = "NoSuchAccessPoint";
    protected static final String NO_SUCH_ACCESSPOINT_POLICY = "NoSuchAccessPointPolicy";
    protected static final String TOO_MANY_ACCESSPOINTS = "TooManyAccessPoints";
    protected static final String MALFORMED_POLICY_ERROR_CODE = "MalformedPolicy";
    protected static final String ACCESS_DENIED_ERROR_CODE = "AccessDenied";
    protected static final String INVALID_REQUEST = "InvalidRequest";

    // Error messages returned to the customer.
    protected static final String ACCESSPOINT_ARN_REQD = "AccessPoint ARN is required.";
    protected static final String ACCESSPOINT_NAME_REQD = "AccessPoint Name is required.";
    protected static final String BUCKET_ARN_REQD = "Bucket ARN is required.";
    protected static final String VPC_CONFIGURATION_REQD = "VpcConfiguration is required.";
    protected static final String INVALID_ACCESSPOINT_STATE = "Access Point is not in a state where it can be deleted";

    // Constants
    protected static final int CALLBACK_DELAY_SECONDS = 20;
    protected static final int MAX_STABILIZATION_RETRIES = 10;
    protected static final String MAX_RETRY_ATTEMPTS = "Maximum number of Stabilization attempts reached. Returning SUCCESS.";

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
        } catch (InvalidNextTokenException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());
        } catch (NotFoundException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
        } catch (TooManyRequestsException e) {
            return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.Throttling, e.getMessage());
        } catch (S3ControlException e) {
            switch (e.awsErrorDetails().errorCode()) {
                case ACCESSPOINT_ALREADY_OWNED_BY_YOU: // 409
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.AlreadyExists, e.getMessage());
                case INVALID_ACCESSPOINT:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, e.getMessage());
                case NO_SUCH_ACCESSPOINT:
                case NO_SUCH_ACCESSPOINT_POLICY:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.NotFound, e.getMessage());
                case TOO_MANY_ACCESSPOINTS:
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.ServiceLimitExceeded, e.getMessage());
                case MALFORMED_POLICY_ERROR_CODE: // 400
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.InvalidRequest, e.getMessage());
                case ACCESS_DENIED_ERROR_CODE: // 403
                    return ProgressEvent.failed(resourceModel, callbackContext, HandlerErrorCode.AccessDenied, e.getMessage());
                default:
                    ex = new CfnGeneralServiceException(e);
                    return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
            }
        } catch (Exception e) {
            ex = new CfnGeneralServiceException(e);
            return ProgressEvent.failed(resourceModel, callbackContext, ex.getErrorCode(), ex.getMessage());
        }
    }


    /**
     * Adds a total delay of 80s (CALLBACK_DELAY_SECONDS * forcedDelayCount) to allow the AccessPoint state
     * to transition from "Associated" to "Active".
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
        if (callbackContext.isPropagated()) {
            return progressEvent;
        }
        callbackContext.forcedDelayCount++;
        if (callbackContext.forcedDelayCount == 4) {
            callbackContext.setPropagated(true);
        }
        return ProgressEvent.defaultInProgressHandler(callbackContext, CALLBACK_DELAY_SECONDS, progressEvent.getResourceModel());

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
    protected ProgressEvent<ResourceModel, CallbackContext> getAccessPoint(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3ControlClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ResourceModel model,
            CallbackContext callbackContext,
            Logger logger) {

        logger.log(String.format("%s::Read::GetAccessPoint - arn: %s \n", ResourceModel.TYPE_NAME, model.getArn()));
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

}
