package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static S3ControlClient getClient() {

        // AN TODO: [P0]: PLEASE REMOVE prior to making the repository public
        // https://sim.amazon.com/issues/SEAPORT-2652
        // The following code exists for testing against the beta endpoint. This was necessary for developing the resource.
//        return S3ControlClient.builder()
//                .overrideConfiguration(ClientOverrideConfiguration.builder().addExecutionInterceptor(new MyInterceptor()).build())
//                .httpClient(LambdaWrapper.HTTP_CLIENT)
//                .build();

        // For Production
        return S3ControlClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();

    }

}
