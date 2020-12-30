package software.amazon.s3outposts.bucket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.GetBucketRequest;
import software.amazon.awssdk.services.s3control.model.GetBucketResponse;
import software.amazon.awssdk.services.s3control.model.NotFoundException;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    private ReadHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    // Constants
    private static final ResourceModel REQUEST_SUCCESS_MODEL = ResourceModel.builder()
            .arn(ARN)
            .build();

    private static final ResourceModel RESPONSE_SUCCESS_MODEL = ResourceModel.builder()
            .arn(ARN)
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
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    // Tests
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQUEST_SUCCESS_MODEL)
                .build();

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(RESPONSE_SUCCESS_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));

    }

    @Test
    public void handleRequest_NotFound() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQUEST_SUCCESS_MODEL)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
                .thenThrow(NotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));

    }

    @Test
    public void handleRequest_400() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQUEST_SUCCESS_MODEL)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(400).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));

    }

    @Test
    public void handleRequest_404() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQUEST_SUCCESS_MODEL)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(404).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).getBucket(any(GetBucketRequest.class));

    }
}
