[![Build Status](https://travis-ci.org/DennisDenuto/go-aws-ami-poller.svg)](https://travis-ci.org/DennisDenuto/go-aws-ami-poller)

AWS AMI Poller Plugin for Go
==================================

Introduction
------------
This is a [package material](http://www.thoughtworks.com/products/docs/go/13.3/help/package_material.html) plugin for [Go](http://www.thoughtworks.com/products/go-continuous-delivery). It is currently capable of polling [AWS AMI](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html/) for a specific region.

The behaviour and capabilities of the plugin is determined to a significant extent by that of the package material extension point in Go. Be sure to read the package material documentation before using this plugin.

Installation
------------
Just drop aws-ami-poller.jar into plugins/external directory and restart Go. More details [here](http://www.thoughtworks.com/products/docs/go/13.3/help/plugin_user_guide.html)

Repository definition
---------------------
The AWS region must be a [valid region](http://docs.aws.amazon.com/general/latest/gr/rande.html#ec2_region).


Package definition
------------------
- AMI name (required) spec refers to the name assigned to the ami at creation time. (Using wildcards are allowed)
- Architecture (optional) accepts the following values (i386 | x86_64)
- Tag-Key (optional)  refers to the key of a tag assigned to the ami. This filter is independent of the tag-value filter. For example,  if  you  use both  the  filter  "tag-key=Purpose" and the filter "tag-value=X", you get any resources assigned both the tag key  Purpose  (regardless of what the tag's value is), and the tag value X (regardless of what the tag's key is). If you  want  to  list  only  resources
where Purpose is X, see the tag :key =*value* filter.
- Tag-Value (optional) refers to the value of a tag assigned to the resource. This filter is independent of the tag-key filter.

Published Environment Variables
-------------------------------
The following information is made available as environment variables for tasks:

    GO_PACKAGE_<REPO-NAME>_<PACKAGE-NAME>_LABEL (AMI id)
    GO_REPO_<REPO-NAME>_<PACKAGE-NAME>_REGION (Region AMI belongs to)

Logging
-------------------------------
The default logging level for the plugin is set to INFO.
You can override default value by setting system property 'plugin.pluginId_placeholder.log.level' to required logging level. For example, to set the logging level to WARN for plugin with id 'ami-poller', system property 'plugin.ami-poller.log.level' should be set to WARN.

Notes
-----
This plugin will detect at max one package revision per minute (the default interval at which Go materials poll). If multiple versions of a package get published to a repo in the time interval between two polls, Go will register the next sequential (by creationdate) version in that interval.
