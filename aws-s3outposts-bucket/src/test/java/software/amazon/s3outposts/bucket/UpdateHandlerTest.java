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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    private UpdateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * Validation Error - bucket arn not provided
     */
    @Test
    public void handleRequest_NoBucketArn() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_REQ_BUCKET_MODEL_WITH_TAGS_NO_ARN)
                .previousResourceState(BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("Bucket ARN is required.");
        assertThat(progress.getResourceModel()).isEqualTo(UPDATE_REQ_BUCKET_MODEL_WITH_TAGS_NO_ARN);
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);

    }

    /**
     * Happy Path - Update from NoTags model to NoTags model
     */
    @Test
    public void handleRequest_SimpleSuccess_NoTagsToNoTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .previousResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_NO_TAGS_AND_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - from no tags to tags
     */
    @Test
    public void handleRequest_SimpleSuccess_NoTagsToTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_WITH_TAGS)
                .desiredResourceTags(TAG_MAP)
                .previousResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final PutBucketTaggingResponse putBucketTaggingResponse = PutBucketTaggingResponse.builder().build();
        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class))).thenReturn(putBucketTaggingResponse);

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

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - from tags to no tags
     */
    @Test
    public void handleRequest_SimpleSuccess_TagsToNoTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .previousResourceState(BUCKET_MODEL_WITH_TAGS)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final DeleteBucketTaggingResponse deleteBucketTaggingResponse = DeleteBucketTaggingResponse.builder().build();
        when(proxyClient.client().deleteBucketTagging(any(DeleteBucketTaggingRequest.class))).thenReturn(deleteBucketTaggingResponse);

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

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).deleteBucketTagging(any(DeleteBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - same tags provided
     */
    @Test
    public void handleRequest_SimpleSuccess_TagsToTags() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_WITH_TAGS)
                .previousResourceState(BUCKET_MODEL_WITH_TAGS)
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

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - From no tags to resource tags + system tags
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

        Map<String, String> sysTagMap = new HashMap<String, String>() {
            {
                put(sysTag1.getKey(), sysTag1.getValue());
                put(sysTag2.getKey(), sysTag2.getValue());
            }
        };

        final ResourceModel BUCKET_MODEL_WITH_SYSTEM_TAGS = ResourceModel.builder()
                .arn(ARN)
                .bucketName(BUCKET_NAME)
                .outpostId(OUTPOST_ID)
                .tags(allTagSet)
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_WITH_TAGS)
                .previousResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .desiredResourceTags(TAG_MAP)
                .systemTags(sysTagMap)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final PutBucketTaggingResponse putBucketTaggingResponse = PutBucketTaggingResponse.builder().build();
        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class))).thenReturn(putBucketTaggingResponse);

        final GetBucketTaggingResponse getBucketTaggingResponse = GetBucketTaggingResponse.builder().tagSet(allS3TagList).build();
        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class))).thenReturn(getBucketTaggingResponse);

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_SYSTEM_TAGS);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - No changes, GetBucket returns S3ControlException with a 404 status code
     */
    @Test
    public void handleRequest_Error_GetBucket() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .previousResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isEqualTo("Bucket does not exist.");
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - No changes, GetBucket returns NotFoundException
     */
    @Test
    public void handleRequest_Exception_GetBucket() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .previousResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
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
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - PutBucketTagging returns TooManyTags exception
     */
    @Test
    public void handleRequest_Error_PutBucketTagging() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_WITH_TAGS)
                .previousResourceState(BUCKET_MODEL_NO_TAGS_AND_RULES)
                .desiredResourceTags(TAG_MAP)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().putBucketTagging(any(PutBucketTaggingRequest.class)))
                .thenThrow(TooManyTagsException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_WITH_TAGS);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle Configuration - No rules to no rules
     */
    @Test
    public void handleRequest_Lifecycle_NoRulesToNoRules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_NO_RULES)
                .previousResourceState(BUCKET_MODEL_NO_RULES)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenThrow(constructS3ControlExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_NO_TAGS_AND_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle Configuration - No rules to rules
     */
    @Test
    public void handleRequest_Lifecycle_NoRulesToRules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_RULES)
                .previousResourceState(BUCKET_MODEL_NO_RULES)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final PutBucketLifecycleConfigurationResponse putBucketLifecycleConfigurationResponse =
                PutBucketLifecycleConfigurationResponse.builder().build();
        when(proxyClient.client().putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class)))
                .thenReturn(putBucketLifecycleConfigurationResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().rules(LIFECYCLE_RULE_LIST).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_RULES);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle Configuration - Rules to no rules
     */
    @Test
    public void handleRequest_Lifecycle_RulesToNoRules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_EMPTY_RULES)
                .previousResourceState(BUCKET_MODEL_RULES)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final DeleteBucketLifecycleConfigurationResponse deleteBucketLifecycleConfigurationResponse =
                DeleteBucketLifecycleConfigurationResponse.builder().build();
        when(proxyClient.client().deleteBucketLifecycleConfiguration(any(DeleteBucketLifecycleConfigurationRequest.class)))
                .thenReturn(deleteBucketLifecycleConfigurationResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

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

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).deleteBucketLifecycleConfiguration(any(DeleteBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle Configuration - No change in rules
     */
    @Test
    public void handleRequest_Lifecycle_RulesToRules() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_RULES)
                .previousResourceState(BUCKET_MODEL_RULES)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().rules(LIFECYCLE_RULE_LIST).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

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

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle Configuration - Rules1 to Rules2
     */
    @Test
    public void handleRequest_Lifecycle_Rules1ToRules2() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_RULES2)
                .previousResourceState(BUCKET_MODEL_RULES1)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenThrow(S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchTagSet").build()).build());

        final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse =
                GetBucketLifecycleConfigurationResponse.builder().rules(LIFECYCLE_RULE_LIST2).build();
        when(proxyClient.client().getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(getBucketLifecycleConfigurationResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_RULES2);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), atLeast(2)).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Lifecycle Configuration - Rules to Empty Rules
     */
    @Test
    public void handleRequest_Lifecycle_Error_DeleteBucketLifecycleConfiguration() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_MODEL_EMPTY_RULES)
                .previousResourceState(BUCKET_MODEL_RULES)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        when(proxyClient.client().deleteBucketLifecycleConfiguration(any(DeleteBucketLifecycleConfigurationRequest.class)))
                .thenThrow(InternalServiceException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(BUCKET_MODEL_EMPTY_RULES);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));
        verify(proxyClient.client(), never()).putBucketTagging(any(PutBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).putBucketLifecycleConfiguration(any(PutBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client()).deleteBucketLifecycleConfiguration(any(DeleteBucketLifecycleConfigurationRequest.class));
        verify(proxyClient.client(), never()).getBucketTagging(any(GetBucketTaggingRequest.class));
        verify(proxyClient.client(), never()).getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
