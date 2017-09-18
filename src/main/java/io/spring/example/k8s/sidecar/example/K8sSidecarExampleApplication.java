package io.spring.example.k8s.sidecar.example;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAppDeployer;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.MainContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.SidecarContainerFactory;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class K8sSidecarExampleApplication implements CommandLineRunner {
	private static Logger log = LoggerFactory.getLogger(K8sSidecarExampleApplication.class);
	private static int MAX_TRIES = 60;
	private static long SLEEP_TIME_MS = 10000;

	@Autowired
	private KubernetesClient kubernetesClient;

	public static void main(String[] args) {
		SpringApplication.run(K8sSidecarExampleApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {
		KubernetesDeployerProperties deployProperties = new KubernetesDeployerProperties();
		deployProperties.setCreateDeployment(true);
		deployProperties.setCreateLoadBalancer(true);
		deployProperties.setMinutesToWaitForLoadBalancer(1);
		deployProperties.setImagePullPolicy(ImagePullPolicy.Always);
		MainContainerFactory containerFactory = new MainContainerFactory(deployProperties);
		SidecarContainerFactory sidecarContainerFactory = new SidecarContainerFactory();
		KubernetesAppDeployer appDeployer = new KubernetesAppDeployer(deployProperties, kubernetesClient,
			containerFactory, sidecarContainerFactory);

		log.info("Testing {}...", "DeploymentWithSideCar");
		Resource resource = new DockerResource("dturanski/sentiment-demo:latest");
		Map<String, String> props = new HashMap<>();
		//Either this
		props.put("spring.cloud.deployer.kubernetes.sidecars", "{sentiment-analyzer :{image: "
			+ "'dturanski/sentiment-analyzer:latest', ports: [9998]}}");

		// or
//		props.put("spring.cloud.deployer.kubernetes.sidecars",
//			"{sentiment-analyzer :{image: 'dturanski/sentiment-analyzer:latest', livenessProbe: {type: socket,"
//				+ "port: 9998}}}");
		AppDefinition definition = new AppDefinition(appName(), new HashMap<>());

		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, props);
		log.info("Deploying {}...", request.getDefinition().getName());
		String deploymentId = appDeployer.deploy(request);


		AppStatus status = appDeployer.status(definition.getName());
		int tries = 0;
		while(status.getState() != DeploymentState.deployed && tries++ <= MAX_TRIES) {
			log.info("Deployment status is {}", status.getState());
			Thread.sleep(SLEEP_TIME_MS);
		}

		if (status.getState() == DeploymentState.deployed) {
			log.info("Deployed {}...", deploymentId);
		}
		else {
			log.error("Deployment {} failed. Status {}", deploymentId, status.getState());
		}

	}

	private String appName() {
		// Kubernetest service names must start with a letter and can only be 24 characters long
		return "sidecar-main";
	}
}
