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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private ListHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3OutpostsClient> proxyClient;

    @Mock
    S3OutpostsClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3OutpostsClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(proxyClient.client());
    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_EMPTY)
                .build();

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Arrays.asList(endpoint1, endpoint2))
                        .nextToken("fakeNextToken")
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(REQ_MODEL_EMPTY);
        assertThat(progress.getResourceModels()).isNotNull();
        assertThat(progress.getResourceModels()).contains(model1, model2);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();
        assertThat(progress.getNextToken()).isEqualTo("fakeNextToken");

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - ListEndpointsResponse returns nextToken as "null"
     */
    @Test
    public void handleRequest_SimpleSuccess_NullNextToken() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_EMPTY)
                .nextToken("fakeNextToken")
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
        assertThat(progress.getResourceModel()).isEqualTo(REQ_MODEL_EMPTY);
        assertThat(progress.getResourceModels()).isNotNull();
        assertThat(progress.getResourceModels()).contains(model1, model2);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();
        assertThat(progress.getNextToken()).isNull();

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - NotFound
     */
    @Test
    public void handleRequest_Error_404() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_EMPTY)
                .build();

        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class)))
                .thenThrow(constructS3OutpostsExceptionWithStatusCode(404));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(progress.getNextToken()).isNull();

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
