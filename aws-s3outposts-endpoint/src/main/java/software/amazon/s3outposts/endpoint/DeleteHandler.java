package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.*;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        CallbackContext currentContext = callbackContext.stabilizationRetriesRemaining == null ?
                CallbackContext.builder().stabilizationRetriesRemaining(NUMBER_OF_STABILIZATION_RETRIES).build() :
                callbackContext;

        // Expecting 2 inputs from the customer: endpointId, outpostId
        if (model == null || StringUtils.isEmpty(model.getArn()))
            return ProgressEvent.failed(model, currentContext, HandlerErrorCode.InvalidRequest, ENDPOINT_ARN_REQD);

        logger.log(String.format("%s::Delete - arn %s", ResourceModel.TYPE_NAME, model.getArn()));

        return proxy.initiate("AWS-S3Outposts-Endpoint::Delete", proxyClient, model, currentContext)
                // Translate CFN request to SDK compatible DeleteEndpointRequest
                .translateToServiceRequest(Translator::translateToSdkDeleteEndpointRequest)
                .backoffDelay(STABILIZATION_DELAY)
                .makeServiceCall((deleteEndpointRequest, s3OutpostsProxyClient) ->
                        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/DeleteEndpointRequest.html
                        s3OutpostsProxyClient.injectCredentialsAndInvokeV2(deleteEndpointRequest, s3OutpostsProxyClient.client()::deleteEndpoint))
                // Loop over the stabilize method until endpoint is actually deleted
                .stabilize((deleteEndpointRequest, deleteEndpointResponse, s3OutpostsProxyClient, resourceModel, cbContext) ->
                        stabilizedOnDelete(proxyClient, resourceModel, request, cbContext, proxy))
                .handleError(this::handleError)
                .done(deleteEndpointResponse ->
                        ProgressEvent.defaultSuccessHandler(null));

    }

    /**
     * Handler stabilize operation to wait till resource reaches terminal state (Deleted)
     * We check that by calling ListEndpoints -> GetEndpoints -> check the response status.
     *
     * @param proxyClient the aws service client to make the call
     * @param resourceModel resource model
     * @param request Request
     * @param cbContext Callback Context
     * @param proxy AmazonWebServicesClientProxy
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnDelete(
            ProxyClient<S3OutpostsClient> proxyClient,
            ResourceModel resourceModel,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext cbContext,
            AmazonWebServicesClientProxy proxy) {

        String clientRequestToken = request.getClientRequestToken();
        String endpointArn = resourceModel.getArn();
        String outpostId = EndpointArnFields.splitArn(endpointArn).outpostId;

        // Populate the outpost Id in the resourceModel, if present and not ec2.
        // ListHandler uses this field to modify ec2 ARNs to Navy ARNs
        if (outpostId != null && !outpostId.equals("ec2")) {
            resourceModel.setOutpostId(outpostId);
        }

        logger.log(String.format("[ClientRequestToken: %s] Stabilizing delete operation for Endpoint ARN: %s.",
                clientRequestToken, endpointArn));

        // If retries remaining are > 0 , then reduce the remaining retries by 1
        // Else if 0 retries are left, log timeout and return SUCCESS instead
        if (cbContext.getStabilizationRetriesRemaining() > 0) {
            cbContext.setStabilizationRetriesRemaining(cbContext.getStabilizationRetriesRemaining() - 1);
        } else {
            logger.log(MAX_RETRY_ATTEMPTS);
            logger.log(String.format("[ClientRequestToken: %s] Endpoint ARN: %s",
                    clientRequestToken, endpointArn));
            return true;
        }

        ProgressEvent<ResourceModel, CallbackContext> response = (ProgressEvent.progress(resourceModel, new CallbackContext()))
                .then(progress -> listEndpoints(proxy, proxyClient, request, progress, logger))
                .then(progress -> getEndpoint(progress, logger));

        // Failure with NotFound error denotes that endpoint was actually deleted.
        if (response.isFailed()) {
            if (response.getErrorCode().equals(HandlerErrorCode.NotFound) ) {
                logger.log(String.format("[ClientRequestToken: %s] Endpoint ARN: %s is Deleted.",
                        clientRequestToken, endpointArn));
                cbContext.setStabilized(true);
            // Failure with any other error denotes that read called failed for some other reason.
            } else {
                logger.log(String.format("[ClientRequestToken: %s] Endpoint ARN: %s Deletion failed with %s.",
                        clientRequestToken, endpointArn, response.getMessage()));
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, resourceModel.getArn());
            }
        // Success denotes that the endpoint hasn't been completely deleted yet because we can still read it.
        } else {
            logger.log(String.format("[ClientRequestToken: %s] Endpoint ARN: %s is Deleting.",
                    clientRequestToken, endpointArn));
        }

        return cbContext.stabilized;
    }
}
