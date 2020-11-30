package software.amazon.s3outposts.bucket;

import com.google.common.collect.Lists;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.ListRegionalBucketsResponse;
import software.amazon.cloudformation.proxy.*;

import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd {
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<S3ControlClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        // Expecting customer to only provide the OutpostId
        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#listRegionalBuckets-software.amazon.awssdk.services.s3control.model.ListRegionalBucketsRequest-
        logger.log(String.format("ListHandler invoked Account: %s, OutpostId: %s",
                request.getAwsAccountId(), model.getOutpostId()));

        List<ResourceModel> models = Lists.newArrayList();
        // Form ListRegionalBucketsRequest and make call to listRegionalBuckets
        final ListRegionalBucketsResponse response = proxyClient.injectCredentialsAndInvokeV2(
                Translator.translateToListRequest(model, request.getAwsAccountId(), request.getNextToken()),
                proxyClient.client()::listRegionalBuckets);
        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/ListRegionalBucketsResponse.html
        response.regionalBucketList()
                .stream()
                .map(regionalBucket -> Translator.translateFromRegionalBucket(regionalBucket, model))
                .collect(Collectors.toCollection(() -> models));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(response.nextToken())
                .build();

    }
}
