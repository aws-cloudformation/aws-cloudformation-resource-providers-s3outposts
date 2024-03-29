# AWS::S3Outposts::Bucket LifecycleConfiguration

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#rules" title="Rules">Rules</a>" : <i>[ <a href="rule.md">Rule</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#rules" title="Rules">Rules</a>: <i>
      - <a href="rule.md">Rule</a></i>
</pre>

## Properties

#### Rules

A list of lifecycle rules for individual objects in an Amazon S3Outposts bucket.

_Required_: Yes

_Type_: List of <a href="rule.md">Rule</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
