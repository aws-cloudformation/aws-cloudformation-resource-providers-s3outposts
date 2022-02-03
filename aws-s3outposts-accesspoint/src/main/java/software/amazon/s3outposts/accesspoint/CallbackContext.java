package software.amazon.s3outposts.accesspoint;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {

    /**
     * Used to denote if propagation is completed or not
     */
    protected boolean propagated;
    /**
     * Number of times we want to add Propagation delay
     */
    protected int forcedDelayCount;
    /**
     * Used to denote if stabilization is completed or not
     */
    protected boolean stabilized;
    /**
     * Number of times we want to try and stabilize the Create/Delete Operation.
     * NOTE: We return a success if the resource has not reached terminal state after stabilization retries.
     */
    protected int stabilizationCount;
}
