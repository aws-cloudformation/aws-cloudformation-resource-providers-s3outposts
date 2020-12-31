package software.amazon.s3outposts.bucketpolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AbstractTestBase {
    protected static ObjectMapper MAPPER = new ObjectMapper();

    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;
    protected static final String REGION;
    protected static final String ACCOUNT_ID;
    protected static final String OUTPOST_ID;
    protected static final String BUCKET_NAME;
    protected static final String ARN;
    protected static final String BUCKET_POLICY;
    protected static final String BUCKET_POLICY_ARN;
    protected static final String EMPTY_POLICY;

    // mock values used for testing purposes only
    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        REGION = "us-east-1";
        ACCOUNT_ID = "12345789012";
        OUTPOST_ID = "op-12345678901234";
        BUCKET_NAME = "bucket1";
        ARN = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/bucket/%s", REGION, ACCOUNT_ID, OUTPOST_ID, BUCKET_NAME);
        BUCKET_POLICY = String.format("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"st1\",\"Effect\":\"Allow\"," +
                "\"Principal\":{\"AWS\":\"%s\"},\"Action\":\"*\",\"Resource\":\"%s\"}]}", ACCOUNT_ID, ARN);
        BUCKET_POLICY_ARN = String.format("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"st1\",\"Effect\":\"Allow\"," +
                "\"Principal\":{\"AWS\":\"arn:aws:iam::%s:root\"},\"Action\":\"*\",\"Resource\":\"%s\"}]}", ACCOUNT_ID, ARN);
        EMPTY_POLICY = "{}";
    }

    protected static final ResourceModel ONLY_BUCKET_MODEL = ResourceModel.builder()
            .bucket(ARN)
            .build();

    protected static final ResourceModel ONLY_POLICY_MODEL = ResourceModel.builder()
            .policyDocument(getPolicyDocument(BUCKET_POLICY))
            .build();

    protected static final ResourceModel BUCKET_POLICY_MODEL = ResourceModel.builder()
            .bucket(ARN)
            .policyDocument(getPolicyDocument(BUCKET_POLICY))
            .build();

    protected static final ResourceModel GET_BUCKET_POLICY_RESPONSE = ResourceModel.builder()
            .bucket(ARN)
            .policyDocument(getPolicyDocument(BUCKET_POLICY))
            .build();

    /**
     * Invocation of getPolicyDocument returns the policy document .
     *
     * @param request {@link ResourceHandlerRequest <ResourceModel>}
     * @return Returns policy document
     */
    protected static String getPolicyDocument(final ResourceModel request) {
        try {
            return MAPPER.writeValueAsString(request.getPolicyDocument());
        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException(e);
        }
    }

    /**
     * Invocation of getPolicyDocument returns the policy document
     *
     * @param policy
     * @return Map<String, Object>
     */
    protected static Map<String, Object> getPolicyDocument(final String policy) {
        try {
            return MAPPER.readValue(policy, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new CfnGeneralServiceException(e);
        }
    }

    protected static Exception constructS3ControlException(String errorCode) {
        return S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build()).build();
    }


    static ProxyClient<S3ControlClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final S3ControlClient sdkClient) {
        return new ProxyClient<S3ControlClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
            CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
            IterableT
            injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
            injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public S3ControlClient client() {
                return sdkClient;
            }
        };
    }
}
