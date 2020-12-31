package software.amazon.s3outposts.bucketpolicy;

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
     * Happy Path
     */
    @Test
    public void handleRequest_SimpleSuccess() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        final GetBucketPolicyResponse bucketPolicyResponse = GetBucketPolicyResponse.builder()
                .policy(BUCKET_POLICY)
                .build();
        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchBucketPolicy"))
                .thenReturn(bucketPolicyResponse);

        final PutBucketPolicyResponse putBucketPolicyResponse = PutBucketPolicyResponse.builder().build();
        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class))).thenReturn(putBucketPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * Validation error: Bucket ARN is required by CreateHandler.
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
     * Validation error: Policy Document is required by CreateHandler.
     */
    @Test
    public void handleRequest_NoBucketPolicy() {

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
     * preExistenceCheckForCreate: returns Error of type other than NoBucketPolicy.
     */
    @Test
    public void handleRequest_preExistenceCheck_Error_OtherException() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(NullPointerException.class);


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
     * preExistenceCheckForCreate: GetBucketPolicy returns valid policy.
     * This should fail the CreateHandler.
     */
    @Test
    public void handleRequest_preExistenceCheck_Done_AlreadyExists() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        final GetBucketPolicyResponse bucketPolicyResponse = GetBucketPolicyResponse.builder()
                .policy(BUCKET_POLICY)
                .build();
        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(bucketPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getCallbackContext()).isNotNull();
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }


    /**
     * preExistenceCheckForCreate: returns empty policy
     * createBucketPolicy: returns Success
     */
    @Test
    public void handleRequest_preExistenceCheck_Done_Empty() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        final GetBucketPolicyResponse bucketPolicyResponse1 =
                GetBucketPolicyResponse.builder().build();
        final GetBucketPolicyResponse bucketPolicyResponse2 =
                GetBucketPolicyResponse.builder()
                        .policy(BUCKET_POLICY)
                        .build();
        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(bucketPolicyResponse1).thenReturn(bucketPolicyResponse2);

        final PutBucketPolicyResponse putBucketPolicyResponse =
                PutBucketPolicyResponse.builder().build();
        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class)))
                .thenReturn(putBucketPolicyResponse);

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(progress.getResourceModels()).isNull();
        assertThat(progress.getMessage()).isNull();
        assertThat(progress.getErrorCode()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * preExistenceCheckForCreate: returns NoSuchBucketPolicy error
     * createBucketPolicy: return InvalidRequestException
     */
    @Test
    public void handleRequest_createBucketPolicy_Error_InvalidRequest() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchBucketPolicy"));

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
     * preExistenceCheckForCreate: returns NoSuchBucketPolicy error
     * createBucketPolicy: returns InternalServiceException
     */
    @Test
    public void handleRequest_createBucketPolicy_Error_InternalServiceException() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchBucketPolicy"));

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
     * preExistenceCheckForCreate: returns NoSuchBucketPolicy error
     * createBucketPolicy: returns AccessDenied error
     */
    @Test
    public void handleRequest_createBucketPolicy_Error_AccessDenied() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchBucketPolicy"));

        when(proxyClient.client().putBucketPolicy(any(PutBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("AccessDenied"));

        final ProgressEvent<ResourceModel, CallbackContext> progress =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(progress).isNotNull();
        assertThat(progress.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progress.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
        assertThat(progress.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(progress.getResourceModels()).isNull();

        verify(sdkClient, atLeastOnce()).serviceName();

    }

    /**
     * preExistenceCheckForCreate: returns NoSuchBucketPolicy error
     * createBucketPolicy: returns MalformedPolicy error
     */
    @Test
    public void handleRequest_createBucketPolicy_Error_MalformedPolicy() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchBucketPolicy"));

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
     * preExistenceCheckForCreate: returns NoSuchBucketPolicy error
     * createBucketPolicy: returns InternalError
     */
    @Test
    public void handleRequest_createBucketPolicy_Error_InternalError() {

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(BUCKET_POLICY_MODEL)
                .build();

        when(proxyClient.client().getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenThrow(constructS3ControlException("NoSuchBucketPolicy"));

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
