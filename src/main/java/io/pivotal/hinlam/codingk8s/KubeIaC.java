package io.pivotal.hinlam.codingk8s;

import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;

public class KubeIaC {

    /*
    public V1Deployment getDeployment(){

        V1ObjectMeta metadata = new V1ObjectMeta();
        V1DeploymentSpec spec = new V1DeploymentSpec();
        V1PodTemplate template = new V1PodTemplate();
        V1PodTemplateSpec templateSpec = new V1PodTemplateSpec();
        templateSpec.setSpec();
        V1PodSpec podSpec = new V1PodSpec();
        podSpec.setContainers();

        template.setMetadata();

        V1Deployment deployment = new V1Deployment()
                .apiVersion("apps/v1")
                .kind("Deployment")
                .metadata(metadata)
                .spec(spec);


        deployment.apiVersion();


        V1Deployment deployment = new V1DeploymentBuilder()
                .withApiVersion("apps/v1")
                .withKind("Deployment")
                .withMetadata(metadata)
                .withSpec()
                .withNewSpec()
                    .withNewSelector()
                        .addToMatchLabels("app", "nginx")
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", "nginx")
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withImage("nginx:1.7.9")
                                .withName("nginx")
                                .addNewPort()
                                    .withContainerPort(80)
                                .endPort()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();

//Optionally Dump the YAML manifest
        System.out.println(Yaml.dump(deployment));

//Optionally call K8S API server directly
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        AppsV1Api appsApi = new AppsV1Api();
        appsApi.createNamespacedDeployment(
                "default",
                deployment, //This is the V1Deployment object
                null,
                null,
                null
        );
    }
     */

}
