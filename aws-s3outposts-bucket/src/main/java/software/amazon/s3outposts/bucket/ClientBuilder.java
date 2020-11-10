package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  /*
  AN TODO: uncomment the following, replacing YourServiceClient with your service client name
  It is recommended to use static HTTP client so less memory is consumed
  e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ClientBuilder.java#L9
  */

  public static S3ControlClient getClient() {
    return S3ControlClient.builder()
              .httpClient(LambdaWrapper.HTTP_CLIENT)
              .build();
  }

}
