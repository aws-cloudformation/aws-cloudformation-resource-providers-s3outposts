package software.amazon.s3outposts.endpoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    protected boolean stabilized;
    protected Integer stabilizationRetriesRemaining;
}
