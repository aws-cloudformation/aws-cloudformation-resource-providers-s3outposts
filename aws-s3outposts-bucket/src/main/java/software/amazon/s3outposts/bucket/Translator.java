package software.amazon.s3outposts.bucket;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import software.amazon.awssdk.services.s3control.model.AbortIncompleteMultipartUpload;
import software.amazon.awssdk.services.s3control.model.LifecycleConfiguration;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.awssdk.utils.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static software.amazon.s3outposts.bucket.ArnFields.splitArn;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {

    /**
     * Translate from model to request
     *
     * @param model resource model
     * @return CreateBucketRequest
     * <p>
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/CreateBucketRequest.html
     */
    static CreateBucketRequest translateToCreateRequest(final ResourceModel model) {
        return CreateBucketRequest.builder()
                .bucket(model.getBucketName())
                .outpostId(model.getOutpostId())
                .build();
    }

    /**
     * Translate from model to request
     *
     * @param model resource model
     * @return GetBucketRequest
     * <p>
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetBucketRequest.html
     */
    static GetBucketRequest translateToReadRequest(final ResourceModel model,
                                                   final String accountId) {
        String arn = model.getArn();
        // AN TODO: [P0]: We should remove the EC2 specific code before releasing the resource.
        if ((model.getOutpostId() != null) && (arn != null)) {
            arn = arn.replaceFirst("/ec2/", String.format("/%s/", model.getOutpostId()));
            model.setArn(arn);
        }

        return GetBucketRequest.builder()
                .accountId(accountId)
                .bucket(arn)
                .build();
    }

    /**
     * Translates SDK response into a resource model
     *
     * @param getBucketResponse
     * @param arn
     * @return ResourceModel
     * <p>
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetBucketResponse.html
     */
    static ResourceModel translateFromReadResponse(final GetBucketResponse getBucketResponse, final String arn) {
        ArnFields arnFields = splitArn(arn);
        return ResourceModel.builder()
                .arn(arn)
                .bucketName(getBucketResponse.bucket())
                .outpostId(arnFields.outpostId)
                .build();
    }

    /**
     * Translate from model to request
     *
     * @param model resource model
     * @return DeleteBucketRequest
     * <p>
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/DeleteBucketRequest.html
     */
    static DeleteBucketRequest translateToDeleteRequest(final ResourceModel model,
                                                        final String accountId) {
        String arn = model.getArn();

        return DeleteBucketRequest.builder()
                .bucket(arn)
                .accountId(accountId)
                .build();
    }

    /**
     * Translate from model to request
     *
     * @param model     resource model
     * @param accountId extracted from the request
     * @param nextToken token passed to the aws service list resources request
     * @return ListRegionalBucketsRequest
     * <p>
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/ListRegionalBucketsRequest.html
     */
    static ListRegionalBucketsRequest translateToListRequest(final ResourceModel model,
                                                             final String accountId,
                                                             final String nextToken) {
        return ListRegionalBucketsRequest.builder()
                .accountId(accountId)
                .outpostId(model.getOutpostId())
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translate RegionalBucket to model
     * NOTE: listRegionalBuckets returns a list of RegionalBucket objects
     *
     * @param regionalBucket
     * @return ResourceModel
     * <p>
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/RegionalBucket.html
     */
    static ResourceModel translateFromRegionalBucket(final RegionalBucket regionalBucket,
                                                     final ResourceModel model) {

        String arn = regionalBucket.bucketArn();
        String outpostId = regionalBucket.outpostId();

        if (outpostId.equals("ec2") || arn.contains("ec2")) {
            arn = regionalBucket.bucketArn().replaceFirst("ec2", model.getOutpostId());
            outpostId = model.getOutpostId();
        }

        return ResourceModel.builder()
                .outpostId(outpostId)
                .bucketName(regionalBucket.bucket())
                .arn(arn)
                .build();
    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/PutBucketTaggingRequest.html
     *
     * @param resourceModel
     * @param resourceTags
     * @param systemTags
     * @param accountId
     * @return
     */
    static PutBucketTaggingRequest translateToSdkPutBucketTaggingRequest(final ResourceModel resourceModel,
                                                                         final Map<String, String> resourceTags,
                                                                         final Map<String, String> systemTags,
                                                                         final String accountId) {

        // Combine resourceTags and systemTags into one tag map
        Map<String, String> allTags = new HashMap<>();
        if (resourceTags != null)
            allTags.putAll(resourceTags);
//        if (systemTags != null)
//            allTags.putAll(systemTags);

        // Create a List of S3Tag objects from a Map<String, String>
        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/S3Tag.html
        final List<S3Tag> s3TagList = allTags.isEmpty() ? Collections.emptyList() :
                allTags.keySet().stream().map(
                        key -> S3Tag.builder().key(key).value(allTags.get(key)).build()
                ).collect(Collectors.toList());

        return PutBucketTaggingRequest.builder()
                .accountId(accountId)
                .bucket(resourceModel.getArn())
                .tagging(Tagging.builder().tagSet(s3TagList).build())
                .build();

    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetBucketTaggingRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static GetBucketTaggingRequest translateToSdkGetBucketTaggingRequest(final ResourceModel model,
                                                                         final String accountId) {

        return GetBucketTaggingRequest.builder()
                .accountId(accountId)
                .bucket(model.getArn())
                .build();

    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetBucketTaggingResponse.html
     *
     * @param getBucketTaggingResponse
     * @param model
     * @return
     */
    static ResourceModel translateFromSdkGetBucketTaggingResponse(final GetBucketTaggingResponse getBucketTaggingResponse,
                                                                  final ResourceModel model) {

        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/S3Tag.html
        final List<S3Tag> s3TagList = getBucketTaggingResponse.tagSet();
        model.setTags(translateTagsFromSdk(s3TagList));

        return model;

    }

    /**
     * Converts a List<S3Tag> to a List<Tag>, where `Tag` is defined in the Bucket resource model.
     * If the s3TagList is "null" or "empty", we will return an empty list. We do not return "null" for a List.
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/S3Tag.html
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L6
     *
     * @param s3TagList
     * @return
     */
    static List<software.amazon.s3outposts.bucket.Tag> translateTagsFromSdk(final Collection<S3Tag> s3TagList) {

        return Optional.ofNullable(s3TagList).orElse(Collections.emptyList())
                .stream()
                .map(s3Tag -> software.amazon.s3outposts.bucket.Tag.builder()
                        .key(s3Tag.key())
                        .value(s3Tag.value())
                        .build())
                .collect(Collectors.toList());

    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/DeleteBucketTaggingRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static DeleteBucketTaggingRequest translateToSdkDeleteBucketTaggingRequest(final ResourceModel model,
                                                                               final String accountId) {

        return DeleteBucketTaggingRequest.builder()
                .accountId(accountId)
                .bucket(model.getArn())
                .build();

    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetBucketLifecycleConfigurationRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static GetBucketLifecycleConfigurationRequest translateToSdkGetBucketLifecycleConfigurationRequest(final ResourceModel model,
                                                                                                       final String accountId) {

        return GetBucketLifecycleConfigurationRequest.builder()
                .accountId(accountId)
                .bucket(model.getArn())
                .build();

    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetBucketLifecycleConfigurationResponse.html
     *
     * @param getBucketLifecycleConfigurationResponse
     * @param model
     * @return
     */
    static ResourceModel translateFromSdkGetBucketLifecycleConfigurationResponse(final GetBucketLifecycleConfigurationResponse getBucketLifecycleConfigurationResponse,
                                                                                 final ResourceModel model) {

        // Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRule.html
        final List<LifecycleRule> lifecycleRuleList = getBucketLifecycleConfigurationResponse.rules();

        // Set "LifecycleConfiguration" field within the Bucket resource model
        model.setLifecycleConfiguration(software.amazon.s3outposts.bucket.LifecycleConfiguration.builder()
                .rules(translateLifecycleRulesFromSdk(lifecycleRuleList))
                .build());

        return model;

    }

    /**
     * Translate from SDK List<LifecycleRule> to Bucket resource model List<Rule>
     * If the List<LifecycleRule> is "null" or "empty", we return an "empty" list. We do not return a "null" list.
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRule.html
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L45
     *
     * @param lifecycleRuleList
     * @return
     */
    private static List<Rule> translateLifecycleRulesFromSdk(final Collection<LifecycleRule> lifecycleRuleList) {

        return Optional.ofNullable(lifecycleRuleList).orElse(Collections.emptyList())
                .stream()
                .map(lifecycleRule ->
                        Rule.builder()
                                .abortIncompleteMultipartUpload(translateFromSdkAbortIncompleteMultipartUpload(lifecycleRule.abortIncompleteMultipartUpload()))
                                .expirationInDays(Optional.of(lifecycleRule).map(LifecycleRule::expiration).map(LifecycleExpiration::days).orElse(null))
                                .expirationDate(Optional.of(lifecycleRule).map(LifecycleRule::expiration).map(LifecycleExpiration::date).map(Instant::toString).orElse(null))
                                .filter(translateFromSdkLifecycleRuleFilter(lifecycleRule.filter()))
                                .id(lifecycleRule.id())
                                .status(lifecycleRule.statusAsString())
                                .build()
                )
                .collect(Collectors.toList());

    }

    /**
     * Translates from SDK AbortIncompleteMultipartUpload to Bucket resource model's AbortIncompleteMultipartUpload
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/AbortIncompleteMultipartUpload.html
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L136
     *
     * @param abortIncompleteMultipartUpload
     * @return
     */
    private static software.amazon.s3outposts.bucket.AbortIncompleteMultipartUpload translateFromSdkAbortIncompleteMultipartUpload(
            AbortIncompleteMultipartUpload abortIncompleteMultipartUpload) {

        if (abortIncompleteMultipartUpload == null)
            return null;

        return software.amazon.s3outposts.bucket.AbortIncompleteMultipartUpload.builder()
                .daysAfterInitiation(abortIncompleteMultipartUpload.daysAfterInitiation() == null
                        ? null
                        : abortIncompleteMultipartUpload.daysAfterInitiation())
                .build();

    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRuleFilter.html
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L73
     *
     * @param lifecycleRuleFilter
     * @return
     */
    private static Filter translateFromSdkLifecycleRuleFilter(LifecycleRuleFilter lifecycleRuleFilter) {

        if (lifecycleRuleFilter == null)
            return null;

        return Filter.builder()
                .prefix(lifecycleRuleFilter.prefix() == null
                        ? null
                        : lifecycleRuleFilter.prefix())
                .tag(lifecycleRuleFilter.tag() == null
                        ? null
                        : FilterTag.builder()
                        .key(lifecycleRuleFilter.tag().key())
                        .value(lifecycleRuleFilter.tag().value())
                        .build())
                .andOperator(lifecycleRuleFilter.and() == null
                        ? null
                        : translateFromSdkLifecycleRuleAndOperator(lifecycleRuleFilter.and()))
                .build();


    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRuleAndOperator.html
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L178
     *
     * @param lifecycleRuleAndOperator
     * @return
     */
    private static FilterAndOperator translateFromSdkLifecycleRuleAndOperator(LifecycleRuleAndOperator lifecycleRuleAndOperator) {

        return FilterAndOperator.builder()
                .prefix(Optional.ofNullable(lifecycleRuleAndOperator.prefix()).orElse(null))
                .tags(lifecycleRuleAndOperator.hasTags()
                        ? translateFromSdkLifecycleRuleAndOperatorTags(lifecycleRuleAndOperator.tags())
                        : null)
                .build();

    }

    /**
     * Translates from the List<S3Tag> of the SDKs LifecycleRuleOperator to the List<FilterTag> of the Bucket resource model.
     * If the List<S3Tag> is "null" or "empty", we return an "empty" list. We do not return a "null" list.
     * <p>
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRuleAndOperator.html#tags--
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L155
     *
     * @param s3TagList
     * @return
     */
    private static List<FilterTag> translateFromSdkLifecycleRuleAndOperatorTags(List<S3Tag> s3TagList) {

        return Optional.ofNullable(s3TagList).orElse(Collections.emptyList())
                .stream()
                .map(s3Tag -> FilterTag.builder()
                        .key(s3Tag.key())
                        .value(s3Tag.value())
                        .build())
                .collect(Collectors.toList());

    }


    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/PutBucketLifecycleConfigurationRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static PutBucketLifecycleConfigurationRequest translateToSdkPutBucketLifecycleConfigurationRequest(ResourceModel model,
                                                                                                       String accountId) {

        return PutBucketLifecycleConfigurationRequest.builder()
                .accountId(accountId)
                .bucket(model.getArn())
                .lifecycleConfiguration(translateToSdkLifecycleConfiguration(model.getLifecycleConfiguration()))
                .build();

    }

    /**
     * Translate from Bucket resource model's LifecycleConfiguration to SDK's LifecycleConfiguration
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L28
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleConfiguration.html
     *
     * @param lifecycleConfiguration
     * @return
     */
    private static LifecycleConfiguration translateToSdkLifecycleConfiguration(
            software.amazon.s3outposts.bucket.LifecycleConfiguration lifecycleConfiguration) {

        return (lifecycleConfiguration == null
                ? null
                : LifecycleConfiguration.builder()
                .rules(translateToSdkLifecycleRules(lifecycleConfiguration.getRules()))
                .build());

    }

    /**
     * Translates from Bucket resource model's Rule to SDK's LifecycleRule
     * If List<Rule> is "null" or "empty", we will return an "empty" list. We do not return a "null" list.
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L45
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRule.html
     *
     * @param rules
     * @return
     */
    private static List<LifecycleRule> translateToSdkLifecycleRules(List<Rule> rules) {

        return Optional.ofNullable(rules).orElse(Collections.emptyList())
                .stream()
                .map(rule ->
                        LifecycleRule.builder()
                                .abortIncompleteMultipartUpload(
                                        translateToSdkAbortIncompleteMultipartUpload(rule.getAbortIncompleteMultipartUpload()))
                                .expiration(translateToSdkLifecycleExpiration(rule))
                                .filter(translateToSdkLifecycleRuleFilter(rule.getFilter()))
                                .id(Optional.ofNullable(rule.getId()).orElse(null))
                                .status(rule.getStatus())
                                .build()
                )
                .collect(Collectors.toList());

    }

    /**
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L136
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/AbortIncompleteMultipartUpload.html
     *
     * @param abortIncompleteMultipartUpload
     * @return
     */
    private static AbortIncompleteMultipartUpload translateToSdkAbortIncompleteMultipartUpload(
            software.amazon.s3outposts.bucket.AbortIncompleteMultipartUpload abortIncompleteMultipartUpload) {

        if (abortIncompleteMultipartUpload == null)
            return null;

        return AbortIncompleteMultipartUpload.builder()
                .daysAfterInitiation(abortIncompleteMultipartUpload.getDaysAfterInitiation())
                .build();

    }

    /**
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L64-L72
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleExpiration.html
     *
     * @param rule
     * @return
     */
    private static LifecycleExpiration translateToSdkLifecycleExpiration(Rule rule) {

        if (rule.getExpirationDate() == null && rule.getExpirationInDays() == null)
            return null;

        return LifecycleExpiration.builder()
                .date(translateStringToInstant(rule.getExpirationDate()))
                .days(Optional.ofNullable(rule.getExpirationInDays()).orElse(null))
                .build();


    }

    /**
     * Helper function to translate String type to Instant type
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleExpiration.html#date--
     *
     * @param expirationDate
     * @return
     */
    private static Instant translateStringToInstant(String expirationDate) {

        if (StringUtils.isEmpty(expirationDate))
            return null;

        try {

            return Instant.parse(expirationDate);

        } catch (final DateTimeParseException e) {

            return new DateTime(expirationDate, DateTimeZone.UTC).toDate().toInstant();

        }

    }

    /**
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L73
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRuleFilter.html
     *
     * @param filter
     * @return
     */
    private static LifecycleRuleFilter translateToSdkLifecycleRuleFilter(Filter filter) {

        if (filter == null)
            return null;

        return LifecycleRuleFilter.builder()
                .prefix(Optional.ofNullable(filter.getPrefix()).orElse(null))
                .tag(filter.getTag() == null
                        ? null
                        : S3Tag.builder()
                        .key(filter.getTag().getKey())
                        .value(filter.getTag().getValue())
                        .build())
                .and(filter.getAndOperator() == null
                        ? null
                        : translateToSdkLifecycleRuleAndOperator(filter.getAndOperator()))
                .build();

    }

    /**
     * @param filterAndOperator
     * @return
     */
    private static LifecycleRuleAndOperator translateToSdkLifecycleRuleAndOperator(
            FilterAndOperator filterAndOperator
    ) {

        return LifecycleRuleAndOperator.builder()
                .prefix(Optional.ofNullable(filterAndOperator.getPrefix()).orElse(null))
                .tags(translateToSdkLifecycleRuleAndOperatorTags(filterAndOperator.getTags()))
                .build();

    }

    /**
     * Translates List<FilterTag> from Bucket resource model to List<S3Tag> in SDKs LifecycleRuleOperator.
     * In this case, since this is part of a Put request, we send a "null" to the SDK if the user sent a "null".
     * <p>
     * Ref: aws-s3outposts-bucket/aws-s3outposts-bucket.json#L155
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/LifecycleRuleAndOperator.html#tags--
     *
     * @param filterTagsList
     * @return
     */
    private static List<S3Tag> translateToSdkLifecycleRuleAndOperatorTags(List<FilterTag> filterTagsList) {

        if (filterTagsList == null)
            return null;

        return Optional.ofNullable(filterTagsList).orElse(Collections.emptyList())
                .stream()
                .map(filterTag -> S3Tag.builder()
                        .key(filterTag.getKey())
                        .value(filterTag.getValue())
                        .build())
                .collect(Collectors.toList());

    }

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/DeleteBucketLifecycleConfigurationRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static DeleteBucketLifecycleConfigurationRequest translateToSdkDeleteBucketLifecycleConfigurationRequest(
            final ResourceModel model,
            final String accountId) {

        return DeleteBucketLifecycleConfigurationRequest.builder()
                .accountId(accountId)
                .bucket(model.getArn())
                .build();

    }

}
