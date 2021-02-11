package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;

public class ClientBuilder {

    public static S3OutpostsClient getClient() {

//        // AN TODO: [P0]: PLEASE REMOVE prior to making the repository public
//        // https://sim.amazon.com/issues/SEAPORT-2652
//        // The following code exists for testing against the beta endpoint. This was necessary for developing the resource.
//        return S3OutpostsClient.builder()
//                .endpointOverride(URI.create("https://northstar.beta.us-east-1.seaport.aws.a2z.com"))
//                .httpClient(LambdaWrapper.HTTP_CLIENT)
//                .build();

        return S3OutpostsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();

    }

}
