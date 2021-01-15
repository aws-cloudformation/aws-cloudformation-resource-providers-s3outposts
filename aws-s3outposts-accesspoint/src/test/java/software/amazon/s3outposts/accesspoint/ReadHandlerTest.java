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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    private ReadHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    // Mock variables
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    // Pre-, Post- test steps
    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
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

        request = ResourceHandlerRequest.<ResourceModel>builder().build();

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
     * Validation error: No AccessPoint ARN provided in the model
     */
    @Test
    public void handleRequest_NoAccessPointArn() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_READ_NO_ARN_MODEL)
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
     * Happy Path - without Policy
     */
    @Test
    public void handleRequest_SimpleSuccess_withNullPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .build();

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
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_READ_RESPONSE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();
    }

    /**
     * Happy Path - more likely case (as compared to the one above), when no policy exists for the AccessPoint.
     */
    @Test
    public void handleRequest_SimpleSuccess_NoSuchAccessPointPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .build();

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
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_READ_RESPONSE);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - with Policy
     */
    @Test
    public void handleRequest_SimpleSuccess_withPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
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
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_READ_RESPONSE_WITH_POLICY);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Happy Path - with Empty Policy
     * Ideally this case should never occur since we don't allow empty policies in our Put requests.
     * Nevertheless, what we are testing here is that if there is an empty policy, will it be returned appropriately.
     */
    @Test
    public void handleRequest_SimpleSuccess_withEmptyPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .build();

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder()
                .bucket(BUCKET_NAME)
                .name(ACCESSPOINT_NAME)
                .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class))).thenReturn(getAccessPointResponse);

        final GetAccessPointPolicyResponse getAPPolicyResponse = GetAccessPointPolicyResponse.builder()
                .policy(EMPTY_POLICY)
                .build();
        when(proxyClient.client().getAccessPointPolicy(any(GetAccessPointPolicyRequest.class))).thenReturn(getAPPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(AP_READ_RESPONSE_WITH_EMPTY_POLICY);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * API Error: getAccessPoint returns failure
     */
    @Test
    public void handleRequest_GetAccessPoint_Error() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .build();

        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class)))
                .thenThrow(NotFoundException.class);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(progress.getCallbackContext()).isNotNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client(), never()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * API Error: getAccessPointPolicy returns failure
     */
    @Test
    public void handleRequest_GetAccessPointPolicy_Error() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .build();

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder()
                .bucket(BUCKET_NAME)
                .name(ACCESSPOINT_NAME)
                .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class))).thenReturn(getAccessPointResponse);

        when(proxyClient.client().getAccessPointPolicy(any(GetAccessPointPolicyRequest.class)))
                .thenThrow(constructS3ControlException("AccessDenied"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(progress.getCallbackContext()).isNotNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }


    /**
     * AccessPoint does not exist
     */
    @Test
    public void handleRequest_GetAccessPoint_NoSuchAccessPoint() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .build();

        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchAccessPoint"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);

        verify(proxyClient.client()).getAccessPoint(any(GetAccessPointRequest.class));
        verify(proxyClient.client(), never()).getAccessPointPolicy(any(GetAccessPointPolicyRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
