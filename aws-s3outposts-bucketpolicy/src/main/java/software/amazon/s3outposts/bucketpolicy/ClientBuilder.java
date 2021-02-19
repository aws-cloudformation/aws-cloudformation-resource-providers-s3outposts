package software.amazon.s3outposts.bucketpolicy;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static S3ControlClient getClient() {

        return S3ControlClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();

    }

}
