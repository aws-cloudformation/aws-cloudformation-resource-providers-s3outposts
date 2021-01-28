package software.amazon.s3outposts.endpoint;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.Endpoint;
import software.amazon.awssdk.services.s3outposts.model.ListEndpointsRequest;
import software.amazon.awssdk.services.s3outposts.model.ListEndpointsResponse;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private ListHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    private Endpoint endpoint1 = Endpoint.builder()
            .endpointArn(ARN1)
            .cidrBlock(CIDR_BLOCK1)
            .creationTime(Instant.parse(CREATION_TIME1))
            .networkInterfaces(NETWORK_INTERFACE_LIST1)
            .outpostsId(OUTPOST_ID)
            .status("Available")
            .build();

    private ResourceModel model1 = ResourceModel.builder()
            .arn(ARN1)
            .cidrBlock(CIDR_BLOCK1)
            .creationTime(CREATION_TIME1)
            .networkInterfaces(MODEL_NETWORK_INTERFACE_LIST1)
            .outpostId(OUTPOST_ID)
            .status("Available")
            .build();

    private Endpoint endpoint2 = Endpoint.builder()
            .endpointArn(ARN2)
            .cidrBlock(CIDR_BLOCK2)
            .creationTime(Instant.parse(CREATION_TIME2))
            .networkInterfaces(NETWORK_INTERFACE_LIST2)
            .outpostsId(OUTPOST_ID)
            .status("Pending")
            .build();

    private ResourceModel model2 = ResourceModel.builder()
            .arn(ARN2)
            .cidrBlock(CIDR_BLOCK2)
            .creationTime(CREATION_TIME2)
            .networkInterfaces(MODEL_NETWORK_INTERFACE_LIST2)
            .outpostId(OUTPOST_ID)
            .status("Pending")
            .build();

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
                        .endpoints(Lists.newArrayList(endpoint1, endpoint2))
                        .nextToken("fakeNextToken")
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isNull();
        assertThat(progress.getResourceModels()).isNotNull();
        assertThat(progress.getResourceModels()).contains(model1, model2);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();
        assertThat(progress.getNextToken()).isEqualTo("fakeNextToken");

        verify(proxyClient.client()).listEndpoints(any(ListEndpointsRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    @Test
    public void handleRequest_SimpleSuccess_NullNextToken() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQ_MODEL_EMPTY)
                .nextToken("fakeNextToken")
                .build();

        final ListEndpointsResponse listEndpointsResponse =
                ListEndpointsResponse.builder()
                        .endpoints(Lists.newArrayList(endpoint1, endpoint2))
                        .nextToken(null)
                        .build();
        when(proxyClient.client().listEndpoints(any(ListEndpointsRequest.class))).thenReturn(listEndpointsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isNull();
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
