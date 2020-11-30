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
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    private UpdateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    private static final ResourceModel UPDATE_SUCCESS_MODEL = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .build();

    private static final ResourceModel BUCKET_ERROR_MODEL = ResourceModel.builder()
            .arn(ARN)
            .bucketName("bucket-1")
            .outpostId(OUTPOST_ID)
            .build();

    private static final ResourceModel OUTPOST_ERROR_MODEL = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId("op-98765432109876")
            .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    @Mock
    ReadHandler readHandler;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
        readHandler = mock(ReadHandler.class);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
//        verify(sdkClient, atLeastOnce()).serviceName();
//        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .previousResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        // BucketName and OutpostId match
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

    }

    @Test
    public void handleRequest_BucketError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_ERROR_MODEL)
                .previousResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
//        assertThat(response.getMessage()).isEqualTo("Resource of type 'AWS::S3Outposts::Bucket' with identifier 'bucket-1' is not updatable with parameters provided.");
//        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_OutpostError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(OUTPOST_ERROR_MODEL)
                .previousResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
//        assertThat(response.getMessage()).isEqualTo("Resource of type 'AWS::S3Outposts::Bucket' with identifier 'op-98765432109876' is not updatable with parameters provided.");
//        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotUpdatable);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_GetError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .previousResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        when(proxyClient.client().getBucket(any(GetBucketRequest.class)))
                .thenThrow(NotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
