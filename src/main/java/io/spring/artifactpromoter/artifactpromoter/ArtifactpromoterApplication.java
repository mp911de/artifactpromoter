package io.spring.artifactpromoter.artifactpromoter;

import java.io.File;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ArtifactPromoterProperties.class)
@RequiredArgsConstructor
public class ArtifactpromoterApplication {

	@Autowired
	ArtifactPromoterProperties properties;

	public static void main(String[] args) {
		SpringApplication.run(ArtifactpromoterApplication.class, args);
	}

	@PostConstruct
	private void postConstruct() {

		File workingDirectory = properties.getWorkingDirectory();

		if (!workingDirectory.exists()) {
			workingDirectory.mkdirs();
		}

	}

}
