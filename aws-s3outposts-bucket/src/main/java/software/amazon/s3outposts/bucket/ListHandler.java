package software.amazon.s3outposts.bucket;

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

        // Expecting customer to only provide the OutpostId
        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/S3ControlClient.html#listRegionalBuckets-software.amazon.awssdk.services.s3control.model.ListRegionalBucketsRequest-
        if (StringUtils.isNullOrEmpty(model.getOutpostId())) {
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, OUTPOSTID_REQD);
        }

        return proxy.initiate("AWS-S3Outposts-Bucket::List::ListRegionalBuckets", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel ->
                        Translator.translateToListRequest(resourceModel, request.getAwsAccountId(), request.getNextToken()))
                .makeServiceCall(((listRegionalBucketsRequest, s3ControlProxyClient) ->
                        s3ControlProxyClient.injectCredentialsAndInvokeV2(listRegionalBucketsRequest, s3ControlProxyClient.client()::listRegionalBuckets)))
                .handleError(this::handleError)
                .done(listRegionalBucketsResponse ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .resourceModels(
                                        listRegionalBucketsResponse.regionalBucketList()
                                                .stream()
                                                .map(regionalBucket -> Translator.translateFromRegionalBucket(regionalBucket, model))
                                                .collect(Collectors.toList())
                                )
                                .nextToken(listRegionalBucketsResponse.nextToken())
                                .status(OperationStatus.SUCCESS)
                                .build()
                );

    }
}
