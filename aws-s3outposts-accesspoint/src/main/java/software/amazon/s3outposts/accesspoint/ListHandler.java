package software.amazon.s3outposts.accesspoint;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.proxy.*;

import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        if (model == null || StringUtils.isNullOrEmpty(model.getBucket())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Bucket ARN is required.");
        }

        return proxy.initiate("AWS-S3Outposts-AccessPoint::List::ListAccessPoints", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToListRequest(resourceModel, request.getAwsAccountId(), request.getNextToken()))
                .makeServiceCall(((listAccessPointsRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(listAccessPointsRequest, s3ControlProxyClient.client()::listAccessPoints)))
                .handleError(this::handleError)
                .done(listAccessPointsResponse ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModels(
                                        listAccessPointsResponse.accessPointList()
                                                .stream()
                                                .map(accessPoint -> Translator.translateFromAccessPoint(accessPoint, model))
                                                .collect(Collectors.toList())
                                )
                                .nextToken(listAccessPointsResponse.nextToken())
                                .status(OperationStatus.SUCCESS)
                                .build()
                );

    }

}
