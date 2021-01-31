package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.awssdk.services.s3outposts.model.Endpoint;
import software.amazon.awssdk.services.s3outposts.model.NetworkInterface;
import software.amazon.awssdk.services.s3outposts.model.S3OutpostsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;
    protected static final String REGION;
    protected static final String ACCOUNT_ID;
    protected static final String OUTPOST_ID;
    protected static final String SECURITY_GROUP_ID;
    protected static final String SUBNET_ID;
    protected static final String ID1, ID2;
    protected static final String ARN1, ARN2;
    protected static final String CIDR_BLOCK1, CIDR_BLOCK2;
    protected static final String CREATION_TIME1, CREATION_TIME2;
    protected static final NetworkInterface NETWORK_INTERFACE_ID1_1, NETWORK_INTERFACE_ID1_2;
    protected static final NetworkInterface NETWORK_INTERFACE_ID2_1, NETWORK_INTERFACE_ID2_2;
    protected static final List<NetworkInterface> NETWORK_INTERFACE_LIST1, NETWORK_INTERFACE_LIST2;
    protected static final software.amazon.s3outposts.endpoint.NetworkInterface
            MODEL_NETWORK_INTERFACE_ID1_1, MODEL_NETWORK_INTERFACE_ID1_2,
            MODEL_NETWORK_INTERFACE_ID2_1, MODEL_NETWORK_INTERFACE_ID2_2;
    protected static final List<software.amazon.s3outposts.endpoint.NetworkInterface>
            MODEL_NETWORK_INTERFACE_LIST1, MODEL_NETWORK_INTERFACE_LIST2;

    // mock values used for testing purposes only
    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        REGION = "us-east-1";
        ACCOUNT_ID = "12345789012";
        OUTPOST_ID = "op-12345678901234";
        SECURITY_GROUP_ID = "sg-00e44e8bca59d36bb";
        SUBNET_ID = "subnet-45b6de0f";
        ID1 = "12abcd3efghij4kl5m6";
        ARN1 = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/endpoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ID1);
        CIDR_BLOCK1 = "172.31.0.0/16";
        ID2 = "98abcd7efghij6kl5m4";
        ARN2 = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/endpoint/%s", REGION, ACCOUNT_ID, OUTPOST_ID, ID2);
        CIDR_BLOCK2 = "172.32.0.0/16";
        CREATION_TIME1 = "2020-01-01T10:00:00Z";
        CREATION_TIME2 = "2020-01-02T10:00:00Z";
        NETWORK_INTERFACE_ID1_1 = NetworkInterface.builder().networkInterfaceId("eni-00abcd1234567e89f").build();
        NETWORK_INTERFACE_ID1_2 = NetworkInterface.builder().networkInterfaceId("eni-01abcd2345678e90f").build();
        NETWORK_INTERFACE_ID2_1 = NetworkInterface.builder().networkInterfaceId("eni-02abcd1234567e89f").build();
        NETWORK_INTERFACE_ID2_2 = NetworkInterface.builder().networkInterfaceId("eni-03abcd2345678e90f").build();

        NETWORK_INTERFACE_LIST1 = Arrays.asList(NETWORK_INTERFACE_ID1_1, NETWORK_INTERFACE_ID1_2);
        NETWORK_INTERFACE_LIST2 = Arrays.asList(NETWORK_INTERFACE_ID2_1, NETWORK_INTERFACE_ID2_2);

        MODEL_NETWORK_INTERFACE_ID1_1 = software.amazon.s3outposts.endpoint.NetworkInterface.builder().networkInterfaceId("eni-00abcd1234567e89f").build();
        MODEL_NETWORK_INTERFACE_ID1_2 = software.amazon.s3outposts.endpoint.NetworkInterface.builder().networkInterfaceId("eni-01abcd2345678e90f").build();
        MODEL_NETWORK_INTERFACE_ID2_1 = software.amazon.s3outposts.endpoint.NetworkInterface.builder().networkInterfaceId("eni-02abcd1234567e89f").build();
        MODEL_NETWORK_INTERFACE_ID2_2 = software.amazon.s3outposts.endpoint.NetworkInterface.builder().networkInterfaceId("eni-03abcd2345678e90f").build();

        MODEL_NETWORK_INTERFACE_LIST1 = Arrays.asList(MODEL_NETWORK_INTERFACE_ID1_1, MODEL_NETWORK_INTERFACE_ID1_2);
        MODEL_NETWORK_INTERFACE_LIST2 = Arrays.asList(MODEL_NETWORK_INTERFACE_ID2_1, MODEL_NETWORK_INTERFACE_ID2_2);


    }

    // Endpoint Resources (used by ReadHandler & ListHandler)
    protected Endpoint endpoint1 = Endpoint.builder()
            .endpointArn(ARN1)
            .cidrBlock(CIDR_BLOCK1)
            .creationTime(Instant.parse(CREATION_TIME1))
            .networkInterfaces(NETWORK_INTERFACE_LIST1)
            .outpostsId(OUTPOST_ID)
            .status("Available")
            .build();

    protected ResourceModel model1 = ResourceModel.builder()
            .arn(ARN1)
            .cidrBlock(CIDR_BLOCK1)
            .creationTime(CREATION_TIME1)
            .networkInterfaces(MODEL_NETWORK_INTERFACE_LIST1)
            .outpostId(OUTPOST_ID)
            .status("Available")
            .build();

    protected Endpoint endpoint2 = Endpoint.builder()
            .endpointArn(ARN2)
            .cidrBlock(CIDR_BLOCK2)
            .creationTime(Instant.parse(CREATION_TIME2))
            .networkInterfaces(NETWORK_INTERFACE_LIST2)
            .outpostsId(OUTPOST_ID)
            .status("Pending")
            .build();

    protected ResourceModel model2 = ResourceModel.builder()
            .arn(ARN2)
            .cidrBlock(CIDR_BLOCK2)
            .creationTime(CREATION_TIME2)
            .networkInterfaces(MODEL_NETWORK_INTERFACE_LIST2)
            .outpostId(OUTPOST_ID)
            .status("Pending")
            .build();

    // Request Models

    protected static final ResourceModel REQ_MODEL_EMPTY = ResourceModel.builder().build();
    protected static final ResourceModel REQ_MODEL_CREATE = ResourceModel.builder()
            .outpostId(OUTPOST_ID)
            .securityGroupId(SECURITY_GROUP_ID)
            .subnetId(SUBNET_ID)
            .build();
    protected static final ResourceModel REQ_MODEL_ARN = ResourceModel.builder()
            .arn(ARN1)
            .build();
    protected static final ResourceModel REQ_MODEL_NO_ARN = ResourceModel.builder()
            .arn("")
            .build();

    // Response Models

    protected static final ResourceModel RESP_MODEL_CREATE = ResourceModel.builder()
            .arn(ARN1)
            .id(ID1)
            .outpostId(OUTPOST_ID)
            .securityGroupId(SECURITY_GROUP_ID)
            .subnetId(SUBNET_ID)
            .build();

    protected static Exception constructS3OutpostsExceptionWithStatusCode(Integer statusCode) {
        return S3OutpostsException.builder().statusCode(statusCode).build();
    }

    static ProxyClient<S3OutpostsClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final S3OutpostsClient sdkClient) {
        return new ProxyClient<S3OutpostsClient>() {
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
            public S3OutpostsClient client() {
                return sdkClient;
            }
        };
    }
}
