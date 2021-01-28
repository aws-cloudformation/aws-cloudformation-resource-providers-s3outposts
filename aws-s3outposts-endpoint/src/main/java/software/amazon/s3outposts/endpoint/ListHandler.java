package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.cloudformation.proxy.*;

import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3OutpostsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> listEndpoints(proxy, proxyClient, request, progress, logger));

    }

    /**
     * Calling the API listEndpoints
     *
     * @param proxy
     * @param proxyClient
     * @param request
     * @param progress
     * @param logger
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> listEndpoints(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<S3OutpostsClient> proxyClient,
            ResourceHandlerRequest<ResourceModel> request,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            Logger logger) {

        ResourceModel model = progress.getResourceModel();
        CallbackContext callbackContext = progress.getCallbackContext();

        if (request.getNextToken() != null) {
            logger.log(String.format("%s::List::listEndpoints - Account: %s, NextToken: %s",
                    ResourceModel.TYPE_NAME, request.getAwsAccountId(), request.getNextToken()));
        } else {
            logger.log(String.format("%s::List::listEndpoints - Account: %s",
                    ResourceModel.TYPE_NAME, request.getAwsAccountId()));
        }

        return proxy.initiate("AWS-S3Outposts-Endpoint::List::ListEndpoints", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToSdkListEndpointsRequest(request.getNextToken())
                )
                .makeServiceCall((listEndpointsRequest, s3ControlProxyClient) ->
                        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/ListEndpointsRequest.html
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(listEndpointsRequest,
                                s3ControlProxyClient.client()::listEndpoints))
                .handleError(this::handleError)
                .done(listEndpointsResponse ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModels(
                                        listEndpointsResponse.endpoints()
                                                .stream()
                                                .map(endpoint -> Translator.translateFromSdkEndpoint(endpoint))
                                                .collect(Collectors.toList())
                                )
                                .nextToken(listEndpointsResponse.nextToken())
                                .status(OperationStatus.SUCCESS)
                                .build());


    }
}
