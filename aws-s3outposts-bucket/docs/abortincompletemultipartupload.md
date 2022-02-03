# AWS::S3Outposts::Bucket AbortIncompleteMultipartUpload

Specifies the days since the initiation of an incomplete multipart upload that Amazon S3Outposts will wait before permanently removing all parts of the upload.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#daysafterinitiation" title="DaysAfterInitiation">DaysAfterInitiation</a>" : <i>Integer</i>
}
</pre>

### YAML

<pre>
<a href="#daysafterinitiation" title="DaysAfterInitiation">DaysAfterInitiation</a>: <i>Integer</i>
</pre>

## Properties

#### DaysAfterInitiation

Specifies the number of days after which Amazon S3Outposts aborts an incomplete multipart upload.

_Required_: Yes

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
