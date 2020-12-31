package software.amazon.s3outposts.bucketpolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3control.model.DeleteBucketPolicyRequest;
import software.amazon.awssdk.services.s3control.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3control.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3control.model.PutBucketPolicyRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.Map;

import static software.amazon.s3outposts.bucketpolicy.ArnFields.splitArn;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Translate from model to PutBucketPolicyRequest
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/PutBucketPolicyRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static PutBucketPolicyRequest translateToPutRequest(final ResourceModel model,
                                                        final String accountId) {

        return PutBucketPolicyRequest.builder()
                .accountId(accountId)
                .bucket(model.getBucket())
                .policy(convertJsonObjectToString(model.getPolicyDocument()))
                .build();
    }

    /**
     * Translate from model to GetBucketPolicyRequest
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/GetBucketPolicyRequest.html
     *
     * @param model
     * @param accountId
     * @return
     */
    static GetBucketPolicyRequest translateToReadRequest(final ResourceModel model,
                                                         final String accountId) {
        String arn = model.getBucket();
        ArnFields arnFields = splitArn(arn);
        return GetBucketPolicyRequest.builder()
                .accountId(accountId)
                .bucket(arn)
                .build();
    }

    /**
     * Translates GetBucketPolicyResponse object to resource model
     *
     * @param sdkResponse
     * @param model
     * @return ResourceModel with populated policy
     */
    static ResourceModel translateFromReadResponse(final GetBucketPolicyResponse sdkResponse,
                                                   final ResourceModel model) {
        return ResourceModel.builder()
                .bucket(model.getBucket())
                .policyDocument(convertStringToJsonObject(sdkResponse.policy()))
                .build();
    }

    /**
     * Converts a String to a JSON object.
     * Ref: https://fasterxml.github.io/jackson-databind/javadoc/2.7/com/fasterxml/jackson/databind/ObjectMapper.html#readValue(java.lang.String,%20com.fasterxml.jackson.core.type.TypeReference)
     *
     * @param policy
     * @return Map<String, Object>
     */
    static Map<String, Object> convertStringToJsonObject(final String policy) {
        try {
            return MAPPER.readValue(policy, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
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
        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException(e);
        }
    }


    /**
     * Translates model to DeleteBucketPolicy request
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3control/model/DeleteBucketPolicyRequest.html
     *
     * @param model
     * @return
     */
    static DeleteBucketPolicyRequest translateToDeleteRequest(final ResourceModel model,
                                                              final String accountId) {
        return DeleteBucketPolicyRequest.builder()
                .accountId(accountId)
                .bucket(model.getBucket())
                .build();
    }

}
