package software.amazon.s3outposts.bucket;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.ListRegionalBucketsRequest;
import software.amazon.awssdk.services.s3control.model.ListRegionalBucketsResponse;
import software.amazon.awssdk.services.s3control.model.RegionalBucket;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private ListHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    // Constants
    private static final String OUTPOST_ID = "op-12345678901234";

    private static final ResourceModel REQUEST_MODEL = ResourceModel.builder()
            .outpostId(OUTPOST_ID)
            .build();

    private RegionalBucket regionalBucket1 = RegionalBucket.builder()
            .bucket("bucket1")
            .bucketArn("arn:aws:s3-outposts:us-east-1:12345789012:outpost/op-12345678901234/bucket/bucket1")
            .creationDate(Instant.now())
            .outpostId("op-12345678901234")
            .publicAccessBlockEnabled(true)
            .build();
    private ResourceModel model1 = ResourceModel.builder()
            .outpostId("op-12345678901234")
            .arn("arn:aws:s3-outposts:us-east-1:12345789012:outpost/op-12345678901234/bucket/bucket1")
            .bucketName("bucket1")
            .build();

    private RegionalBucket regionalBucket2 = RegionalBucket.builder()
            .bucket("bucket2")
            .bucketArn("arn:aws:s3-outposts:us-east-1:12345789012:outpost/op-12345678901234/bucket/bucket2")
            .creationDate(Instant.now())
            .outpostId("op-12345678901234")
            .publicAccessBlockEnabled(true)
            .build();
    private ResourceModel model2 = ResourceModel.builder()
            .arn("arn:aws:s3-outposts:us-east-1:12345789012:outpost/op-12345678901234/bucket/bucket2")
            .bucketName("bucket2")
            .outpostId("op-12345678901234")
            .build();

    // Mock variables
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);

    }

    @AfterEach
    public void tear_down() {
//        verify(sdkClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(sdkClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQUEST_MODEL)
                .build();

        final ListRegionalBucketsResponse listRegionalBucketsResponse =
                ListRegionalBucketsResponse.builder()
                        .regionalBucketList(Lists.newArrayList(regionalBucket1, regionalBucket2))
                        .build();
        when(proxyClient.client().listRegionalBuckets(any(ListRegionalBucketsRequest.class))).thenReturn(listRegionalBucketsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getResourceModels()).contains(model1, model2);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getNextToken()).isNull();

        verify(proxyClient.client()).listRegionalBuckets(any(ListRegionalBucketsRequest.class));
    }

    @Test
    public void handleRequest_NoOutpostId() {
        ResourceModel model = ResourceModel.builder().build();
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

//        final ListRegionalBucketsResponse listRegionalBucketsResponse =
//                ListRegionalBucketsResponse.builder()
//                        .regionalBucketList(Lists.newArrayList(regionalBucket1, regionalBucket2))
//                        .build();
//        when(proxyClient.client().listRegionalBuckets(any(ListRegionalBucketsRequest.class))).thenReturn(listRegionalBucketsResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isEqualTo("OutpostId is required.");
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(response.getNextToken()).isNull();

    }

    @Test
    public void handleRequest_Exception() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(REQUEST_MODEL)
                .build();

        when(proxyClient.client().listRegionalBuckets(any(ListRegionalBucketsRequest.class))).thenThrow(S3ControlException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
        assertThat(response.getNextToken()).isNull();
    }


}
