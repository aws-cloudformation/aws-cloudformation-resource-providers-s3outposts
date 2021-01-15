package software.amazon.s3outposts.accesspoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.VpcConfiguration;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private CreateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    // Mocks
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    // Pre-, Post- test steps
    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * Validation error - No Bucket ARN provided
     */
    @Test
    public void handleRequest_NoBucket() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_NO_BUCKET_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("Bucket ARN is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client(), never()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(sdkClient, times(0)).serviceName();
    }

    /**
     * Validation error - No AccessPoint name provided
     */
    @Test
    public void handleRequest_NoAccessPointName() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_NO_ACCESSPOINT_NAME_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("AccessPoint Name is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client(), never()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(sdkClient, times(0)).serviceName();

    }

    /**
     * ValidationError - No VpcConfiguration provided
     */
    @Test
    public void handleRequest_NoVpcConfiguration() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_NO_VPCCONFIGURATION_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("VpcConfiguration is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client(), never()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(sdkClient, times(0)).serviceName();

    }

    /**
     * Happy Path - Created AccessPoint, waiting for propagation delay.
     */
    @Test
    public void handleRequest_Success_Pending() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_CREATE_MODEL)
                .build();

        final CreateAccessPointResponse createAccessPointResponse = CreateAccessPointResponse.builder()
                .accessPointArn(ACCESSPOINT_ARN)
                .build();
        when(proxyClient.client().createAccessPoint(any(CreateAccessPointRequest.class))).thenReturn(createAccessPointResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(progress.getCallbackContext().stabilized).isEqualTo(true);
        assertThat(progress.getCallbackContext().propagated).isEqualTo(false);
        assertThat(progress.getCallbackContext().forcedDelayCount).isEqualTo(1);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(20);
        assertThat(progress.getResourceModel()).isEqualTo(AP_COMPLETE_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Created AccessPoint and AccessPointPolicy, called ReadHandler.
     */
    @Test
    public void handleRequest_Success_Complete() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_CREATE_MODEL)
                .build();

        final CreateAccessPointResponse createAccessPointResponse = CreateAccessPointResponse.builder()
                .accessPointArn(ACCESSPOINT_ARN)
                .build();
        when(proxyClient.client().createAccessPoint(any(CreateAccessPointRequest.class))).thenReturn(createAccessPointResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final PutAccessPointPolicyResponse putAPPolicyResponse = PutAccessPointPolicyResponse.builder().build();
        when(proxyClient.client().putAccessPointPolicy(any(PutAccessPointPolicyRequest.class))).thenReturn(putAPPolicyResponse);

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder()
                .bucket(BUCKET_NAME)
                .name(ACCESSPOINT_NAME)
                .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class))).thenReturn(getAccessPointResponse);

        final GetAccessPointPolicyResponse getAPPolicyResponse = GetAccessPointPolicyResponse.builder()
                .policy(ACCESSPOINT_POLICY)
                .build();
        when(proxyClient.client().getAccessPointPolicy(any(GetAccessPointPolicyRequest.class))).thenReturn(getAPPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_COMPLETE_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(proxyClient.client()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - No AccessPointPolicy supplied while creating AccessPoint
     */
    @Test
    public void handleRequest_Success_Complete_NoPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_NO_POLICY_MODEL)
                .build();

        final CreateAccessPointResponse createAccessPointResponse = CreateAccessPointResponse.builder()
                .accessPointArn(ACCESSPOINT_ARN)
                .build();
        when(proxyClient.client().createAccessPoint(any(CreateAccessPointRequest.class))).thenReturn(createAccessPointResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder()
                .bucket(BUCKET_NAME)
                .name(ACCESSPOINT_NAME)
                .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class))).thenReturn(getAccessPointResponse);

        when(proxyClient.client().getAccessPointPolicy(any(GetAccessPointPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchAccessPointPolicy"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_NO_POLICY_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * AccessPoint creation fails - BadRequest
     */
    @Test
    public void handleRequest_CreateAP_BadRequest() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_CREATE_MODEL)
                .build();

        when(proxyClient.client().createAccessPoint(any(CreateAccessPointRequest.class)))
                .thenThrow(BadRequestException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * AccessPoint creation fails - AlreadyExists
     */
    @Test
    public void handleRequest_CreateAP_AlreadyExists() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_CREATE_MODEL)
                .build();

        when(proxyClient.client().createAccessPoint(any(CreateAccessPointRequest.class)))
                .thenThrow(constructS3ControlException("AccessPointAlreadyOwnedByYou"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);

        verify(proxyClient.client()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * AccessPointPolicy creation fails - Invalid Request
     */
    @Test
    public void handleRequest_PutAPPolicy_InvalidRequest() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_CREATE_MODEL)
                .build();

        final CreateAccessPointResponse createAccessPointResponse = CreateAccessPointResponse.builder()
                .accessPointArn(ACCESSPOINT_ARN)
                .build();
        when(proxyClient.client().createAccessPoint(any(CreateAccessPointRequest.class))).thenReturn(createAccessPointResponse);

        when(proxyClient.client().putAccessPointPolicy(any(PutAccessPointPolicyRequest.class)))
                .thenThrow(InvalidRequestException.class);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(context);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_CREATE_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);

        verify(proxyClient.client()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * AccessPointPolicy creation fails - Empty Policy - AccessDenied
     */
    @Test
    public void handleRequest_PutAPPolicy_EmptyPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_EMPTY_POLICY_MODEL)
                .build();

        final CreateAccessPointResponse createAccessPointResponse = CreateAccessPointResponse.builder()
                .accessPointArn(ACCESSPOINT_ARN)
                .build();
        when(proxyClient.client().createAccessPoint(any(CreateAccessPointRequest.class))).thenReturn(createAccessPointResponse);

        CallbackContext context = new CallbackContext();
        context.setStabilized(true);
        context.setPropagated(true);
        context.setForcedDelayCount(2);

        when(proxyClient.client().putAccessPointPolicy(any(PutAccessPointPolicyRequest.class)))
                .thenThrow(constructS3ControlException("AccessDenied"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, context, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(context);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_EMPTY_POLICY_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);

        verify(proxyClient.client()).createAccessPoint(any(CreateAccessPointRequest.class));
        verify(proxyClient.client()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client(), never()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client(), never()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
