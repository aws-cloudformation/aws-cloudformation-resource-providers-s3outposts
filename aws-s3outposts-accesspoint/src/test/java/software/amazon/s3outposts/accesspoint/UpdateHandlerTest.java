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
public class UpdateHandlerTest extends AbstractTestBase {

    private UpdateHandler handler;
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
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * Validation error - Empty Model (i.e. model is null)
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
        assertThat(progress.getMessage()).isEqualTo("AccessPoint ARN is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Validation error - AccessPoint ARN is not provided
     */
    @Test
    public void handleRequest_NoAccessPointArn() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_READ_NO_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("AccessPoint ARN is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Happy Path - Null Policy
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .previousResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

//        final DeleteAccessPointPolicyResponse deleteAPPolicyResponse = DeleteAccessPointPolicyResponse.builder().build();
//        when(proxyClient.client().deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class))).thenReturn(deleteAPPolicyResponse);

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder()
                .bucket(BUCKET_NAME)
                .name(ACCESSPOINT_NAME)
                .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class))).thenReturn(getAccessPointResponse);

        when(proxyClient.client().getAccessPointPolicy(any(GetAccessPointPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchAccessPointPolicy"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_NO_POLICY_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client(), never()).deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Valid Policy - but no change
     */
    @Test
    public void handleRequest_Success_SamePolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_COMPLETE_MODEL)
                .previousResourceState(AP_COMPLETE_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

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
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_COMPLETE_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client(), never()).deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Valid Policy - with change
     */
    @Test
    public void handleRequest_Success_DifferentPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_COMPLETE_MODEL2)
                .previousResourceState(AP_COMPLETE_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final PutAccessPointPolicyResponse putAPPolicyResponse = PutAccessPointPolicyResponse.builder().build();
        when(proxyClient.client().putAccessPointPolicy(any(PutAccessPointPolicyRequest.class))).thenReturn(putAPPolicyResponse);

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder()
                .bucket(BUCKET_NAME)
                .name(ACCESSPOINT_NAME)
                .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class))).thenReturn(getAccessPointResponse);

        final GetAccessPointPolicyResponse getAPPolicyResponse = GetAccessPointPolicyResponse.builder()
                .policy(ACCESSPOINT_POLICY2)
                .build();
        when(proxyClient.client().getAccessPointPolicy(any(GetAccessPointPolicyRequest.class))).thenReturn(getAPPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_COMPLETE_MODEL2);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client(), never()).deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Set policy to null
     */
    @Test
    public void handleRequest_Success_PolicyToNullPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_NO_POLICY_MODEL)
                .previousResourceState(AP_COMPLETE_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final DeleteAccessPointPolicyResponse deleteAPPolicyResponse = DeleteAccessPointPolicyResponse.builder().build();
        when(proxyClient.client().deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class))).thenReturn(deleteAPPolicyResponse);

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder()
                .bucket(BUCKET_NAME)
                .name(ACCESSPOINT_NAME)
                .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class))).thenReturn(getAccessPointResponse);

        final GetAccessPointPolicyResponse getAPPolicyResponse = GetAccessPointPolicyResponse.builder().build();
        when(proxyClient.client().getAccessPointPolicy(any(GetAccessPointPolicyRequest.class))).thenReturn(getAPPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_NO_POLICY_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client()).deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - Set policy
     */
    @Test
    public void handleRequest_Success_NullPolicyToPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_COMPLETE_MODEL)
                .previousResourceState(AP_NO_POLICY_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

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
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_COMPLETE_MODEL);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client(), never()).deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - Empty Policy - AccessDenied
     */
    @Test
    public void handleRequest_Error_EmptyPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_EMPTY_POLICY_MODEL)
                .previousResourceState(AP_NO_POLICY_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().putAccessPointPolicy(any(PutAccessPointPolicyRequest.class)))
                .thenThrow(constructS3ControlException("AccessDenied"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getResourceModel()).isEqualTo(AP_EMPTY_POLICY_MODEL);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client(), never()).deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error Path - DeleteAccessPointPolicy fails
     */
    @Test
    public void handleRequest_Error_DeleteAPPolicyFailure() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_NO_POLICY_MODEL)
                .previousResourceState(AP_EMPTY_POLICY_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class)))
                .thenThrow(constructS3ControlException("AccessDenied"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getResourceModel()).isEqualTo(AP_NO_POLICY_MODEL);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client(), never()).putAccessPointPolicy(any(PutAccessPointPolicyRequest.class));
        verify(proxyClient.client()).deleteAccessPointPolicy(any(DeleteAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
