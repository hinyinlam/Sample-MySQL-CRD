# KubeInfra-as Code (KIaC)
Kubernetes Infrastructure as Code (KIaC) is methodology for deploy and manage applications running on top of K8S.
Instead of Infra-as-Config, which is a set of static files, used by common tools such as Terraform / Helm, this methodology provides another way of using existing programming languages to dynamically generate and even as a runnable code.

## Custom Resource Definition
CRD is an mechanism in K8S which you can define your own `kind` of resource that K8S can store.
Your own operator thus able to read the stored CRD and react accordingly.

## Sample-MySQL-CRD
This reop is an example of CRD and its Kubernetes Operator (ie a controller) specifically for creating MySQL resource and as an educational purpose, knowledge about how CRD is created, interaction with controller and serve as an example for further development.

There is a companion [blog post](https://hinyinlam.medium.com/kubernetes-custom-resource-definition-implement-in-java-part-1-a9e726e78c98) explaining the details of CRD and how this repo was created.

## Not for production
This sample is far away from production ready as it deliberately target for textbook example and ignore operation readiness (upgrade/retry/corner cases/testing).
