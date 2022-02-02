# AWS::S3Outposts::Bucket Rule

Specifies lifecycle rules for an Amazon S3Outposts bucket. You must specify at least one of the following: AbortIncompleteMultipartUpload, ExpirationDate, ExpirationInDays.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#status" title="Status">Status</a>" : <i>String</i>,
    "<a href="#id" title="Id">Id</a>" : <i>String</i>,
    "<a href="#abortincompletemultipartupload" title="AbortIncompleteMultipartUpload">AbortIncompleteMultipartUpload</a>" : <i><a href="abortincompletemultipartupload.md">AbortIncompleteMultipartUpload</a></i>,
    "<a href="#expirationdate" title="ExpirationDate">ExpirationDate</a>" : <i>String</i>,
    "<a href="#expirationindays" title="ExpirationInDays">ExpirationInDays</a>" : <i>Integer</i>,
    "<a href="#filter" title="Filter">Filter</a>" : <i><a href="rule.md">Rule</a></i>
}
</pre>

### YAML

<pre>
<a href="#status" title="Status">Status</a>: <i>String</i>
<a href="#id" title="Id">Id</a>: <i>String</i>
<a href="#abortincompletemultipartupload" title="AbortIncompleteMultipartUpload">AbortIncompleteMultipartUpload</a>: <i><a href="abortincompletemultipartupload.md">AbortIncompleteMultipartUpload</a></i>
<a href="#expirationdate" title="ExpirationDate">ExpirationDate</a>: <i>String</i>
<a href="#expirationindays" title="ExpirationInDays">ExpirationInDays</a>: <i>Integer</i>
<a href="#filter" title="Filter">Filter</a>: <i><a href="rule.md">Rule</a></i>
</pre>

## Properties

#### Status

_Required_: Yes

_Type_: String

_Allowed Values_: <code>Enabled</code> | <code>Disabled</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Id

Unique identifier for the lifecycle rule. The value can't be longer than 255 characters.

_Required_: No

_Type_: String

_Maximum_: <code>255</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### AbortIncompleteMultipartUpload

Specifies the days since the initiation of an incomplete multipart upload that Amazon S3Outposts will wait before permanently removing all parts of the upload.

_Required_: Yes

_Type_: <a href="abortincompletemultipartupload.md">AbortIncompleteMultipartUpload</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ExpirationDate

The date value in ISO 8601 format. The timezone is always UTC. (YYYY-MM-DDThh:mm:ssZ)

_Required_: Yes

_Type_: String

_Pattern_: <code>^([0-2]\d{3})-(0[0-9]|1[0-2])-([0-2]\d|3[01])T([01]\d|2[0-4]):([0-5]\d):([0-6]\d)((\.\d{3})?)Z$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ExpirationInDays

Indicates the number of days after creation when objects are deleted from Amazon S3Outposts.

_Required_: Yes

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Filter

_Required_: No

_Type_: <a href="rule.md">Rule</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
