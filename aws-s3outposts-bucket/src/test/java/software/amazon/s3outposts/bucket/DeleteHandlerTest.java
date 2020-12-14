package software.amazon.s3outposts.bucket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.BadRequestException;
import software.amazon.awssdk.services.s3control.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3control.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

public class DeleteHandlerTest extends AbstractTestBase {

    private DeleteHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    // Constants
    private static final ResourceModel DELETE_SUCCESS_MODEL = ResourceModel.builder()
            .arn(ARN)
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
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
//        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    // Tests
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(DELETE_SUCCESS_MODEL).build();

        final DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(proxyClient.client().deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteBucket(any(DeleteBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_NoArn() {

        ResourceModel model = ResourceModel.builder().build();
        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("Bucket ARN is required.");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

    }

    @Test
    public void handleRequest_BadRequest() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(DELETE_SUCCESS_MODEL).build();

        when(proxyClient.client().deleteBucket(any(DeleteBucketRequest.class)))
                .thenThrow(BadRequestException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).deleteBucket(any(DeleteBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_400() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(DELETE_SUCCESS_MODEL).build();

        when(proxyClient.client().deleteBucket(any(DeleteBucketRequest.class)))
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

        verify(proxyClient.client()).deleteBucket(any(DeleteBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_BucketNotEmpty() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(DELETE_SUCCESS_MODEL).build();

        when(proxyClient.client().deleteBucket(any(DeleteBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(409)
                        .message("BucketNotEmpty").build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("BucketNotEmpty");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ResourceConflict);

        verify(proxyClient.client()).deleteBucket(any(DeleteBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_InvalidBucketState() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(DELETE_SUCCESS_MODEL).build();

        when(proxyClient.client().deleteBucket(any(DeleteBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(409)
                        .message("InvalidBucketState").build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("InvalidBucketState");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ResourceConflict);

        verify(proxyClient.client()).deleteBucket(any(DeleteBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_NoSuchBucket() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(DELETE_SUCCESS_MODEL).build();

        when(proxyClient.client().deleteBucket(any(DeleteBucketRequest.class)))
                .thenThrow(S3ControlException.builder().statusCode(404)
                        .message("NoSuchBucket").build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("NoSuchBucket");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).deleteBucket(any(DeleteBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
