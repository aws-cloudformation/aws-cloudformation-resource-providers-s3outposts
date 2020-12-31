package software.amazon.s3outposts.bucketpolicy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3control.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3control.model.NotFoundException;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    private ReadHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

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
     * Validation error: No Bucket ARN provided in the model.
     */
    @Test
    public void handleRequest_NoBucket() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ONLY_POLICY_MODEL)
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
     * Happy Path.
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ONLY_BUCKET_MODEL)
                .build();

        final GetBucketPolicyResponse getBucketPolicyResponse = GetBucketPolicyResponse.builder()
                .policy(BUCKET_POLICY)
                .build();
        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class))).thenReturn(getBucketPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(GET_BUCKET_POLICY_RESPONSE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * API error: GetBucketPolicy call returns a failure.
     */
    @Test
    public void handleRequest_Error() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ONLY_BUCKET_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(NotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
