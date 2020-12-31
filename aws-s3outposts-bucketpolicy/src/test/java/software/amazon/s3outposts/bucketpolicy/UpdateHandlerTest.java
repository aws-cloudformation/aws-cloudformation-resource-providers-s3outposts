package software.amazon.s3outposts.bucketpolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3control.S3ControlClient;
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
    private ObjectMapper MAPPER = new ObjectMapper();
    private ResourceModel UPDATE_SUCCESS_MODEL = ResourceModel.builder()
            .bucket(ARN)
            .policyDocument(getPolicyDocument(BUCKET_POLICY))
            .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<S3ControlClient> proxyClient;

    @Mock
    S3ControlClient sdkClient;

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
     * Error: Bucket ARN is required
     */
    @Test
    public void handleRequest_NoBucket() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ONLY_POLICY_MODEL)
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

        verify(sdkClient, times(0)).serviceName();

    }

    /**
     * Error: PolicyDocument is required
     */
    @Test
    public void handleRequest_NoPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(ONLY_BUCKET_MODEL)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getMessage()).isEqualTo("Policy Document is required.");
        assertThat(progress.getCallbackContext()).isEqualTo(new CallbackContext());
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, times(0)).serviceName();

    }

    /**
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        final GetBucketPolicyResponse getBucketPolicyResponse =
                GetBucketPolicyResponse.builder()
                        .policy(BUCKET_POLICY)
                        .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(getBucketPolicyResponse);

        final PutBucketPolicyResponse putBucketPolicyResponse =
                PutBucketPolicyResponse.builder()
                        .build();
        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class)))
                .thenReturn(putBucketPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackContext()).isNull();
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }


    /**
     * preExistenceCheckForUpdate: returns NoSuchBucketPolicy error
     * UpdateHandler failure
     */
    @Test
    public void handleRequest_preExistenceCheckForUpdate_Error_NoSuchBucketPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchBucketPolicy"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(progress.getMessage()).isEqualTo("Bucket Policy does not exist.");
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * preExistenceCheckForUpdate: returns exception other than NoSuchBucketPolicy
     * UpdateHandler failure
     */
    @Test
    public void handleRequest_preExistenceCheckForUpdate_Error_OtherException() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * preExistenceCheckForUpdate: returns policy
     * updateBucketPolicy: returns InvalidRequestException
     * UpdateHandler failure
     */
    @Test
    public void handleRequest_updateBucketPolicy_Error_InvalidRequest() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        final GetBucketPolicyResponse getBucketPolicyResponse =
                GetBucketPolicyResponse.builder()
                        .policy(BUCKET_POLICY)
                        .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(getBucketPolicyResponse);

        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * preExistenceCheckForUpdate: returns policy
     * updateBucketPolicy: returns InternalServiceException
     * UpdateHandler failure
     */
    @Test
    public void handleRequest_updateBucketPolicy_Error_InternalServiceException() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        final GetBucketPolicyResponse getBucketPolicyResponse =
                GetBucketPolicyResponse.builder()
                        .policy(BUCKET_POLICY)
                        .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(getBucketPolicyResponse);


        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class)))
                .thenThrow(InternalServiceException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * preExistenceCheckForUpdate: returns policy
     * updateBucketPolicy: returns MalformedPolicy
     * UpdateHandler failure
     */
    @Test
    public void handleRequest_updateBucketPolicy_Error_MalformedPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        final GetBucketPolicyResponse getBucketPolicyResponse =
                GetBucketPolicyResponse.builder()
                        .policy(BUCKET_POLICY)
                        .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(getBucketPolicyResponse);


        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("MalformedPolicy"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * preExistenceCheckForUpdate: returns policy
     * updateBucketPolicy: returns InternalError
     * UpdateHandler failure
     */
    @Test
    public void handleRequest_updateBucketPolicy_Error_InternalError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(UPDATE_SUCCESS_MODEL)
                .build();

        final GetBucketPolicyResponse getBucketPolicyResponse =
                GetBucketPolicyResponse.builder()
                        .policy(BUCKET_POLICY)
                        .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(getBucketPolicyResponse);


        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("InternalError"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

}
