package io.pivotal.hinlam.codingk8s.controller;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.LeaderElectingController;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.ControllerManagerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.kubernetes.client.extended.leaderelection.LeaderElector;
import io.kubernetes.client.extended.leaderelection.resourcelock.EndpointsLock;
import io.kubernetes.client.informer.ListerWatcher;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watchable;
import okhttp3.Call;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

import static io.pivotal.hinlam.codingk8s.Codingk8sApplication.dumpYAMLwithWorkaround;

@Component
public class MySQLController {
    @Bean
    public CommandLineRunner minimumController(){
        return arg -> {
            SharedInformerFactory sharedInformerFactory = new SharedInformerFactory();
            Reconciler reconciler = new Reconciler() {
                @Override
                public Result reconcile(Request request) {
                    return null;
                }
            };
            Controller controller = ControllerBuilder.defaultBuilder(sharedInformerFactory)
                    .withReconciler(reconciler)
                    .build();
            ControllerManager manager = ControllerBuilder.controllerManagerBuilder(sharedInformerFactory)
                    .addController(controller)
                    .build();
            manager.run();
        };
    }

    //@Bean
    public CommandLineRunner runInformer(){
        return args ->{
            ApiClient client = Config.defaultClient();
            client.setReadTimeout(10);

            Configuration.setDefaultApiClient(client);

            CoreV1Api coreV1Api = new CoreV1Api();

            SharedInformerFactory informerFactory = new SharedInformerFactory();
            SharedIndexInformer<V1Pod> podInformer = informerFactory.sharedIndexInformerFor(
                    (CallGeneratorParams callGeneratorParams) -> {
                            System.out.println("======Informer=====");
                            System.out.println("Resource Version: " + callGeneratorParams.resourceVersion);
                            System.out.println("Timeout: " + callGeneratorParams.timeoutSeconds);
                            System.out.println("Watch: " + callGeneratorParams.watch);
                            System.out.println("===========");

                            Call podCall = coreV1Api.listNamespacedPodCall(
                                    "test",
                                    null,null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    callGeneratorParams.resourceVersion,
                                    null,
                                    callGeneratorParams.timeoutSeconds,
                                    callGeneratorParams.watch,
                                    null);

                        return podCall;
                    },
                    V1Pod.class,
                    V1PodList.class,
                    100000
            );
            ResourceEventHandler<V1Pod> reh = new ResourceEventHandler<V1Pod>() {
                @Override
                public void onAdd(V1Pod v1Pod) {
                    System.out.println("onAdd: " + v1Pod.getMetadata().getName() + " Version: " + v1Pod.getMetadata().getResourceVersion());
                }

                @Override
                public void onUpdate(V1Pod before, V1Pod after) {
                    System.out.println("onUpdate v1Pod: " + before.getMetadata().getName() + " Version: " + before.getMetadata().getResourceVersion());
                    //dumpYAMLwithWorkaround(v1Pod);
                    System.out.println("onUpdate apiType1: " + after.getMetadata().getName() + " Version: " + after.getMetadata().getResourceVersion());
                    //dumpYAMLwithWorkaround(apiType1);
                }

                @Override
                public void onDelete(V1Pod v1Pod, boolean b) {
                    System.out.println("onDelete: " + v1Pod.getMetadata().getName() + "boolean: " + b + " Version: " + v1Pod.getMetadata().getResourceVersion());
                }
            };
            //podInformer.addEventHandler(reh);

            PodPrintingReconciler reconciler = new PodPrintingReconciler();

            Controller controller = ControllerBuilder.defaultBuilder(informerFactory)
                    .withName("MyControllerName")
                    //.withReadyFunc(podInformer::hasSynced)
                    .watch(
                            (workQueue) -> {
                                return ControllerBuilder.controllerWatchBuilder(V1Pod.class,workQueue)
                                        .withOnAddFilter(item -> {
                                            System.out.println("Add filter: " + item.getClass().getName());
                                            return true;
                                        })
                                        /*
                                        .withWorkQueueKeyFunc(
                                                (V1Pod pod) -> new Request(pod.getMetadata().getName())
                                        )
                                         */
                                        .build();
                            }
                    )
                    .withReconciler(reconciler)
                    .withWorkerCount(1)
                    .build();

            ControllerManager manager = ControllerBuilder.controllerManagerBuilder(informerFactory)
                    .addController(controller)
                    .build();

            manager.run();
            /*
            LeaderElectingController leaderElectingController =
                    new LeaderElectingController(new LeaderElector(
                            new LeaderElectionConfig(
                                    new EndpointsLock("kube-system","hinlam-controller-leader-election","myidentity"+ UUID.randomUUID().toString()),
                                    //new ConfigMapLock("kube-system","custom-controller-configmap-lock","hinlam-controller-id" + UUID.randomUUID().toString()),
                                    Duration.ofMillis(10000),
                                    null,
                                    Duration.ofMillis(5000)
                            )
                    ),
                    manager);

            leaderElectingController.run();
             */
        };
    }

    static class PodPrintingReconciler implements Reconciler {

        private Lister<V1Pod> podLister;
        private SharedIndexInformer<V1Pod> informer;

        public PodPrintingReconciler(SharedIndexInformer<V1Pod> informer){
            System.out.println("Reconciler create!");
            Indexer<V1Pod> indexer = informer.getIndexer();
            this.informer = informer;
            podLister = new Lister<>(indexer,"test");
        }

        public PodPrintingReconciler(){
            //do nothing
        }

        @Override
        public Result reconcile(Request request) {
            /*
            V1Pod pod = podLister.get(request.getName());
            System.out.println(pod.getMetadata().getName());
             */
            System.out.println("Reconciler request:" + request.toString());
            Result r = new Result(false);
            return r;
        }
    }

}
