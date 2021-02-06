# AWS::S3Outposts::Endpoint

Resource Type Definition for AWS::S3Outposts::Endpoint

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::S3Outposts::Endpoint",
    "Properties" : {
        "<a href="#outpostid" title="OutpostId">OutpostId</a>" : <i>String</i>,
        "<a href="#securitygroupid" title="SecurityGroupId">SecurityGroupId</a>" : <i>String</i>,
        "<a href="#subnetid" title="SubnetId">SubnetId</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::S3Outposts::Endpoint
Properties:
    <a href="#outpostid" title="OutpostId">OutpostId</a>: <i>String</i>
    <a href="#securitygroupid" title="SecurityGroupId">SecurityGroupId</a>: <i>String</i>
    <a href="#subnetid" title="SubnetId">SubnetId</a>: <i>String</i>
</pre>

## Properties

#### OutpostId

The id of the customer outpost on which the bucket resides.

_Required_: Yes

_Type_: String

_Pattern_: <code>^(op-[a-f0-9]{17}|\d{12}|ec2)$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SecurityGroupId

The ID of the security group to use with the endpoint.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>100</code>

_Pattern_: <code>^sg-([0-9a-f]{8}|[0-9a-f]{17})$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### SubnetId

The ID of the subnet in the selected VPC.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>100</code>

_Pattern_: <code>^subnet-([0-9a-f]{8}|[0-9a-f]{17})$</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

## Return Values

### Ref

When you pass the logical ID of this resource to the intrinsic `Ref` function, Ref returns the Arn.

### Fn::GetAtt

The `Fn::GetAtt` intrinsic function returns a value for a specified attribute of this type. The following are the available attributes and sample return values.

For more information about using the `Fn::GetAtt` intrinsic function, see [Fn::GetAtt](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html).

#### Arn

The Amazon Resource Name (ARN) of the endpoint.

#### CidrBlock

The VPC CIDR committed by this endpoint.

#### CreationTime

The date value in ISO 8601 format. The timezone is always UTC. (YYYY-MM-DDThh:mm:ssZ)

#### Id

The ID of the endpoint.

#### NetworkInterfaces

The network interfaces of the endpoint.

#### Status

Returns the <code>Status</code> value.
