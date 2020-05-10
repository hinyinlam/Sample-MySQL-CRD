package io.pivotal.hinlam.codingk8s;

import com.google.gson.Gson;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.models.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class Codingk8sApplication {

    public static void main(String[] args) throws IOException {
        getCRD();

        //SpringApplication.run(Codingk8sApplication.class, args);
    }


    public static V1beta1CustomResourceValidation getCRDValidation(){
        V1beta1JSONSchemaProps mysql_cluster_size = new V1beta1JSONSchemaPropsBuilder()
                .withType("integer")
                .withMaximum(7d)
                .build();

        V1beta1JSONSchemaProps mysql_db_encoding = new V1beta1JSONSchemaPropsBuilder()
                .withType("string")
                .withEnum("utf8", "swe7", "ascii", "cp932")
                .build();

        V1beta1JSONSchemaProps mysql_version_property = new V1beta1JSONSchemaPropsBuilder()
                .withType("string")
                .build();

        V1beta1JSONSchemaProps autoscaling_by = new V1beta1JSONSchemaPropsBuilder()
                .withType("string")
                .withEnum("IO", "CPU", "RAM", "Storage")
                .build();

        V1beta1JSONSchemaProps autoscaling_up_threshold = new V1beta1JSONSchemaPropsBuilder()
                .withType("number")
                .withMaximum(1d)
                .withMinimum(0d)
                .build();

        V1beta1JSONSchemaProps autoscaling_down_threshold = new V1beta1JSONSchemaPropsBuilder()
                .withType("number")
                .withMaximum(1d)
                .withMinimum(0d)
                //.withDefault(0.05) //does not work in K8S 1.15
                .build();

        Map<String, V1beta1JSONSchemaProps> autoscaling_properties = new HashMap<>();
        autoscaling_properties.put("by", autoscaling_by);
        autoscaling_properties.put("scaleUp", autoscaling_up_threshold);
        autoscaling_properties.put("scaleDown", autoscaling_down_threshold);

        V1beta1JSONSchemaProps mysql_cluster_autoscaling = new V1beta1JSONSchemaPropsBuilder()
                .withProperties(autoscaling_properties)
                .withRequired(Arrays.asList("by", "scaleUp", "scaleDown"))
                .build();

        V1beta1JSONSchemaProps cpu_resource_limit = new V1beta1JSONSchemaPropsBuilder()
                .withType("integer")
                .withMinimum(1d)
                .build();

        V1beta1JSONSchemaProps ram_resource_limit = new V1beta1JSONSchemaPropsBuilder()
                .withType("string")
                .build();

        V1beta1JSONSchemaProps disk_resource_limit = new V1beta1JSONSchemaPropsBuilder()
                .withType("string")
                .build();

        Map<String, V1beta1JSONSchemaProps> resource_limit_properties = new HashMap<>();
        resource_limit_properties.put("CPU", cpu_resource_limit);
        resource_limit_properties.put("RAM", ram_resource_limit);
        resource_limit_properties.put("Disk",disk_resource_limit);

        V1beta1JSONSchemaProps mysql_resource_limit = new V1beta1JSONSchemaPropsBuilder()
                .withProperties(resource_limit_properties)
                .build();

        Map<String, V1beta1JSONSchemaProps> spec_properties = new HashMap<>();
        spec_properties.put("mysqlVersion",mysql_version_property);
        spec_properties.put("mysqlDbEncoding", mysql_db_encoding);
        spec_properties.put("clusterSize", mysql_cluster_size);
        spec_properties.put("autoscaling", mysql_cluster_autoscaling);
        spec_properties.put("limits", mysql_resource_limit);

        V1beta1JSONSchemaProps top_spec = new V1beta1JSONSchemaPropsBuilder()
                .withProperties(spec_properties)
                .withRequired(Arrays.asList("autoscaling"))
                .build();
        Map<String, V1beta1JSONSchemaProps> top_spec_map = new HashMap<>();
        top_spec_map.put("spec", top_spec);


        V1beta1JSONSchemaProps openApiSchema = new V1beta1JSONSchemaPropsBuilder()
                .withType("object")
                .withProperties(top_spec_map)
                .withRequired(Arrays.asList("spec"))
                .withDescription("spec level description")
                .build();

        V1beta1CustomResourceValidation validation = new V1beta1CustomResourceValidationBuilder()
                .withOpenAPIV3Schema(openApiSchema)
                .build();

        return validation;
    }

    public static void getCRD(){

        String groupName = "database.hinlam.io";
        String kindName = "MySQL";
        String listKind = kindName+"List";
        String singular = kindName.toLowerCase();
        String plural = kindName.toLowerCase()+"s";

        V1ObjectMeta crdMeta = new V1ObjectMetaBuilder()
                .withName("mysqls" + "." + groupName.toLowerCase())
                .build();

        V1beta1CustomResourceDefinitionVersion crdV1alpha1 = new V1beta1CustomResourceDefinitionVersionBuilder()
                .withName("v1alpha1")
                .withServed(true)
                .withStorage(true)
                .build();

        V1beta1CustomResourceDefinitionNames names = new V1beta1CustomResourceDefinitionNamesBuilder()
                .withKind(kindName)
                .withSingular(singular)
                .withPlural(plural)
                .withShortNames("ms")
                .withCategories("hinlamdb", "all")
                .build();

        V1beta1CustomResourceColumnDefinition clusterSizeColumn = new V1beta1CustomResourceColumnDefinitionBuilder()
                .withJsONPath(".spec.clusterSize")
                .withName("ClusterSize")
                .withDescription("Size of a cluster")
                .withType("integer")
                .build();

        Map<String, Integer> myCustomStatus = new HashMap<>();
        myCustomStatus.put("clusterSize",0);

        V1beta1CustomResourceSubresourceScale crsrScale =  new V1beta1CustomResourceSubresourceScaleBuilder()
                .withSpecReplicasPath(".spec.clusterSize")
                .withStatusReplicasPath(".status.clusterSize")
                .build();

        V1beta1CustomResourceSubresources crdSubRes = new V1beta1CustomResourceSubresourcesBuilder()
                .withStatus(myCustomStatus)
                .withScale(crsrScale)
                .build();

        V1beta1CustomResourceDefinitionSpec crdSpec = new V1beta1CustomResourceDefinitionSpecBuilder()
                .withGroup(groupName)
                .withVersions(crdV1alpha1)
                .withScope("Namespaced")
                .withNames(names)
                .withAdditionalPrinterColumns(Arrays.asList(clusterSizeColumn))
                .withValidation(getCRDValidation())
                .withSubresources(crdSubRes)
                .build();

        V1beta1CustomResourceDefinitionStatus crdStatus = new V1beta1CustomResourceDefinitionStatusBuilder()
                .withNewAcceptedNames()
                    .withKind(kindName)
                    .withPlural(plural)
                    .withSingular(singular)
                    .withListKind(listKind)
                .endAcceptedNames()
                .build();

        V1beta1CustomResourceDefinition crd = new V1beta1CustomResourceDefinitionBuilder()
                .withApiVersion("apiextensions.k8s.io/v1beta1")
                .withKind("CustomResourceDefinition")
                .withMetadata(crdMeta)
                .withSpec(crdSpec)
                .withStatus(crdStatus)
                .build();

        dumpYAMLwithWorkaround(crd);

    }

    public static void dumpYAMLwithWorkaround(Object crd){
        //System.out.println(Yaml.dump(crd));
        //You should see there is a bug that cannot dump the essential boolean value of V1beta1CustomResourceDefinitionVersion
        //See https://github.com/kubernetes-client/java/issues/340

        Gson gson = new Gson();
        String json = gson.toJson(crd);

        System.out.println(json);

        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        Object result = yaml.load(json);
        String output = yaml.dumpAsMap(result);

        System.out.println(output);

        FileWriter writer = null;
        try {
            writer = new FileWriter(new File("/tmp/crd.yaml"));
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
