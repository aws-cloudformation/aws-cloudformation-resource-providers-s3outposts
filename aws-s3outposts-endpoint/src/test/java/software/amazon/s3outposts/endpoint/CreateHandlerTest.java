package software.amazon.s3outposts.endpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.CreateEndpointRequest;
import software.amazon.awssdk.services.s3outposts.model.CreateEndpointResponse;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private CreateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3OutpostsClient> proxyClient;

    @Mock
    S3OutpostsClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3OutpostsClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(proxyClient.client());
    }

    /**
     * Validation error - Null model
     */
    @Test
    public void handleRequest_NullModel() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("OutpostId, SecurityGroupId, SubnetId are required parameters.");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

    }

    /**
     * Validation error - Empty model
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
        assertThat(response.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("OutpostId, SecurityGroupId, SubnetId are required parameters.");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateEndpointResponse createEndpointResponse = CreateEndpointResponse.builder().endpointArn(ARN1).build();
        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class))).thenReturn(createEndpointResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getResourceModel()).isEqualTo(RESP_MODEL_CREATE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Re-entry on stabilize function
     */
    @Test
    public void handleRequest_Success_Stabilized() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(RESP_MODEL_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final CreateEndpointResponse createEndpointResponse = CreateEndpointResponse.builder().endpointArn(ARN1).build();
        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class))).thenReturn(createEndpointResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getResourceModel()).isEqualTo(RESP_MODEL_CREATE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - AccessDenied
     */
    @Test
    public void handleRequest_Error_403() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class)))
                .thenThrow(constructS3OutpostsExceptionWithStatusCode(403));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - Throttling
     */
    @Test
    public void handleRequest_Error_503() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class)))
                .thenThrow(constructS3OutpostsExceptionWithStatusCode(503));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
