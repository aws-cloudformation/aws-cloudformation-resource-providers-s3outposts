package software.amazon.s3outposts.accesspoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.AccessPoint;
import software.amazon.awssdk.services.s3control.model.ListAccessPointsRequest;
import software.amazon.awssdk.services.s3control.model.ListAccessPointsResponse;
import software.amazon.awssdk.services.s3control.model.VpcConfiguration;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private ListHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    private static final String ACCESSPOINT_NAME1 = "bucket1-ap1";
    private static final String ACCESSPOINT_ARN1 = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/accesspoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ACCESSPOINT_NAME1);
    private static final String ACCESSPOINT_NAME2 = "bucket1-ap2";
    private static final String ACCESSPOINT_ARN2 = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/accesspoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ACCESSPOINT_NAME2);

    private AccessPoint ap1 = AccessPoint.builder()
            .accessPointArn(ACCESSPOINT_ARN1)
            .bucket(BUCKET_NAME)
            .name(ACCESSPOINT_NAME1)
            .vpcConfiguration(VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();
    private ResourceModel model1 = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN1)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME1)
            .vpcConfiguration(software.amazon.s3outposts.accesspoint.VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();

    private AccessPoint ap2 = AccessPoint.builder()
            .accessPointArn(ACCESSPOINT_ARN2)
            .bucket(BUCKET_NAME)
            .name(ACCESSPOINT_NAME2)
            .vpcConfiguration(VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();
    private ResourceModel model2 = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN2)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME2)
            .vpcConfiguration(software.amazon.s3outposts.accesspoint.VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();

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
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * Validation error - Bucket ARN not provided
     */
    @Test
    public void handlerRequest_InvalidRequest() {

        request = ResourceHandlerRequest.<ResourceModel>builder().build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isEqualTo("Bucket ARN is required.");
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getNextToken()).isNull();

    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_ARN_MODEL)
                .build();

        final ListAccessPointsResponse listAccessPointsResponse = ListAccessPointsResponse.builder()
                .accessPointList(ap1, ap2)
                .nextToken("fakeNextToken")
                .build();
        when(proxyClient.client().listAccessPoints(any(ListAccessPointsRequest.class)))
                .thenReturn(listAccessPointsResponse);

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

        verify(sdkClient, atLeastOnce()).serviceName();
    }

    /**
     * Exception thrown
     */
    @Test
    public void handleRequest_Exception() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_ARN_MODEL)
                .build();

        when(proxyClient.client().listAccessPoints(any(ListAccessPointsRequest.class)))
                .thenThrow(constructS3ControlException("AccessDenied"));

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

        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
