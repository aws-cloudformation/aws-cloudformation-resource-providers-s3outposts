package software.amazon.s3outposts.bucket;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    protected boolean stabilized;
    protected boolean propagated;
    protected int forcedDelayCount;
}
