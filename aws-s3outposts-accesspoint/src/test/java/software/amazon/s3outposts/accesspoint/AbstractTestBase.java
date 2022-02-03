package software.amazon.s3outposts.accesspoint;

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
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

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
    protected static final String BUCKET_ARN;
    protected static final String ACCESSPOINT_NAME;
    protected static final String ACCESSPOINT_ARN;
    protected static final String VPC_ID;
    protected static final String ACCESSPOINT_POLICY;
    protected static final String ACCESSPOINT_POLICY2;
    protected static final String EMPTY_POLICY;
    protected static final String NO_SUCH_ACCESSPOINT;
    protected static final int STABILIZATION_COUNT;

    // mock values used for testing purposes only
    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        REGION = "us-east-1";
        ACCOUNT_ID = "12345789012";
        OUTPOST_ID = "op-12345678901234";
        BUCKET_NAME = "bucket1";
        BUCKET_ARN = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/bucket/%s", REGION, ACCOUNT_ID, OUTPOST_ID, BUCKET_NAME);
        ACCESSPOINT_NAME = "ap1-bucket1";
        ACCESSPOINT_ARN = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/accesspoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ACCESSPOINT_NAME);
        VPC_ID = "vpc-123";
        ACCESSPOINT_POLICY = String.format("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"st1\",\"Effect\":\"Allow\"," +
                "\"Principal\":{\"AWS\":\"%s\"},\"Action\":\"*\",\"Resource\":\"%s\"}]}", ACCOUNT_ID, ACCESSPOINT_ARN);
        ACCESSPOINT_POLICY2 = String.format("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"st1\",\"Effect\":\"Allow\"," +
                "\"Principal\":{\"AWS\":\"%s\"},\"Action\":\"s3-outposts:*\",\"Resource\":\"%s\"}]}", ACCOUNT_ID, ACCESSPOINT_ARN);
        EMPTY_POLICY = "{}";
        NO_SUCH_ACCESSPOINT = "NoSuchAccessPoint";
        STABILIZATION_COUNT = 10;
    }

    protected static final ResourceModel AP_COMPLETE_MODEL = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .policy(getPolicyDocument(ACCESSPOINT_POLICY))
            .build();

    protected static final ResourceModel AP_COMPLETE_MODEL2 = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .policy(getPolicyDocument(ACCESSPOINT_POLICY2))
            .build();

    protected static final ResourceModel AP_CREATE_MODEL = ResourceModel.builder()
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .policy(getPolicyDocument(ACCESSPOINT_POLICY))
            .build();

    protected static final ResourceModel AP_NO_POLICY_MODEL = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .build();

    protected static final ResourceModel AP_EMPTY_POLICY_MODEL = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .policy(getPolicyDocument("{}"))
            .build();

    protected static final ResourceModel AP_NO_BUCKET_MODEL = ResourceModel.builder()
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .build();

    protected static final ResourceModel AP_NO_ACCESSPOINT_NAME_MODEL = ResourceModel.builder()
            .bucket(BUCKET_ARN)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .build();

    protected static final ResourceModel AP_NO_VPCCONFIGURATION_MODEL = ResourceModel.builder()
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .build();

    protected static final ResourceModel AP_ONLY_ARN_MODEL = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .build();

    protected static final ResourceModel AP_READ_RESPONSE = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .build();

    protected static final ResourceModel AP_READ_RESPONSE_WITH_POLICY = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .policy(getPolicyDocument(ACCESSPOINT_POLICY))
            .build();

    protected static final ResourceModel AP_READ_RESPONSE_WITH_EMPTY_POLICY = ResourceModel.builder()
            .arn(ACCESSPOINT_ARN)
            .bucket(BUCKET_ARN)
            .name(ACCESSPOINT_NAME)
            .vpcConfiguration(VpcConfiguration.builder().vpcId(VPC_ID).build())
            .policy(getPolicyDocument(EMPTY_POLICY))
            .build();

    protected static final ResourceModel AP_READ_NO_ARN_MODEL = ResourceModel.builder()
            .arn("")
            .build();

    protected static final ResourceModel BUCKET_ARN_MODEL = ResourceModel.builder()
            .bucket(BUCKET_ARN)
            .build();

    protected static Map<String, Object> getPolicyDocument(final String policy) {
        try {
            return MAPPER.readValue(policy, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new CfnGeneralServiceException(e);
        }
    }

    protected static Exception constructS3ControlException(String errorCode) {
        return S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build()).build();
    }

    protected static Exception constructS3ControlException(Integer statusCode, String errorCode, String errorMsg) {
        return S3ControlException.builder()
                .statusCode(statusCode)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).errorMessage(errorMsg).build())
                .build();
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
