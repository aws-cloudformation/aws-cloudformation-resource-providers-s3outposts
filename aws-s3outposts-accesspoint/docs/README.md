# AWS::S3Outposts::AccessPoint

Resource Type Definition for AWS::S3Outposts::AccessPoint

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::S3Outposts::AccessPoint",
    "Properties" : {
        "<a href="#bucket" title="Bucket">Bucket</a>" : <i>String</i>,
        "<a href="#name" title="Name">Name</a>" : <i>String</i>,
        "<a href="#vpcconfiguration" title="VpcConfiguration">VpcConfiguration</a>" : <i><a href="vpcconfiguration.md">VpcConfiguration</a></i>,
        "<a href="#policy" title="Policy">Policy</a>" : <i>Map</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::S3Outposts::AccessPoint
Properties:
    <a href="#bucket" title="Bucket">Bucket</a>: <i>String</i>
    <a href="#name" title="Name">Name</a>: <i>String</i>
    <a href="#vpcconfiguration" title="VpcConfiguration">VpcConfiguration</a>: <i><a href="vpcconfiguration.md">VpcConfiguration</a></i>
    <a href="#policy" title="Policy">Policy</a>: <i>Map</i>
</pre>

## Properties

#### Bucket

The Amazon Resource Name (ARN) of the bucket you want to associate this AccessPoint with.

_Required_: Yes

_Type_: String

_Minimum_: <code>20</code>

_Maximum_: <code>2048</code>

_Pattern_: <code>^arn:[^:]+:s3-outposts:[a-zA-Z0-9\-]+:\d{12}:outpost\/[^:]+\/bucket\/[^:]+$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Name

A name for the AccessPoint.

_Required_: Yes

_Type_: String

_Minimum_: <code>3</code>

_Maximum_: <code>50</code>

_Pattern_: <code>^[a-z0-9]([a-z0-9\\-]*[a-z0-9])?$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### VpcConfiguration

_Required_: Yes

_Type_: <a href="vpcconfiguration.md">VpcConfiguration</a>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### Policy

The access point policy associated with this access point.

_Required_: No

_Type_: Map

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The Amazon Resource Name (ARN) of the specified AccessPoint.
