package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static S3ControlClient getClient() {

        return S3ControlClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();

    }

}
