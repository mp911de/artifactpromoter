/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.artifactpromoter.artifactpromoter.nexus;

import io.spring.artifactpromoter.artifactpromoter.Artifact;
import io.spring.artifactpromoter.artifactpromoter.ArtifactPromoterProperties;
import io.spring.artifactpromoter.artifactpromoter.GAVC;
import io.spring.artifactpromoter.artifactpromoter.Modules;
import io.spring.artifactpromoter.artifactpromoter.PromotionContext;
import io.spring.artifactpromoter.artifactpromoter.WorkspaceUtils;
import lombok.extern.apachecommons.CommonsLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Client for Sonatype Nexus 2.
 *
 * @author Mark Paluch
 */
@CommonsLog
public class NexusClient {

	private static final String SELECT_STAGING_PROFILE = "%s/service/local/staging/profile_evaluate?a={artifactId}&t=maven2&g={groupId}&v={version}";
	private static final String STAGING_START = "%s/service/local/staging/profiles/{profileId}/start";
	private static final String STAGING_CLOSE = "%s/service/local/staging/profiles/{profileId}/finish";

	private static final String DEPLOY_BY_REPOSITORY = "%s/service/local/staging/deployByRepositoryId/{repositoryId}/%s/%s";

	private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
	private final ArtifactPromoterProperties.Nexus nexusProperties;
	private final File workingDirectory;
	private final WebClient webClient;

	public NexusClient(ArtifactPromoterProperties properties) {

		this.nexusProperties = properties.getNexus();
		this.workingDirectory = properties.getWorkingDirectory();

		ExchangeFilterFunction exchangeFilterFunction = ExchangeFilterFunctions
				.basicAuthentication(nexusProperties.getUsername(), nexusProperties.getPassword());

		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
		decoder.setMaxInMemorySize(1024 * 1024 * 4 /* 4mb */);
		ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(clientCodecConfigurer -> {
			clientCodecConfigurer.customCodecs().register(decoder);
		}).build();

		this.webClient = WebClient.builder().filter(exchangeFilterFunction).exchangeStrategies(strategies).build();
	}

	/**
	 * Select the {@link StagingProfile} to use for the staging operation.
	 *
	 * @param gavc
	 * @return
	 */
	public Mono<StagingProfile> selectStagingProfile(GAVC gavc) {
		return selectStagingProfile(gavc.getGroupId(), gavc.getArtifactId(), gavc.getArtifactId());
	}

	/**
	 * Select the {@link StagingProfile} to use for the staging operation.
	 *
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return
	 */
	public Mono<StagingProfile> selectStagingProfile(String groupId, String artifactId, String version) {

		Map<String, String> parameters = Map.of("artifactId", artifactId, "groupId", groupId, "version", version);

		return webClient.get().uri(String.format(SELECT_STAGING_PROFILE, nexusProperties.getAddress()), parameters)//
				.accept(MediaType.APPLICATION_JSON).retrieve() //
				.bodyToMono(StagingProfiles.class) //
				.map(it -> {

					if (it.getData().isEmpty()) {
						throw new StagingException("Cannot resolve staging profile for " + parameters);
					}

					return StagingProfile.of(it.getData().get(0).getId());
				});
	}

	/**
	 * Start the staging process by creating a {@link StagingRepository}.
	 *
	 * @param profileId
	 * @param description
	 * @return
	 */
	public Mono<StagingRepository> createStagingRepository(StagingProfile profileId, String description) {

		Map<String, String> parameters = Collections.singletonMap("profileId", profileId.getProfileId());

		return webClient.post().uri(String.format(STAGING_START, nexusProperties.getAddress()), parameters)
				.accept(MediaType.APPLICATION_JSON) //
				.bodyValue(new StagingRequest(new Staging(null, null, description))) //
				.retrieve() //
				.bodyToMono(StagingRequest.class) //
				.map(it -> {
					return StagingRepository.of(it.getData().getStagedRepositoryId());
				}).onErrorMap(WebClientResponseException.class,
						e -> new StagingException(String.format("Cannot create staging repository for %s: %s",
								profileId.getProfileId(), e.getResponseBodyAsString()), e));
	}

