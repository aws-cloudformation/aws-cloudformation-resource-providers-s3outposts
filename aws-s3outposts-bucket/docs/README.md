# AWS::S3Outposts::Bucket

Resource Type Definition for AWS::S3Outposts::Bucket

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::S3Outposts::Bucket",
    "Properties" : {
        "<a href="#bucketname" title="BucketName">BucketName</a>" : <i>String</i>,
        "<a href="#outpostid" title="OutpostId">OutpostId</a>" : <i>String</i>,
        "<a href="#tags" title="Tags">Tags</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
        "<a href="#lifecycleconfiguration" title="LifecycleConfiguration">LifecycleConfiguration</a>" : <i><a href="lifecycleconfiguration.md">LifecycleConfiguration</a></i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::S3Outposts::Bucket
Properties:
    <a href="#bucketname" title="BucketName">BucketName</a>: <i>String</i>
    <a href="#outpostid" title="OutpostId">OutpostId</a>: <i>String</i>
    <a href="#tags" title="Tags">Tags</a>: <i>
      - <a href="tag.md">Tag</a></i>
    <a href="#lifecycleconfiguration" title="LifecycleConfiguration">LifecycleConfiguration</a>: <i><a href="lifecycleconfiguration.md">LifecycleConfiguration</a></i>
</pre>

## Properties

#### BucketName

A name for the bucket.

_Required_: Yes

_Type_: String

_Minimum_: <code>3</code>

_Maximum_: <code>63</code>

_Pattern_: <code>(?=^.{3,63}$)(?!^(\d+\.)+\d+$)(^(([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])\.)*([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])$)</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### OutpostId

The id of the customer outpost on which the bucket resides.

_Required_: Yes

_Type_: String

_Pattern_: <code>^(op-[a-f0-9]{17}|\d{12}|ec2)$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Tags

An arbitrary set of tags (key-value pairs) for this S3Outposts bucket.

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### LifecycleConfiguration

_Required_: No

_Type_: <a href="lifecycleconfiguration.md">LifecycleConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The Amazon Resource Name (ARN) of the specified bucket.
