package software.amazon.s3outposts.endpoint;

import software.amazon.awssdk.services.s3outposts.S3OutpostsClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;

public class ClientBuilder {

    public static S3OutpostsClient getClient() {

        return S3OutpostsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();

    }

}
