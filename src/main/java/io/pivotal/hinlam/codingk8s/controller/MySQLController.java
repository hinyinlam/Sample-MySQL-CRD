package io.pivotal.hinlam.codingk8s.controller;

import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.extended.controller.LeaderElectingController;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.leaderelection.LeaderElectionConfig;
import io.kubernetes.client.extended.leaderelection.LeaderElector;
import io.kubernetes.client.extended.leaderelection.resourcelock.EndpointsLock;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Indexer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Config;
import okhttp3.Call;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.UUID;

public class MySQLController {

    @Bean
    public static CommandLineRunner runInformer(){
        return args ->{
            ApiClient client = Config.defaultClient();
            client.setReadTimeout(10);

            Configuration.setDefaultApiClient(client);

            CoreV1Api coreV1Api = new CoreV1Api();

            SharedInformerFactory informerFactory = new SharedInformerFactory();

            SharedIndexInformer<V1Pod> podInformer = informerFactory.sharedIndexInformerFor(
                    (CallGeneratorParams callGeneratorParams) -> {
                            System.out.println("Resource Version: " + callGeneratorParams.resourceVersion);
                            System.out.println("Timeout: " + callGeneratorParams.timeoutSeconds);
                            System.out.println("Watch: " + callGeneratorParams.watch);

                            Call podCall = coreV1Api.listNamespacedPodCall("default",
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    callGeneratorParams.resourceVersion,
                                    callGeneratorParams.timeoutSeconds,
                                    callGeneratorParams.watch,
                                    null,
                                    null);
                        return podCall;
                    },
                    V1Pod.class,
                    V1PodList.class,
                    1000
            );

            PodPrintingReconciler reconciler = new PodPrintingReconciler(podInformer);

            Controller controller = ControllerBuilder.defaultBuilder(informerFactory)
                    .withName("MyControllerName")
                    .withReadyFunc(podInformer::hasSynced)
                    .watch(
                            (workQueue) -> {
                                return ControllerBuilder.controllerWatchBuilder(V1Pod.class,workQueue)
                                        .withWorkQueueKeyFunc(
                                                (V1Pod pod) -> new Request(pod.getMetadata().getUid())
                                        )
                                        .build();
                            }
                    )
                    .withReconciler(reconciler)
                    .withWorkerCount(4)
                    .build();

            ControllerManager manager = ControllerBuilder.controllerManagerBuilder(informerFactory)
                    .addController(controller)
                    .build();

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
        };
    }

    static class PodPrintingReconciler implements Reconciler {

        private Lister<V1Pod> podLister;

        public PodPrintingReconciler(SharedIndexInformer<V1Pod> informer){
            Indexer<V1Pod> indexer = informer.getIndexer();
            podLister = new Lister<>(indexer);
        }

        @Override
        public Result reconcile(Request request) {
            System.out.println(request.toString());
            return null;
        }
    }

}
