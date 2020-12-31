package software.amazon.s3outposts.bucketpolicy;

public class ArnFields {
    String region, accountId, outpostId, bucket;

    public ArnFields() {
        region = "";
        accountId = "";
        outpostId = "";
        bucket = "";
    }

    public ArnFields(String r, String a, String o, String b) {
        region = r;
        accountId = a;
        outpostId = o;
        bucket = b;
    }

    protected static ArnFields splitArn(final String arn) {

        if (arn == null) {
            return new ArnFields();
        }

        System.out.println(String.format("arn is %s", arn));

        // An arn for S3Outposts service looks like:
        // arn:aws:s3-outposts:us-west-2:12345:outpost/op-12345/bucket/my-bucket

        // Split the ARN into 2 parts:
        // * arn:aws:s3-outposts:us-west-2:12345
        // * op-12345/bucket/my-bucket
        String parts[] = arn.split(":outpost/");

        // Extract region and accountId from part1
        //        [0]:[1]:    [2]    :  [3]    :  [4]
        // part1: arn:aws:s3-outposts:us-west-2:12345
        String region = parts[0].split(":")[3];
        String accountId = parts[0].split(":")[4];


        // Extract outpostId and bucket from part2
        //          [0]    /bucket/   [1]
        // part2: op-12345/bucket/my-bucket

        String outpostId = parts[1].split("/bucket/")[0];
        String bucket = parts[1].split("/bucket/")[1];

        return new ArnFields(region, accountId, outpostId, bucket);
    }
}
