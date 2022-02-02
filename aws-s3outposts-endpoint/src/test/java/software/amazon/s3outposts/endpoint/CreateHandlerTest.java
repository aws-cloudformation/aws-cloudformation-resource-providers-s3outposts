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
import software.amazon.awssdk.services.s3outposts.model.ListEndpointsRequest;
import software.amazon.awssdk.services.s3outposts.model.ListEndpointsResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
                = handler.handleRequest(proxy, request, new CallbackContext(false, NUMBER_OF_STABILIZATION_RETRIES), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualTo(FAILURE_CREATE_CALLBACK_CONTEXT);
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
                = handler.handleRequest(proxy, request, new CallbackContext(false, NUMBER_OF_STABILIZATION_RETRIES), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualTo(FAILURE_CREATE_CALLBACK_CONTEXT);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("OutpostId, SecurityGroupId, SubnetId are required parameters.");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    /**
     * Happy Path for minimum required input
     */
    @Test
    public void handleRequest_MinimumInput_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_MIN_INPUT_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .clientRequestToken("12345")
                .build();

        final CreateEndpointResponse createEndpointResponse = CreateEndpointResponse.builder().endpointArn(ARN1).build();
        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class))).thenReturn(createEndpointResponse);

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Arrays.asList(endpoint1, endpoint2))
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getResourceModel()).isEqualTo(RESP_MODEL_MIN_INPUT_CREATE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path for input with accessType CoIP
     */
    @Test
    public void handleRequest_CoipInput_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_COIP_INPUT_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .clientRequestToken("12345")
                .build();

        final CreateEndpointResponse createEndpointResponse = CreateEndpointResponse.builder().endpointArn(ARN1).build();
        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class))).thenReturn(createEndpointResponse);

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Arrays.asList(endpoint1, endpoint2))
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getResourceModel()).isEqualTo(RESP_MODEL_COIP_INPUT_CREATE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Max Retries reached waiting for Resource to stabilize for input with accessType CoIP
     */
    @Test
    public void handleRequest_CoipInput_MaxRetryAttempts_Success() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_COIP_INPUT_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .clientRequestToken("12345")
                .build();

        final CreateEndpointResponse createEndpointResponse = CreateEndpointResponse.builder().endpointArn(ARN1).build();
        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class))).thenReturn(createEndpointResponse);

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Arrays.asList(endpoint2, endpoint3))
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(false, NUMBER_OF_STABILIZATION_RETRIES), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getResourceModel()).isEqualTo(RESP_MODEL_COIP_INPUT_CREATE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(proxyClient.client(), times(NUMBER_OF_STABILIZATION_RETRIES)).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * CFNStabilizationException for invalid status returned for read during stabilizing.
     */
    @Test
    public void handleRequest_CoipInput_InvalidStatus_StabilizedException() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_COIP_INPUT_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .clientRequestToken("12345")
                .build();

        final CreateEndpointResponse createEndpointResponse = CreateEndpointResponse.builder().endpointArn(ARN2).build();
        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class))).thenReturn(createEndpointResponse);

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Arrays.asList(endpoint1, endpoint4))
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        Exception exception = assertThrows(
                CfnNotStabilizedException.class,
                () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
        assertThat(exception.getMessage()).contains("did not stabilize.");
        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - All Caps AccessType should throw 400
     */
    @Test
    public void handleRequest_InvalidAccessType_Error_400() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_INVALID_ACCESS_TYPE_CREATE1)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("AccessType is invalid.");
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Error Path - Invalid AccessType should throw 400
     */
    @Test
    public void handleRequest_InvalidRandomAccessType_Error_400() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_INVALID_ACCESS_TYPE_CREATE2)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("AccessType is invalid.");
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Happy Path - Re-entry on stabilize function
     */
    @Test
    public void handleRequest_Success_Stabilized() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(RESP_MODEL_MIN_INPUT_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .clientRequestToken("12345")
                .build();

        final CreateEndpointResponse createEndpointResponse = CreateEndpointResponse.builder().endpointArn(ARN1).build();
        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class))).thenReturn(createEndpointResponse);

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Arrays.asList(endpoint1, endpoint2))
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getResourceModel()).isEqualTo(RESP_MODEL_MIN_INPUT_CREATE);
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
                .desiredResourceState(REQ_MODEL_MIN_INPUT_CREATE)
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
                .desiredResourceState(REQ_MODEL_MIN_INPUT_CREATE)
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

    /**
     * Error Path - InvalidRequest
     */
    @Test
    public void handleRequest_Error_400() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_INVALID_COIP_CREATE)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().createEndpoint(any(CreateEndpointRequest.class)))
                .thenThrow(constructS3OutpostsExceptionWithStatusCode(400));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).createEndpoint(any(CreateEndpointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
