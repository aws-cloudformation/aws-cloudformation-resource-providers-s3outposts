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
    private static final ResourceModel CREATE_BUCKET_MODEL = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .build();

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
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

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
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_SuccessComplete() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        final CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().bucketArn(ARN).build();
        when(proxyClient.client().createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final GetBucketResponse getBucketResponse = GetBucketResponse.builder().bucket(BUCKET_NAME).build();
        when(proxyClient.client().getBucket(any(GetBucketRequest.class))).thenReturn(getBucketResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progressEvent).isNotNull();
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progressEvent.getCallbackContext()).isNull();
        assertThat(progressEvent.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progressEvent.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progressEvent.getResourceModels()).isNull();
        assertThat(progressEvent.getMessage()).isNull();
        assertThat(progressEvent.getErrorCode()).isNull();

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }


    @Test
    public void handleRequest_Success_NoBucketName() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_NOBUCKETNAME_MODEL).build();

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


    @Test
    public void handleRequest_Success_NoOutpostId() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_NOOUTPOSTID_MODEL).build();

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

    @Test
    public void handleRequest_AlreadyExists() {

        request = ResourceHandlerRequest.<ResourceModel>builder().desiredResourceState(CREATE_BUCKET_MODEL).build();

        when(proxyClient.client().createBucket(any(CreateBucketRequest.class)))
                .thenThrow(BucketAlreadyExistsException.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ResourceConflict);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

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
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

        verify(proxyClient.client()).createBucket(any(CreateBucketRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
