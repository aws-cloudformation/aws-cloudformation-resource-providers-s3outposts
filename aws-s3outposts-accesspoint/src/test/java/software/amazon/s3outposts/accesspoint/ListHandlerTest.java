package software.amazon.s3outposts.accesspoint;

import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.awssdk.services.s3control.model.VpcConfiguration;
import software.amazon.cloudformation.proxy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private ListHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    private static final String ACCESSPOINT_NAME1 = "bucket1-ap1";
    private static final String ACCESSPOINT_NAME2 = "bucket1-ap2";

    private AccessPoint ap1 = AccessPoint.builder()
            .accessPointArn(String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/accesspoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ACCESSPOINT_NAME1))
            .bucket(BUCKET_NAME)
            .name(ACCESSPOINT_NAME1)
            .vpcConfiguration(VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();
    private ResourceModel model1 = ResourceModel.builder()
            .arn(String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/accesspoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ACCESSPOINT_NAME1))
            .bucket(String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/bucket/%s", REGION, ACCOUNT_ID, OUTPOST_ID, BUCKET_NAME))
            .name(ACCESSPOINT_NAME1)
            .vpcConfiguration(software.amazon.s3outposts.accesspoint.VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();

    private AccessPoint ap2 = AccessPoint.builder()
            .accessPointArn(String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/accesspoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ACCESSPOINT_NAME2))
            .bucket(BUCKET_NAME)
            .name(ACCESSPOINT_NAME2)
            .vpcConfiguration(VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();
    private ResourceModel model2 = ResourceModel.builder()
            .arn(String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/accesspoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ACCESSPOINT_NAME2))
            .bucket(String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/bucket/%s", REGION, ACCOUNT_ID, OUTPOST_ID, BUCKET_NAME))
            .name(ACCESSPOINT_NAME2)
            .vpcConfiguration(software.amazon.s3outposts.accesspoint.VpcConfiguration.builder().vpcId("vpc-12345").build())
            .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        handler = new ListHandler();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_ARN_MODEL)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(ListAccessPointsRequest.class), any())).thenReturn(
                ListAccessPointsResponse.builder().accessPointList(ap1,ap2).build()
        );

        final ProgressEvent<ResourceModel, CallbackContext> progress =
            handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isNull();
        assertThat(progress.getResourceModels()).isNotNull();
        assertThat(progress.getResourceModels()).contains(model1, model2);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();
    }

    @Test
    public void handlerRequest_InvalidRequest() {

        handler = new ListHandler();

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

    @Test
    public void handleRequest_Exception() {

        handler = new ListHandler();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_ARN_MODEL)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(ListAccessPointsRequest.class), any()))
                .thenThrow(S3ControlException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
        assertThat(progress.getNextToken()).isNull();


    }
}
