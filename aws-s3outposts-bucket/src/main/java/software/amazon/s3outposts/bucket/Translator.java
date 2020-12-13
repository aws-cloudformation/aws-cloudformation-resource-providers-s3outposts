package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.model.*;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

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
            arn = arn.replaceFirst("ec2", model.getOutpostId());
            System.out.printf("Modified ARN: %s \n", arn);
            model.setArn(arn);
        }
//        ArnFields arnFields = splitArn(arn);
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
//        ArnFields arnFields = splitArn(arn);
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
            System.out.printf("Modified - Arn: %s, OutpostId: %s \n", arn, outpostId);
        }

        return ResourceModel.builder()
                .outpostId(outpostId)
                .bucketName(regionalBucket.bucket())
                .arn(arn)
                .build();
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}
