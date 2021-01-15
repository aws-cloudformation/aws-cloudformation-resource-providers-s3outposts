package software.amazon.s3outposts.accesspoint;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.s3control.model.ListAccessPointsResponse;
import software.amazon.cloudformation.proxy.*;

import java.util.stream.Collectors;


public class ListHandler extends BaseHandler<CallbackContext> {

    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        if (model == null || StringUtils.isNullOrEmpty(model.getBucket())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, "Bucket ARN is required.");
        }

        try {
            final ListAccessPointsResponse listAccessPointsResponse = proxy.injectCredentialsAndInvokeV2(
                    Translator.translateToListRequest(model, request.getAwsAccountId(), request.getNextToken()),
                    ClientBuilder.getClient()::listAccessPoints);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(
                            listAccessPointsResponse.accessPointList()
                                    .stream()
                                    .map(accessPoint -> Translator.translateFromAccessPoint(accessPoint, model))
                                    .collect(Collectors.toList())
                    )
                    .nextToken(listAccessPointsResponse.nextToken())
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.GeneralServiceException, e.getMessage());
        }

    }

}
