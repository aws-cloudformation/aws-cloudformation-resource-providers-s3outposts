package software.amazon.s3outposts.bucket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private CreateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    // Constants
    private static final String REGION = "us-east-1";
    private static final String ACCOUNT_ID = "12345789012";
    private static final String OUTPOST_ID = "op-12345678901234";
    private static final String BUCKET_NAME = "bucket1";

    private static final String ARN =
            String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/bucket/%s", REGION, ACCOUNT_ID, OUTPOST_ID, BUCKET_NAME);
    private static final ResourceModel CREATE_BUCKET_MODEL = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
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
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(proxyClient.client());
    }

    // Tests
    @Test
    public void handleRequest_SimpleSuccess() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_AlreadyExists() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(BucketAlreadyExistsException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_AlreadyOwnedByYou() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(BucketAlreadyOwnedByYouException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_400() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(400).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_403() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(403).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_404() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(404).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_409() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(409).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ResourceConflict);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_500() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(500).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_503() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(503).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void handleRequest_OtherStatusCodes() {
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(408).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
    }
}