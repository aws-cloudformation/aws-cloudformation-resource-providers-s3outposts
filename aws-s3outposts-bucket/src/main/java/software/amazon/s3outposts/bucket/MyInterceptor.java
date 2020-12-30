package software.amazon.s3outposts.bucket;


import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;

public class MyInterceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        // AN TODO: [P0]: The file `MyInterceptor.java` should not be removed prior to making the Bucket resource public.
        // https://sim.amazon.com/issues/SEAPORT-2652
        // This file was introduced to enable testing against the beta endpoint.
        return context.httpRequest().toBuilder().host("northstar.beta.us-east-1.seaport.aws.a2z.com").build();
    }
}
