package software.amazon.s3outposts.accesspoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.DeleteAccessPointRequest;
import software.amazon.awssdk.services.s3control.model.DeleteAccessPointResponse;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    private DeleteHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * ValidationError - No AccessPoint ARN provided
     */
    @Test
    public void handleRequest_NoAccessPointArn() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("AccessPoint ARN is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final DeleteAccessPointResponse deleteAccessPointResponse = DeleteAccessPointResponse.builder().build();
        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenReturn(deleteAccessPointResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isNull();
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();
    }

    /**
     * Error Path
     */
    @Test
    public void handleRequest_Error() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenThrow(constructS3ControlException("InvalidAccessPoint"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
