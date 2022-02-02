package software.amazon.s3outposts.accesspoint;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    private DeleteHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(S3ControlClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(sdkClient);
    }

    /**
     * ValidationError - No AccessPoint ARN provided
     */
    @Test
    public void handleRequest_NoAccessPointArn() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .awsAccountId(ACCOUNT_ID)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        final CallbackContext callbackContext = new CallbackContext();
        callbackContext.setStabilizationCount(STABILIZATION_COUNT);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("AccessPoint ARN is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(callbackContext);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SuccessAfterMaxStabilizationRetries_Pending() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        CallbackContext callbackContext = new CallbackContext();
        callbackContext.setStabilizationCount(2);

        final DeleteAccessPointResponse deleteAccessPointResponse = DeleteAccessPointResponse.builder().build();
        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenReturn(deleteAccessPointResponse);

        final GetAccessPointResponse getAccessPointResponse = GetAccessPointResponse.builder().build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class)))
                .thenReturn(getAccessPointResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(progress.getCallbackContext().stabilized).isEqualTo(false);
        assertThat(progress.getCallbackContext().propagated).isEqualTo(false);
        assertThat(progress.getCallbackContext().forcedDelayCount).isEqualTo(1);
        assertThat(progress.getCallbackContext().stabilizationCount).isEqualTo(0);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(20);
        assertThat(progress.getResourceModel()).isEqualTo(AP_ONLY_ARN_MODEL);
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();
    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess_Complete() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        final DeleteAccessPointResponse deleteAccessPointResponse = DeleteAccessPointResponse.builder().build();
        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenReturn(deleteAccessPointResponse);

        S3ControlException s3ControlException = (S3ControlException) S3ControlException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(NO_SUCH_ACCESSPOINT).build())
                .build();
        when(proxyClient.client().getAccessPoint(any(GetAccessPointRequest.class)))
                .thenThrow(s3ControlException);

        CallbackContext callbackContext = new CallbackContext();
        callbackContext.setPropagated(true);
        callbackContext.setStabilized(true);
        callbackContext.setForcedDelayCount(4);
        callbackContext.setStabilizationCount(2);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, callbackContext, proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isNull();
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(proxyClient.client(), times(1)).getAccessPoint(any(GetAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();
    }

    /**
     * Error Path
     */
    @Test
    public void handleRequest_Error() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenThrow(constructS3ControlException("InvalidAccessPoint"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - 400 BadRequest - InvalidAccessPointState
     */
    @Test
    public void handleRequest_Error_InvalidAccessPointState() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenThrow(constructS3ControlException(400, "InvalidRequest", "Access Point is not in a state where it can be deleted"));


        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(20);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - 400 BadRequest - InvalidRequest with wrong message
     */
    @Test
    public void handleRequest_Error_InvalidRequest_WrongMessage() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenThrow(constructS3ControlException(400, "InvalidRequest", "Access Point is in an invalid state"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Error - 400 BadRequest - BadRequest with correct message
     */
    @Test
    public void handleRequest_Error_BadRequest_CorrectMessage() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(AP_ONLY_ARN_MODEL)
                .awsAccountId(ACCOUNT_ID)
                .build();

        when(proxyClient.client().deleteAccessPoint(any(DeleteAccessPointRequest.class)))
                .thenThrow(constructS3ControlException(400, "BadRequest", "Access Point is not in a state where it can be deleted"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getCallbackContext()).isEqualToComparingOnlyGivenFields(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);

        verify(proxyClient.client()).deleteAccessPoint(any(DeleteAccessPointRequest.class));
        verify(sdkClient, atLeastOnce()).serviceName();

    }
}
