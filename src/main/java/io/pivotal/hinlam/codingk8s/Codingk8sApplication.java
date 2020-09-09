package io.pivotal.hinlam.codingk8s;

import com.google.gson.Gson;
import io.kubernetes.client.openapi.models.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Codingk8sApplication {

    public static void main(String[] args) throws IOException {
        getCRD();

        getCRDDatabase();

        //SpringApplication.run(Codingk8sApplication.class, args);
    }

    public static void getCRDDatabase(){

        V1ObjectMeta meta = new V1ObjectMetaBuilder()
                .withName("mydb")
                .build();


    }


    public static V1CustomResourceValidation getCRDValidation(){
        V1JSONSchemaProps mysql_cluster_size = new V1JSONSchemaPropsBuilder()
                .withType("integer")
                .withMaximum(7d)
                .build();

        V1JSONSchemaProps mysql_db_encoding = new V1JSONSchemaPropsBuilder()
                .withType("string")
                .withEnum("utf8", "swe7", "ascii", "cp932")
                .build();

        V1JSONSchemaProps mysql_version_property = new V1JSONSchemaPropsBuilder()
                .withType("number")
                .build();

        V1JSONSchemaProps autoscaling_by = new V1JSONSchemaPropsBuilder()
                .withType("string")
                .withEnum("IO", "CPU", "RAM", "Storage")
                .build();

        V1JSONSchemaProps autoscaling_out_threshold = new V1JSONSchemaPropsBuilder()
                .withType("number")
                .withMaximum(1d)
                .withMinimum(0d)
                .build();

        V1JSONSchemaProps autoscaling_in_threshold = new V1JSONSchemaPropsBuilder()
                .withType("number")
                .withMaximum(1d)
                .withMinimum(0d)
                //.withDefault(0.05) //does not work in K8S 1.15
                .build();

        Map<String, V1JSONSchemaProps> autoscaling_properties = new HashMap<>();
        autoscaling_properties.put("by", autoscaling_by);
        autoscaling_properties.put("scale-out", autoscaling_out_threshold);
        autoscaling_properties.put("scale-in", autoscaling_in_threshold);

        V1JSONSchemaProps mysql_cluster_autoscaling = new V1JSONSchemaPropsBuilder()
                .withProperties(autoscaling_properties)
                .withType("object")
                .withRequired(Arrays.asList("by", "scale-in", "scale-out"))
                .build();

        V1JSONSchemaProps cpu_resource_limit = new V1JSONSchemaPropsBuilder()
                .withType("integer")
                .withMinimum(1d)
                .build();

        V1JSONSchemaProps ram_resource_limit = new V1JSONSchemaPropsBuilder()
                .withType("string")
                .build();

        V1JSONSchemaProps disk_resource_limit = new V1JSONSchemaPropsBuilder()
                .withType("string")
                .build();

        Map<String, V1JSONSchemaProps> resource_limit_properties = new HashMap<>();
        resource_limit_properties.put("CPU", cpu_resource_limit);
        resource_limit_properties.put("RAM", ram_resource_limit);
        resource_limit_properties.put("Disk",disk_resource_limit);

        V1JSONSchemaProps mysql_resource_limit = new V1JSONSchemaPropsBuilder()
                .withProperties(resource_limit_properties)
                .withType("object")
                .build();

        Map<String, V1JSONSchemaProps> spec_properties = new HashMap<>();
        spec_properties.put("mysql-version",mysql_version_property);
        spec_properties.put("mysql-db-encoding", mysql_db_encoding);
        spec_properties.put("cluster-size", mysql_cluster_size);
        spec_properties.put("autoscaling", mysql_cluster_autoscaling);
        spec_properties.put("resource-limits", mysql_resource_limit);

        V1JSONSchemaProps top_spec = new V1JSONSchemaPropsBuilder()
                .withType("object")
                .withProperties(spec_properties)
                .withRequired(Arrays.asList("autoscaling"))
                .build();

        Map<String, V1JSONSchemaProps> top_spec_map = new HashMap<>();
        top_spec_map.put("spec", top_spec);


        V1JSONSchemaProps openApiSchema = new V1JSONSchemaPropsBuilder()
                .withType("object")
                .withProperties(top_spec_map)
                .withRequired(Arrays.asList("spec"))
                .withDescription("spec level description")
                .build();

        V1CustomResourceValidation validation = new V1CustomResourceValidationBuilder()
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

        V1CustomResourceColumnDefinition clusterSizeColumn = new V1CustomResourceColumnDefinitionBuilder()
                .withJsonPath(".spec.cluster-size")
                .withName("ClusterSize")
                .withDescription("Size of a cluster")
                .withType("integer")
                .build();

        V1CustomResourceSubresourceScale crsrScale =  new V1CustomResourceSubresourceScaleBuilder()
                .withSpecReplicasPath(".spec.clusterSize")
                .withStatusReplicasPath(".status.clusterSize")
                .build();

        Map<String, Integer> myCustomStatus = new HashMap<>();
        myCustomStatus.put("clusterSize",0);
        V1CustomResourceSubresources crdSubRes = new V1CustomResourceSubresourcesBuilder()
                .withStatus(myCustomStatus)
                .withScale(crsrScale)
                .build();

        V1CustomResourceDefinitionVersion crdV1 = new V1CustomResourceDefinitionVersionBuilder()
                .withName("v1alpha1")
                .withServed(true)
                .withStorage(true)
                .withAdditionalPrinterColumns(clusterSizeColumn)
                .withSchema(getCRDValidation())
                .withSubresources(crdSubRes)
                .build();

        V1CustomResourceDefinitionNames names = new V1CustomResourceDefinitionNamesBuilder()
                .withKind(kindName)
                .withSingular(singular)
                .withPlural(plural)
                .withShortNames("ms")
                .withCategories("hinlamdb", "all")
                .build();

        V1CustomResourceDefinitionSpec crdSpec = new V1CustomResourceDefinitionSpecBuilder()
                .withGroup(groupName)
                .withVersions(crdV1)
                .withScope("Namespaced")
                .withNames(names)
                .build();

        V1CustomResourceDefinitionStatus crdStatus = new V1CustomResourceDefinitionStatusBuilder()
                .withNewAcceptedNames()
                    .withKind(kindName)
                    .withPlural(plural)
                    .withSingular(singular)
                    .withListKind(listKind)
                .endAcceptedNames()
                .build();

        V1CustomResourceDefinition crd = new V1CustomResourceDefinitionBuilder()
                .withApiVersion("apiextensions.k8s.io/v1")
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
