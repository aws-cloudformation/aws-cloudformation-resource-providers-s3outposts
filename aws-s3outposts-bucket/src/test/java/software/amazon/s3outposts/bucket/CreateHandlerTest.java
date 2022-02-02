package software.amazon.s3outposts.bucket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private CreateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    // Constants
    private static final ResourceModel CREATE_NOBUCKETNAME_MODEL = ResourceModel.builder()
            .outpostId(OUTPOST_ID)
            .build();

    private static final ResourceModel CREATE_NOOUTPOSTID_MODEL = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .build();

    // Mock variables
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    // Pre-, Post- test steps
    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(proxyClient.client());
    }

    // Tests

    /**
     * Validation error - no bucket name provided in input model
     */
    @Test
    public void handleRequest_Success_NoBucketName() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(CREATE_NOBUCKETNAME_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getCallbackContext()).isEqualToComparingFieldByField(new CallbackContext());
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isEqualTo("Bucket Name is required.");
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

    }


    /**
     * Validation error - no outpostId provided in input model
     */
    @Test
    public void handleRequest_Success_NoOutpostId() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(CREATE_NOOUTPOSTID_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getCallbackContext()).isEqualToComparingFieldByField(new CallbackContext());
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isEqualTo("OutpostId is required.");
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

    }

    /**
     * Happy Path - Bucket creation succeeded, but still waiting for propagation
     */
    @Test
    public void handleRequest_Success_Pending() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_NO_TAGS_AND_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(progressEvent.getCallbackContext().stabilized).isEqualTo(true);
        assertThat(progressEvent.getCallbackContext().propagated).isEqualTo(false);
        assertThat(progressEvent.getCallbackContext().forcedDelayCount).isEqualTo(1);
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(20);
        assertThat(progressEvent.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }


    /**
     * Happy Path - Bucket no tags and lifecycle configuration provided, propagation completed
     */
    @Test
    public void handleRequest_SuccessComplete_NoTagsAndRules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_NO_TAGS_AND_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_TAGS);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Bucket with empty tags, no lifecycle configuration, propagation completed
     */
    @Test
    public void handleRequest_SuccessComplete_WithEmptyTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_EMPTY_TAGS)
                .desiredResourceTags(new HashMap<>())
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutBucketTaggingResponse putBucketTaggingResponse = PutBucketTaggingResponse.builder().build();
        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class))).thenReturn(putBucketTaggingResponse);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().tagSet(Collections.emptyList()).build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_TAGS);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Bucket with Tags provided, no lifecycle configuration, propagation completed
     */
    @Test
    public void handleRequest_SuccessComplete_WithTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .desiredResourceTags(TAG_MAP)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutBucketTaggingResponse putBucketTaggingResponse = PutBucketTaggingResponse.builder().build();
        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class))).thenReturn(putBucketTaggingResponse);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().tagSet(S3TAG_LIST1).build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_TAGS);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - BucketAlreadyExists
     */
    @Test
    public void handleRequest_AlreadyExists() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(BucketAlreadyExistsException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - BucketAlreadyOwnedByYou
     */
    @Test
    public void handleRequest_AlreadyOwnedByYou() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(BucketAlreadyOwnedByYouException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - InvalidRequest - 400
     */
    @Test
    public void handleRequest_400() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(400).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - AccessDenied - 403
     */
    @Test
    public void handleRequest_403() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(403).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - NotFound - 404
     */
    @Test
    public void handleRequest_404() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(404).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - Conflict - 409
     */
    @Test
    public void handleRequest_409() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(409).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ResourceConflict);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - InternalError - 500
     */
    @Test
    public void handleRequest_500() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(500).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - Throttling - 503
     */
    @Test
    public void handleRequest_503() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(503).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - RequestTimeout - 408
     */
    @Test
    public void handleRequest_OtherStatusCodes() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(408).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - Bucket creation succeeds, tagging fails - TooManyTags
     */
    @Test
    public void handleRequest_Error_Tagging_TooManyTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .desiredResourceTags(TAG_MAP)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class)))
                .thenThrow(TooManyTagsException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(context);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_TAGS);
        assertThat(progress.getResourceModels()).isNull();


        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - Bucket creation succeeds, tagging fails - AccessDenied
     */
    @Test
    public void handleRequest_Error_Tagging_AccessDenied() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .desiredResourceTags(TAG_MAP)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(403));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(context);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_TAGS);
        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();


    }

    /**
     * Happy Path - Bucket creation with resource tags and system tags
     */
    @Test
    public void handleRequest_Tagging_SystemTags() {

        Tag sysTag1 = Tag.builder().key("aws:key1").value("value1").build();
        Tag sysTag2 = Tag.builder().key("AWS:key2").value(ARN).build();

        S3Tag sysS3Tag1 = S3Tag.builder().key("aws:key1").value("value1").build();
        S3Tag sysS3Tag2 = S3Tag.builder().key("AWS:key2").value(ARN).build();

        Set<Tag> allTagSet = new HashSet<Tag>() {{
            add(TAG1);
            add(TAG2);
            add(sysTag1);
            add(sysTag2);
        }};
        List<S3Tag> allS3TagList = Arrays.asList(S3TAG1, S3TAG2, sysS3Tag1, sysS3Tag2);

        Map<String, String> sysTagMap = new HashMap<String, String>() {{
            put(sysTag1.getKey(), sysTag1.getValue());
            put(sysTag2.getKey(), sysTag2.getValue());
        }};

        final ResourceModel BUCKET_MODEL_WITH_SYSTEM_TAGS = ResourceModel.builder()
                .arn(ARN)
                .bucketName(BUCKET_NAME)
                .outpostId(OUTPOST_ID)
                .tags(allTagSet)
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_WITH_TAGS)
                .desiredResourceTags(TAG_MAP)
                .systemTags(sysTagMap)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutBucketTaggingResponse putBucketTaggingResponse = PutBucketTaggingResponse.builder().build();
        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class))).thenReturn(putBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().tagSet(allS3TagList).build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_SYSTEM_TAGS);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Bucket creation with no tags and lifecycle configuration with no rules, propagation completed
     */
    @Test
    public void handleRequest_Lifecycle_NoRules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_NO_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutBucketLifecycleConfigurationResponse putBucketLifecycleConfigurationResponse =
                PutBucketLifecycleConfigurationResponse.builder().build();
        when(proxyClient.client().putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class)))
                .thenReturn(putBucketLifecycleConfigurationResponse);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().rules(Collections.emptyList()).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_RULES);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Bucket creation with no tags, lifecycle configuration with empty rule list, propagation completed
     */
    @Test
    public void handleRequest_Lifecycle_EmptyRules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_EMPTY_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutBucketLifecycleConfigurationResponse putBucketLifecycleConfigurationResponse =
                PutBucketLifecycleConfigurationResponse.builder().build();
        when(proxyClient.client().putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class)))
                .thenReturn(putBucketLifecycleConfigurationResponse);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().rules(Collections.emptyList()).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_RULES);
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Bucket creation with no tags, lifecycle configuration with rules, propagation completed
     */
    @Test
    public void handleRequest_Lifecycle_Rules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutBucketLifecycleConfigurationResponse putBucketLifecycleConfigurationResponse =
                PutBucketLifecycleConfigurationResponse.builder().build();
        when(proxyClient.client().putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class)))
                .thenReturn(putBucketLifecycleConfigurationResponse);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().rules(LIFECYCLE_RULE_LIST).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - Lifecycle Configuration - Bad Request
     */
    @Test
    public void handleRequest_Lifecycle_Error() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        when(proxyClient.client().putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class)))
                .thenThrow(BadRequestException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(context);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_RULES);
        assertThat(progress.getResourceModels()).isNull();


        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Bucket creation with tags and lifecycle configuration with rules - propagation completed
     */
    @Test
    public void handleRequest_Complete_Success() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_COMPLETE_MODEL)
                .desiredResourceTags(TAG_MAP)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutBucketTaggingResponse putBucketTaggingResponse = PutBucketTaggingResponse.builder().build();
        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class))).thenReturn(putBucketTaggingResponse);

        final PutBucketLifecycleConfigurationResponse putBucketLifecycleConfigurationResponse =
                PutBucketLifecycleConfigurationResponse.builder().build();
        when(proxyClient.client().putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class)))
                .thenReturn(putBucketLifecycleConfigurationResponse);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().tagSet(S3TAG_LIST1).build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().rules(LIFECYCLE_RULE_LIST).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_COMPLETE_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
