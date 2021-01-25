package software.amazon.s3outposts.bucket;

import java.util.Map;
import java.util.stream.Collectors;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("aws-s3outposts-bucket.json");
    }

    /**
     * Implementing this method in order to set the resource tags in the field `desiredResourceTags` of the `request` object.
     * NOTE: Providers should implement this method if their resource has a 'Tags' property to define resource-level tags
     *
     * @return
     */
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {

        if (resourceModel.getTags() == null) {
            return null;
        } else {
            return resourceModel.getTags().stream()
                    .collect(Collectors.toMap(tag -> tag.getKey(), tag -> tag.getValue()));
        }

    }

}
