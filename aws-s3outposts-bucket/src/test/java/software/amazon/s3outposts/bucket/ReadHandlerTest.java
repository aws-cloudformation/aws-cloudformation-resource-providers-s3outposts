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
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    private ReadHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

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
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * Validation Error - No Arn provided
     */
    @Test
    public void handleRequest_NoBucketArn() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_NO_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("Bucket ARN is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Happy Path - Return bucket with no tags, no lifecycle configuration
     */
    @Test
    public void handleRequest_SimpleSuccess_NoTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_TAGS);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Return bucket with tags, no lifecycle configuration
     */
    @Test
    public void handleRequest_SimpleSuccess_WithTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().tagSet(S3TAG_LIST1).build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_TAGS);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - GetBucket returns NotFound
     */
    @Test
    public void handleRequest_NotFound() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
                .thenThrow(NotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - GetBucket returns 400
     */
    @Test
    public void handleRequest_400() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
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

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - GetBucket returns 404
     */
    @Test
    public void handleRequest_404() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
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

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - GetBucketTagging returns TooManyTags
     */
    @Test
    public void handleRequest_Error_Tagging_TooManyTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(TooManyTagsException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("TooManyTags").build()).build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - GetBucketTagging returns NoSuchTagSet
     */
    @Test
    public void handleRequest_Error_Tagging_NoSuchTagSet() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(constructS3ControlExceptionWithErrorCode("NoSuchTagSet"));

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_NO_TAGS_AND_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle Configuration with no rules
     */
    @Test
    public void handleRequest_Lifecycle_NoRuleList() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_TAGS_AND_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle configuration with empty rule list
     */
    @Test
    public void handleRequest_Lifecycle_EmptyRuleList() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(Collections.emptyList()).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle configuration with rules
     */
    @Test
    public void handleRequest_Lifecycle_Rules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse = GetBucketLifecycleConfigurationResponse.builder().rules(LIFECYCLE_RULE_LIST).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - GetBucketLifecycleConfiguration returns InternalServiceException
     */
    @Test
    public void handleRequest_Lifecycle_Error() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(InternalServiceException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_IgnoreSystemTags() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_BUCKET_MODEL_ONLY_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().tagSet(S3TAG_LIST2).build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_TAGS);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();
    }

}
