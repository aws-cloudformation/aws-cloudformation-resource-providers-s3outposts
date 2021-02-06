package software.amazon.s3outposts.endpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.DeleteEndpointRequest;
import software.amazon.awssdk.services.s3outposts.model.DeleteEndpointResponse;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    private DeleteHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3OutpostsClient> proxyClient;

    @Mock
    S3OutpostsClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3OutpostsClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(proxyClient.client());
    }

    /**
     * Validation Error - Empty Model
     */
    @Test
    public void handleRequest_EmptyModel() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_EMPTY)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingFieldByField(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("Endpoint ARN is required.");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final DeleteEndpointResponse deleteEndpointResponse = DeleteEndpointResponse.builder().build();
        when(proxyClient.client().deleteEndpoint(any(DeleteEndpointRequest.class))).thenReturn(deleteEndpointResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isEqualTo(null);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isNull();
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteEndpoint(any(DeleteEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - ResourceConflict
     */
    @Test
    public void handleRequest_Error_409() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().deleteEndpoint(any(DeleteEndpointRequest.class)))
                .thenThrow(constructS3OutpostsExceptionWithStatusCode(409));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ResourceConflict);

        verify(proxyClient.client()).deleteEndpoint(any(DeleteEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
