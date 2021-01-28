package software.amazon.s3outposts.endpoint;

import org.apache.commons.lang3.StringUtils;

public class EndpointArnFields {

    String region, accountId, outpostId, endpointId;

    public EndpointArnFields() {
        region = "";
        accountId = "";
        outpostId = "";
        endpointId = "";
    }

    public EndpointArnFields(final String r, final String a, final String o, final String e) {

        region = r;
        accountId = a;
        outpostId = o;
        endpointId = e;

    }

    protected static EndpointArnFields splitArn(final String arn) {

        if (StringUtils.isEmpty(arn))
            return new EndpointArnFields();

        // An endpoint ARN for S3Outposts looks like:
        // arn:aws:s3-outposts:us-east-1:12345789012:outpost/op-12345678901234/endpoint/12abcd3efghij4kl5m6

        // Split the ARN into 2 parts:
        // * arn:aws:s3-outposts:us-east-1:123456789012
        // * op-12345678901234/endpoint/12abcd3efghij4kl5m6
        String parts[] = arn.split(":outpost/");

        // Extract region and accountId from part1
        //        [0]:[1]:    [2]    :  [3]    :  [4]
        // part1: arn:aws:s3-outposts:us-east-1:123456789012
        String region = parts[0].split(":")[3];
        String accountId = parts[0].split(":")[4];

        // Extract outpostId and endpointId from part2
        //               [0]        /endpoint/     [1]
        // part2: op-12345678901234/endpoint/12abcd3efghij4kl5m6
        String outpostId = parts[1].split("/endpoint/")[0];
        String endpointId = parts[1].split("/endpoint/")[1];

        return new EndpointArnFields(region, accountId, outpostId, endpointId);
    }

}
