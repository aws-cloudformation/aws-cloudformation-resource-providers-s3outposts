package software.amazon.s3outposts.endpoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.ListEndpointsRequest;
import software.amazon.awssdk.services.s3outposts.model.ListEndpointsResponse;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    private ReadHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3OutpostsClient> proxyClient;

    @Mock
    S3OutpostsClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3OutpostsClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * Validation error: Empty model
     */
    @Test
    public void handleRequest_EmptyModel() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("Endpoint ARN is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Validation Error - No ARN in model
     */
    @Test
    public void handleRequest_NoEndpointArn() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_NO_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();
        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("Endpoint ARN is required.");
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
                .desiredResourceState(REQ_MODEL_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

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
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(model1);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();
        assertThat(progress.getNextToken()).isNull();

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * ErrorPath - ListResponse doesn't contain any Endpoint objects
     */
    @Test
    public void handleRequest_Error_ListResponseEmpty() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Collections.emptyList())
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(REQ_MODEL_ARN);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isEqualTo("Endpoint with provided ARN not found.");
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(progress.getNextToken()).isNull();

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * ErrorPath - The API call `listEndpoints` returns an error
     */
    @Test
    public void handleRequest_Error_ListResponseError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_ARN)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class)))
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
        assertThat(progress.getNextToken()).isNull();

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * ErrorPath - ListResponse contains Endpoint objects, but none of those match with the Arn supplied by the caller.
     */
    @Test
    public void handleRequest_Error_ArnMissingInListResponse() {

        final String ID3 = "34abcd3efghij4kl5m6";
        final String ARN3 = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/endpoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ID3);
        final ResourceModel REQ_MODEL_ARN3 = ResourceModel.builder().arn(ARN3).build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_ARN3)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Arrays.asList(endpoint1, endpoint2))
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(REQ_MODEL_ARN3);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isEqualTo("Endpoint with provided ARN not found.");
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(progress.getNextToken()).isNull();

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
