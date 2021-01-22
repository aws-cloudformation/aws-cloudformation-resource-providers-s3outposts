package software.amazon.s3outposts.accesspoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3control.model.VpcConfiguration;
import software.amazon.awssdk.services.s3control.model.*;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.Map;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Translate from model to request
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/CreateAccessPointRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static CreateAccessPointRequest translateToCreateAPRequest(final ResourceModel model, final String accountId) {

        return CreateAccessPointRequest.builder()
                .accountId(accountId)
                .bucket(model.getBucket())
                .name(model.getName())
                .vpcConfiguration(VpcConfiguration.builder().vpcId(model.getVpcConfiguration().getVpcId()).build())
                .build();

    }

    /**
     * Translate from model to request
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/DeleteAccessPointRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static DeleteAccessPointRequest translateToDeleteAPRequest(final ResourceModel model,
                                                             final String accountId) {
        return DeleteAccessPointRequest.builder()
                .accountId(accountId)
                .name(model.getArn())
                .build();
    }

    /**
     * Translate from model to request
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/DeleteAccessPointPolicyRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static DeleteAccessPointPolicyRequest translateToDeleteAPPolicyRequest(final ResourceModel model, final String accountId) {

        return DeleteAccessPointPolicyRequest.builder()
                .accountId(accountId)
                .name(model.getArn())
                .build();

    }

    /**
     * Translate from model to request
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetAccessPointRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static GetAccessPointRequest translateToGetAPRequest(final ResourceModel model, final String accountId) {

        return GetAccessPointRequest.builder()
                .accountId(accountId)
                .name(model.getArn())
                .build();

    }

    /**
     * Translates from model to request
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetAccessPointPolicyRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static GetAccessPointPolicyRequest translateToGetAPPolicyRequest(final ResourceModel model, final String accountId) {

        return GetAccessPointPolicyRequest.builder()
                .accountId(accountId)
                .name(model.getArn())
                .build();

    }

    /**
     * Translate from model to request
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/PutAccessPointPolicyRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static PutAccessPointPolicyRequest translateToPutAPPolicyRequest(final ResourceModel model, final String accountId) {

        return PutAccessPointPolicyRequest.builder()
                .accountId(accountId)
                .name(model.getArn())
                .policy(convertJsonObjectToString(model.getPolicy()))
                .build();

    }

    /**
     * Translates SDK response to model
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetAccessPointResponse.html
     *
     * @param getAccessPointResponse
     * @param arn
     * @return
     */
    static ResourceModel translateFromGetAPResponse(final GetAccessPointResponse getAPResponse, final ResourceModel model) {

        final String accessPointArn = model.getArn();
        final String bucketName = getAPResponse.bucket();
        final String accessPointName = getAPResponse.name();

        // The model requires the bucketArn. The SDK response contains the bucketName.
        final String replaceFrom = String.format("accesspoint/%s", accessPointName);
        final String replaceTo = String.format("bucket/%s", bucketName);
        final String bucketArn = accessPointArn.replaceFirst(replaceFrom, replaceTo);

        return ResourceModel.builder()
                .arn(accessPointArn)
                .bucket(bucketArn)
                .name(accessPointName)
                .vpcConfiguration(software.amazon.s3outposts.accesspoint.VpcConfiguration.builder()
                        .vpcId(getAPResponse.vpcConfiguration().vpcId())
                        .build())
                .build();

    }

    /**
     * Translates from response to model
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetAccessPointPolicyResponse.html
     *
     * @param getAPPolicyResponse
     * @param model
     * @return
     */
    static ResourceModel translateFromGetAPPolicyResponse(final GetAccessPointPolicyResponse getAPPolicyResponse, final ResourceModel model) {

        if (getAPPolicyResponse.policy() != null) {
            model.setPolicy(convertStringToJsonObject(getAPPolicyResponse.policy()));
        }
        return model;

    }


    /**
     * Request to list resources
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/ListAccessPointsRequest.html
     *
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListAccessPointsRequest translateToListRequest(final ResourceModel model,
                                                          final String accountId,
                                                          final String nextToken) {
        return ListAccessPointsRequest.builder()
                .accountId(accountId)
                .bucket(model.getBucket())
                .nextToken(nextToken)
                .build();
    }

    /**
     * Translates from AccessPoint object to resource model.
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/AccessPoint.html
     *
     * @param accessPoint
     * @param model
     * @return
     */
    static ResourceModel translateFromAccessPoint(final AccessPoint accessPoint,
                                                  final ResourceModel model) {


        // AN TODO: [P0]: Please revisit this code block prior to making the resource public.
        // We do need the statement to get the accessPointArn for real outposts as well.
        // Start: Code Block
        // Get outpostId from bucket arn and replace `ec2` with outpostId in accesspoint arn.
        final BucketArnFields arnFields = BucketArnFields.splitArn(model.getBucket());
        final String accessPointArn = accessPoint.accessPointArn()
                .replaceFirst("/ec2/", String.format("/%s/", arnFields.outpostId));
        // End: Code Block

        return ResourceModel.builder()
                .arn(accessPointArn)
                .bucket(model.getBucket())
                .name(accessPoint.name())
                .vpcConfiguration(software.amazon.s3outposts.accesspoint.VpcConfiguration.builder().vpcId(accessPoint.vpcConfiguration().vpcId()).build())
                .build();
    }

    /**
     * Converts a String to a JSON object.
     * Ref: https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html#readValue(java.lang.String,%20com.fasterxml.jackson.core.type.TypeReference)
     *
     * @param policy
     * @return
     */
    static Map<String, Object> convertStringToJsonObject(final String policy) {
        try {
            return MAPPER.readValue(policy, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new CfnGeneralServiceException(e);
        }
    }

    /**
     * Converts user inputted JSON to a String
     *
     * @param policyDocument
     * @return
     */
    static String convertJsonObjectToString(Map<String, Object> policyDocument) {
        try {
            return MAPPER.writeValueAsString(policyDocument);
        } catch (Exception e) {
            throw new CfnInvalidRequestException(e);
        }
    }

}