	/**
	 * Upload a {@link io.spring.artifactpromoter.artifactpromoter.Module} to the {@link StagingRepository}.
	 *
	 * @param stagingRepository
	 * @param modules
	 * @param context
	 * @return
	 */
	public Mono<Void> upload(StagingRepository stagingRepository, Modules modules, PromotionContext context) {

		File buildDirectory = WorkspaceUtils.getContextDirectory(workingDirectory, context);

		Map<String, String> parameters = Collections.singletonMap("repositoryId", stagingRepository.getRepositoryId());

		return Flux.fromIterable(modules.getModules()).flatMap(module -> {

			GAVC gavc = module.getId();
			File moduleDirectory = WorkspaceUtils.getModuleDirectory(buildDirectory, module);

			return Flux.fromIterable(module.getArtifacts()).flatMap(artifact -> {

				Map<String, File> filesToUpload = getFilesToUpload(moduleDirectory, artifact);

				log.info(String.format("Uploading %s ...", artifact.getName()));

				Flux<Void> uploads = Flux.fromIterable(filesToUpload.entrySet())
						.flatMap(it -> uploadFile(parameters, gavc, it.getKey(), it.getValue()));

				return uploads.then()
						.onErrorMap(WebClientResponseException.class,
								e -> new StagingException(
										String.format("Cannot upload %s: %s", artifact.getName(), e.getResponseBodyAsString()), e))
						.doOnSuccess(it -> log.info(String.format("Upload %s done", artifact.getName())));
			});

		}).then();
	}

	/**
	 * Finish the staging process by closing a {@link StagingRepository}.
	 *
	 * @param stagingRepository
	 * @return
	 */
	public Mono<Void> closeStagingRepository(StagingRepository stagingRepository) {

		Map<String, String> parameters = Collections.singletonMap("repositoryId", stagingRepository.getRepositoryId());

		return webClient.post().uri(String.format(STAGING_CLOSE, nexusProperties.getAddress()), parameters)
				.accept(MediaType.APPLICATION_JSON) //
				.bodyValue(new StagingRequest(new Staging(stagingRepository.getRepositoryId(), null, null))) //
				.retrieve() //
				.bodyToMono(String.class) //
				.onErrorMap(WebClientResponseException.class, e -> new StagingException("Cannot close staging repository "
						+ stagingRepository.getRepositoryId() + ": " + e.getResponseBodyAsString(), e))
				.then();
	}

	private Mono<Void> uploadFile(Map<String, String> parameters, GAVC gavc, String filename, File file) {

		String uri = String.format(DEPLOY_BY_REPOSITORY, nexusProperties.getAddress(), gavc.toString(true, "/"), filename);

		Flux<DataBuffer> uploadStream = DataBufferUtils.read(file.toPath(), dataBufferFactory, 256 * 1000);

		return webClient.put().uri(uri, parameters) //
				.header(HttpHeaders.PRAGMA, "no-cache") //
				.header(HttpHeaders.CACHE_CONTROL, "no-cache") //
				.header(HttpHeaders.CONTENT_LENGTH, "" + file.length()) //
				.contentType(MediaType.APPLICATION_OCTET_STREAM) //
				.body(uploadStream, DataBuffer.class) //
				.retrieve() //
				.bodyToMono(String.class) //
				.then();
	}

	private static Map<String, File> getFilesToUpload(File moduleDirectory, Artifact artifact) {

		Map<String, File> filesToUpload = new LinkedHashMap<>();

		for (String filename : artifact.getDistributionFileNames()) {

			File file = new File(moduleDirectory, filename);

			if (!file.exists()) {
				throw new IllegalStateException(String.format("File %s does not exist", filename));
			}

			filesToUpload.put(filename, file);
		}

		return filesToUpload;
	}

}
