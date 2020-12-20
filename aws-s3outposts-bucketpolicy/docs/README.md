# AWS::S3Outposts::BucketPolicy

Resource Type Definition for AWS::S3Outposts::BucketPolicy

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::S3Outposts::BucketPolicy",
    "Properties" : {
        "<a href="#bucket" title="Bucket">Bucket</a>" : <i>String</i>,
        "<a href="#policydocument" title="PolicyDocument">PolicyDocument</a>" : <i>Map</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::S3Outposts::BucketPolicy
Properties:
    <a href="#bucket" title="Bucket">Bucket</a>: <i>String</i>
    <a href="#policydocument" title="PolicyDocument">PolicyDocument</a>: <i>Map</i>
</pre>

## Properties

#### Bucket

The Amazon Resource Name (ARN) of the specified bucket.

_Required_: Yes

_Type_: String

_Minimum_: <code>20</code>

_Maximum_: <code>2048</code>

_Pattern_: <code>^arn:[^:]+:s3-outposts:[a-zA-Z0-9\-]+:\d{12}:outpost\/[^:]+\/bucket\/[^:]+$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### PolicyDocument

A policy document containing permissions to add to the specified bucket.

_Required_: Yes

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Bucket.
