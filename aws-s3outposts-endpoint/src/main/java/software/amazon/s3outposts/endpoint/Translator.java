package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.model.NetworkInterface;
import software.amazon.awssdk.services.s3outposts.model.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Translator {

    // CreateEndpoint

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/CreateEndpointRequest.html
     *
     * @param model
     * @return
     */
    static CreateEndpointRequest translateToSdkCreateEndpointRequest(final ResourceModel model) {

        final EndpointAccessType endpointAccessType = EndpointAccessType.fromValue(model.getAccessType());

        return CreateEndpointRequest.builder()
                .outpostId(model.getOutpostId())
                .securityGroupId(model.getSecurityGroupId())
                .subnetId(model.getSubnetId())
                .accessType(endpointAccessType)
                .customerOwnedIpv4Pool(model.getCustomerOwnedIpv4Pool())
                .build();

    }

    // DeleteEndpoint

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/DeleteEndpointRequest.html
     *
     * @param model
     * @return
     */
    static DeleteEndpointRequest translateToSdkDeleteEndpointRequest(final ResourceModel model) {

        final EndpointArnFields endpointArnFields = EndpointArnFields.splitArn(model.getArn());
        final String outpostId = endpointArnFields.outpostId;
        final String endpointId = endpointArnFields.endpointId;

        return DeleteEndpointRequest.builder()
                .endpointId(endpointId)
                .outpostId(outpostId)
                .build();

    }

    // ListEndpoints

    /**
     * Ref: https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/s3outposts/model/ListEndpointsRequest.html
     *
     * @param nextToken
     * @return
     */
    static ListEndpointsRequest translateToSdkListEndpointsRequest(String nextToken) {

        return ListEndpointsRequest.builder().nextToken(nextToken).build();

    }

    static ResourceModel translateFromSdkEndpoint(final Endpoint endpoint) {

        return ResourceModel.builder()
                .arn(endpoint.endpointArn())
                .cidrBlock(endpoint.cidrBlock())
                .creationTime(endpoint.creationTime().toString())
                .networkInterfaces(translateFromSdkNetworkInterfaces(endpoint.networkInterfaces()))
                .outpostId(endpoint.outpostsId())
                .status(endpoint.statusAsString())
                .accessType(endpoint.accessTypeAsString())
                .customerOwnedIpv4Pool(endpoint.customerOwnedIpv4Pool())
                .subnetId(endpoint.subnetId())
                .securityGroupId(endpoint.securityGroupId())
                .build();

    }

    static ResourceModel translateFromSdkEc2Endpoint(final Endpoint endpoint, final String outpostId) {

        final String arn = endpoint.endpointArn().replaceFirst("/ec2/", String.format("/%s/", outpostId));
        return ResourceModel.builder()
                .arn(arn)
                .cidrBlock(endpoint.cidrBlock())
                .creationTime(endpoint.creationTime().toString())
                .networkInterfaces(translateFromSdkNetworkInterfaces(endpoint.networkInterfaces()))
                .outpostId(outpostId)
                .status(endpoint.statusAsString())
                .accessType(endpoint.accessTypeAsString())
                .customerOwnedIpv4Pool(endpoint.customerOwnedIpv4Pool())
                .subnetId(endpoint.subnetId())
                .securityGroupId(endpoint.securityGroupId())
                .build();

    }

    static Set<software.amazon.s3outposts.endpoint.NetworkInterface> translateFromSdkNetworkInterfaces(
            List<NetworkInterface> networkInterfaceList) {

        return Optional.ofNullable(networkInterfaceList).orElse(Collections.emptyList())
                .stream()
                .map(networkInterface -> software.amazon.s3outposts.endpoint.NetworkInterface.builder()
                        .networkInterfaceId(networkInterface.networkInterfaceId())
                        .build())
                .collect(Collectors.toSet());
    }

}
