package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.model.AbortIncompleteMultipartUpload;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class AbstractTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final LoggerProxy logger;
    protected static final String REGION;
    protected static final String ACCOUNT_ID;
    protected static final String OUTPOST_ID;
    protected static final String BUCKET_NAME;
    protected static final String ARN;
    // Tagging
    protected static final Tag TAG1, TAG2;
    protected static final S3Tag S3TAG1, S3TAG2;
    protected static final List<Tag> TAG_LIST;
    protected static final Map<String, String> TAG_MAP;
    protected static final List<S3Tag> S3TAG_LIST;
    // LifecycleConfiguration
    protected static final FilterTag FILTER_TAG1, FILTER_TAG2;
    protected static final Rule RULE1, RULE2, RULE3, RULE4, RULE5, RULE6, RULE7, RULE8, RULE9;
    protected static final LifecycleRule LIFECYCLE_RULE1, LIFECYCLE_RULE2, LIFECYCLE_RULE3, LIFECYCLE_RULE4, LIFECYCLE_RULE5,
            LIFECYCLE_RULE6, LIFECYCLE_RULE7, LIFECYCLE_RULE8, LIFECYCLE_RULE9;
    protected static final List<Rule> RULE_LIST, RULE_LIST1, RULE_LIST2;
    protected static final List<LifecycleRule> LIFECYCLE_RULE_LIST, LIFECYCLE_RULE_LIST1, LIFECYCLE_RULE_LIST2;

    // mock values used for testing purposes only.
    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        logger = new LoggerProxy();
        REGION = "us-east-1";
        ACCOUNT_ID = "12345789012";
        OUTPOST_ID = "op-12345678901234";
        BUCKET_NAME = "bucket1";
        ARN = String.format("arn:aws:s3-outposts:%s:%s:outpost/%s/bucket/%s", REGION, ACCOUNT_ID, OUTPOST_ID, BUCKET_NAME);
        TAG1 = Tag.builder().key("key1").value("value1").build();
        TAG2 = Tag.builder().key("key2").value("value2").build();
        S3TAG1 = S3Tag.builder().key("key1").value("value1").build();
        S3TAG2 = S3Tag.builder().key("key2").value("value2").build();
        TAG_LIST = new ArrayList<Tag>() {{
            add(TAG1);
            add(TAG2);
        }};
        S3TAG_LIST = Arrays.asList(S3TAG1, S3TAG2);
        TAG_MAP = new HashMap<String, String>() {{
            put(TAG1.getKey(), TAG1.getValue());
            put(TAG2.getKey(), TAG2.getValue());
        }};
        FILTER_TAG1 = FilterTag.builder().key("key1").value("value1").build();
        FILTER_TAG2 = FilterTag.builder().key("key2").value("value2").build();
        RULE1 = Rule.builder()
                .id("1")
                .abortIncompleteMultipartUpload(
                        software.amazon.s3outposts.bucket.AbortIncompleteMultipartUpload.builder().daysAfterInitiation(2).build())
                .status("Enabled")
                .build();
        RULE2 = Rule.builder()
                .expirationInDays(4)
                .status("Disabled")
                .build();
        RULE3 = Rule.builder()
                .expirationDate("2020-02-25T10:00:00Z")
                .status("Enabled")
                .build();
        RULE4 = Rule.builder()
                .expirationInDays(4)
                .filter(Filter.builder().prefix("k").build())
                .status("Disabled")
                .build();
        RULE5 = Rule.builder()
                .id("5")
                .expirationInDays(4)
                .filter(Filter.builder().tag(FILTER_TAG1).build())
                .status("Enabled")
                .build();
        RULE6 = Rule.builder()
                .expirationInDays(4)
                .filter(Filter.builder().andOperator(
                        FilterAndOperator.builder().prefix("k").tags(Arrays.asList(FILTER_TAG1, FILTER_TAG2)).build())
                        .build())
                .status("Disabled")
                .build();
        RULE7 = Rule.builder()
                .expirationInDays(4)
                .filter(Filter.builder().andOperator(
                        FilterAndOperator.builder().tags(Arrays.asList(FILTER_TAG1, FILTER_TAG2)).build())
                        .build())
                .status("Enabled")
                .build();
        // Invalid rule: missing (abortIncompleteMultipartUpload or expiration) and status
        RULE8 = Rule.builder()
                .id("8")
                .filter(Filter.builder().andOperator(
                        FilterAndOperator.builder().prefix("k").tags(Arrays.asList(FILTER_TAG1)).build())
                        .build())
                .build();
        // Invalid rule: missing abortIncompleteMultipartUpload or expiration
        RULE9 = Rule.builder()
                .id("9")
                .status("Enabled")
                .build();

        LIFECYCLE_RULE1 = LifecycleRule.builder()
                .id("1")
                .abortIncompleteMultipartUpload(
                        AbortIncompleteMultipartUpload.builder().daysAfterInitiation(2).build())
                .status("Enabled")
                .build();
        LIFECYCLE_RULE2 = LifecycleRule.builder()
                .expiration(LifecycleExpiration.builder().days(4).build())
                .status("Disabled")
                .build();
        LIFECYCLE_RULE3 = LifecycleRule.builder()
                .expiration(LifecycleExpiration.builder().date(Instant.parse("2020-02-25T10:00:00Z")).build())
                .status("Enabled")
                .build();
        LIFECYCLE_RULE4 = LifecycleRule.builder()
                .expiration(LifecycleExpiration.builder().days(4).build())
                .filter(LifecycleRuleFilter.builder().prefix("k").build())
                .status("Disabled")
                .build();
        LIFECYCLE_RULE5 = LifecycleRule.builder()
                .id("5")
                .expiration(LifecycleExpiration.builder().days(4).build())
                .filter(LifecycleRuleFilter.builder().tag(S3TAG1).build())
                .status("Enabled")
                .build();
        LIFECYCLE_RULE6 = LifecycleRule.builder()
                .expiration(LifecycleExpiration.builder().days(4).build())
                .filter(LifecycleRuleFilter.builder().and(
                        LifecycleRuleAndOperator.builder().prefix("k").tags(S3TAG_LIST).build())
                        .build())
                .status("Disabled")
                .build();
        LIFECYCLE_RULE7 = LifecycleRule.builder()
                .expiration(LifecycleExpiration.builder().days(4).build())
                .filter(LifecycleRuleFilter.builder().and(
                        LifecycleRuleAndOperator.builder().tags(S3TAG_LIST).build())
                        .build())
                .status("Enabled")
                .build();
        LIFECYCLE_RULE8 = LifecycleRule.builder()
                .id("8")
                .filter(LifecycleRuleFilter.builder().and(
                        LifecycleRuleAndOperator.builder().prefix("k").tags(Arrays.asList(S3TAG1)).build())
                        .build())
                .build();
        LIFECYCLE_RULE9 = LifecycleRule.builder()
                .id("9")
                .status("Enabled")
                .build();

        RULE_LIST = new ArrayList<Rule>() {{
            add(RULE1);
            add(RULE2);
            add(RULE3);
            add(RULE4);
            add(RULE5);
            add(RULE6);
            add(RULE7);
            add(RULE8);
            add(RULE9);
        }};

        LIFECYCLE_RULE_LIST = Arrays.asList(LIFECYCLE_RULE1, LIFECYCLE_RULE2, LIFECYCLE_RULE3, LIFECYCLE_RULE4,
                LIFECYCLE_RULE5, LIFECYCLE_RULE6, LIFECYCLE_RULE7, LIFECYCLE_RULE8, LIFECYCLE_RULE9);

        RULE_LIST1 = Arrays.asList(RULE1, RULE2, RULE3, RULE4);
        RULE_LIST2 = Arrays.asList(RULE4, RULE5, RULE6, RULE7);
        LIFECYCLE_RULE_LIST1 = Arrays.asList(LIFECYCLE_RULE1, LIFECYCLE_RULE2, LIFECYCLE_RULE3, LIFECYCLE_RULE4);
        LIFECYCLE_RULE_LIST2 = Arrays.asList(LIFECYCLE_RULE4, LIFECYCLE_RULE5, LIFECYCLE_RULE6, LIFECYCLE_RULE7);

    }

    // Request Models

    protected static final ResourceModel REQ_BUCKET_MODEL_NO_TAGS_AND_RULES = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .build();

    protected static final ResourceModel REQ_BUCKET_MODEL_EMPTY_TAGS = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(Collections.emptyList())
            .build();

    protected static final ResourceModel REQ_BUCKET_MODEL_WITH_TAGS = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(TAG_LIST)
            .build();

    protected static final ResourceModel REQ_BUCKET_MODEL_NO_RULES = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().build())
            .build();

    protected static final ResourceModel REQ_BUCKET_MODEL_EMPTY_RULES = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(Collections.emptyList()).build())
            .build();

    protected static final ResourceModel REQ_BUCKET_MODEL_RULES = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(RULE_LIST).build())
            .build();

    protected static final ResourceModel REQ_BUCKET_MODEL_ONLY_ARN = ResourceModel.builder()
            .arn(ARN)
            .build();

    protected static final ResourceModel REQ_BUCKET_MODEL_NO_ARN = ResourceModel.builder()
            .arn("")
            .build();

    protected static final ResourceModel UPDATE_REQ_BUCKET_MODEL_WITH_TAGS_NO_ARN = ResourceModel.builder()
            .arn("")
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(TAG_LIST)
            .build();

    // Response models

    protected static final ResourceModel BUCKET_MODEL_NO_TAGS_AND_RULES = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .build();

    protected static final ResourceModel BUCKET_MODEL_EMPTY_TAGS = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(Collections.emptyList())
            .build();

    protected static final ResourceModel BUCKET_MODEL_WITH_TAGS = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(TAG_LIST)
            .build();

    protected static final ResourceModel BUCKET_MODEL_NO_RULES = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(null).build())
            .build();

    protected static final ResourceModel BUCKET_MODEL_EMPTY_RULES = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(Collections.emptyList()).build())
            .build();

    protected static final ResourceModel BUCKET_MODEL_EMPTY_TAGS_AND_RULES = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(Collections.emptyList())
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(Collections.emptyList()).build())
            .build();

    protected static final ResourceModel BUCKET_MODEL_RULES = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(RULE_LIST).build())
            .build();

    protected static final ResourceModel BUCKET_MODEL_RULES1 = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(RULE_LIST1).build())
            .build();

    protected static final ResourceModel BUCKET_MODEL_RULES2 = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(RULE_LIST2).build())
            .build();

    protected static final ResourceModel REQ_BUCKET_COMPLETE_MODEL = ResourceModel.builder()
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(TAG_LIST)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(RULE_LIST).build())
            .build();

    protected static final ResourceModel BUCKET_COMPLETE_MODEL = ResourceModel.builder()
            .arn(ARN)
            .bucketName(BUCKET_NAME)
            .outpostId(OUTPOST_ID)
            .tags(TAG_LIST)
            .lifecycleConfiguration(LifecycleConfiguration.builder().rules(RULE_LIST).build())
            .build();

    protected static Exception constructS3ControlExceptionWithErrorCode(String errorCode) {
        return S3ControlException.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build()).build();
    }

    protected static Exception constructS3ControlExceptionWithStatusCode(Integer statusCode) {
        return S3ControlException.builder().statusCode(statusCode).build();
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
