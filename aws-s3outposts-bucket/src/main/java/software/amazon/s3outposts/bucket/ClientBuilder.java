package software.amazon.s3outposts.bucket;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.cloudformation.LambdaWrapper;
//import java.net.URISyntaxException;

public class ClientBuilder {
  /*
  AN TODO: uncomment the following, replacing YourServiceClient with your service client name
  It is recommended to use static HTTP client so less memory is consumed
  e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/master/aws-logs-loggroup/src/main/java/software/amazon/logs/loggroup/ClientBuilder.java#L9
  */

    public static S3ControlClient getClient() {

//    RequestHandler2 customRequestHandler2 = new RequestHandler2() {
//      @Override
//      public void beforeAttempt(HandlerBeforeAttemptContext context) {
////        super.beforeAttempt(context);
//        context.getRequest().setEndpoint(new URI("https://northstar.beta.us-east-1.seaport.aws.a2z.com"));
//      }
//    };
//    try {
//      URI beta = new URI()
//    }

        // PLEASE REMOVE prior to making repository public
        // For Beta Testing
        return S3ControlClient.builder()
//            .endpointOverride(URI.create("https://northstar.beta.us-east-1.seaport.aws.a2z.com"))
                .overrideConfiguration(ClientOverrideConfiguration.builder().addExecutionInterceptor(new MyInterceptor()).build())
//            .endpointOverride(new URI("https://northstar.beta.us-east-1.seaport.aws.a2z.com"))
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();

//    // For Production
//    return S3ControlClient.builder()
//            .httpClient(LambdaWrapper.HTTP_CLIENT)
//            .build();

    }

}
