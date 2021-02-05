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
package io.spring.artifactpromoter.artifactpromoter.artifactory;

import io.spring.artifactpromoter.artifactpromoter.Artifact;
import io.spring.artifactpromoter.artifactpromoter.ArtifactPromoterProperties;
import io.spring.artifactpromoter.artifactpromoter.Module;
import io.spring.artifactpromoter.artifactpromoter.Modules;
import io.spring.artifactpromoter.artifactpromoter.PromotionContext;
import io.spring.artifactpromoter.artifactpromoter.WorkspaceUtils;
import lombok.extern.apachecommons.CommonsLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.Build;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Client for Artifactory.
 *
 * @author Mark Paluch
 */
@Component
@CommonsLog
public class ArtifactoryClient {

	private static final String BUILD_URL = "%s/api/build/{build}/{number}";
	private static final String BUILD_ARTIFACTS_URL = "%s/api/search/buildArtifacts";

	private final ArtifactPromoterProperties.Artifactory artifactoryProperties;
	private final File workingDirectory;
	private final WebClient webClient;

	public ArtifactoryClient(ArtifactPromoterProperties properties) {

		this.artifactoryProperties = properties.getArtifactory();
		this.workingDirectory = properties.getWorkingDirectory();

		ExchangeFilterFunction exchangeFilterFunction = ExchangeFilterFunctions
				.basicAuthentication(artifactoryProperties.getUsername(), artifactoryProperties.getPassword());

		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();
		decoder.setMaxInMemorySize(1024 * 1024 * 4 /* 4mb */);
		ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(clientCodecConfigurer -> {
			clientCodecConfigurer.customCodecs().register(decoder);
		}).build();

		this.webClient = WebClient.builder().filter(exchangeFilterFunction).exchangeStrategies(strategies).build();
	}

	/**
	 * Resolve {@link Modules} from a {@link ArtifactoryBuild}. Determines which modules and artifacts were deployed as
	 * part of the build.
	 *
	 * @param build
	 * @param number
	 * @param artifactFilter
	 * @return
	 */
	public Mono<Modules> resolveModules(ArtifactoryBuild build, Predicate<String> artifactFilter) {

		Mono<Build> buildInfo = this.webClient.get()
				.uri(String.format(BUILD_URL, artifactoryProperties.getAddress()), build.getBuildName(), build.getBuildNumber())
				.retrieve().bodyToMono(BuildInfoRepresentation.class).onErrorMap(WebClientResponseException.class, e -> {

					return new IllegalStateException(e.getMessage() + ": " + e.getResponseBodyAsString(), e);
				}).map(BuildInfoRepresentation::getBuildInfo);

		BuildArtifactsRequest buildArtifactsRequest = new BuildArtifactsRequest(build.getBuildName(),
				build.getBuildNumber());

		Mono<List<DownloadUri>> downloadUris = webClient.post()
				.uri(String.format(BUILD_ARTIFACTS_URL, artifactoryProperties.getAddress())) //
				.bodyValue(buildArtifactsRequest) //
				.retrieve() //
				.bodyToMono(BuildArtifactsResponse.class) //
				.map(BuildArtifactsResponse::getResults);

		return buildInfo.zipWith(downloadUris)
				.map(it -> DownloadableBuild.from(it.getT1(), it.getT2()).toModules(artifactFilter));
	}

	/**
	 * This method creates the build and module directories synchronously if they do not already exist.
	 *
	 * @param modules
	 * @param context
	 * @throws IOException
	 */
	public void prepareDirectories(Modules modules, PromotionContext context) throws IOException {

		File buildDirectory = new File(workingDirectory, WorkspaceUtils.getSafeFileName(context));
		FileUtils.forceMkdir(buildDirectory);

		Map<Module, File> directories = getDirectories(modules, buildDirectory);
		for (File moduleDirectory : directories.values()) {
			FileUtils.forceMkdir(moduleDirectory);
		}
	}

	/**
	 * Download {@link Modules} including their artifacts and checksum files into the configured workspace directory.
	 * Module directories must exist. See {@link #prepareDirectories(Modules, ArtifactoryBuild)}.
	 *
	 * @param modules
	 * @param build
	 * @return
	 */
	public Mono<Void> download(Modules modules, ArtifactoryBuild build) {

		File buildDirectory = new File(workingDirectory, WorkspaceUtils.getSafeFileName(build));
		Map<io.spring.artifactpromoter.artifactpromoter.Module, File> directories = getDirectories(modules, buildDirectory);

		return Flux.fromIterable(modules.getModules()).flatMap(it -> {

			File moduleDirectory = directories.get(it);
			log.info(String.format("Downloading Module %s to  %s...", it.getId(), moduleDirectory));

			return Flux.fromIterable(it.getArtifacts()).flatMap(artifact -> {

				Mono<Void> file = download(moduleDirectory, artifact.getBinaryDownloadUri(), artifact.getName());
				Mono<Void> md5 = download(moduleDirectory, artifact.getBinaryDownloadUri() + ".md5",
						artifact.getName() + ".md5");
				Mono<Void> sha1 = download(moduleDirectory, artifact.getBinaryDownloadUri() + ".sha1",
						artifact.getName() + ".sha1");

				return Mono.when(file, md5, sha1);
			}).then().doOnSuccess(v -> {
				log.info(String.format("Download of %s complete", it.getId()));
			});
		}).then();
	}

	private Map<io.spring.artifactpromoter.artifactpromoter.Module, File> getDirectories(Modules modules,
			File buildDirectory) {

		Map<io.spring.artifactpromoter.artifactpromoter.Module, File> directories = new HashMap<>();

		for (io.spring.artifactpromoter.artifactpromoter.Module module : modules.getModules()) {

			File moduleDirectory = WorkspaceUtils.getModuleDirectory(buildDirectory, module);
			directories.put(module, moduleDirectory);
		}

		return directories;
	}

	/**
	 * Verify checksums of the {@link Modules} against the checksums reported by the repository and checksum files.
	 * Computes MD5 and SHA1 checksums.
	 *
	 * @param modules
	 * @param build
	 * @throws IOException
	 */
	public void verifyChecksums(Modules modules, PromotionContext build) throws IOException {

		File buildDirectory = new File(workingDirectory, WorkspaceUtils.getSafeFileName(build));

		for (io.spring.artifactpromoter.artifactpromoter.Module module : modules.getModules()) {

			File moduleDirectory = WorkspaceUtils.getModuleDirectory(buildDirectory, module);
			log.info(String.format("Verifying checksums for module %s to  %s...", module.getId(), moduleDirectory));

			for (Artifact artifact : module.getArtifacts()) {

				String sha1File = readChecksumFile(moduleDirectory, artifact, "sha1");
				String md5File = readChecksumFile(moduleDirectory, artifact, "md5");

				String computedSha1 = computeSha1(moduleDirectory, artifact);
				String computedMd5 = computeMd5(moduleDirectory, artifact);

				if (!verify(artifact.getSha1(), sha1File, computedSha1)) {
					throw new IllegalStateException("SHA1 checksum verification failed for " + artifact.getName());
				}

				if (!verify(artifact.getMd5(), md5File, computedMd5)) {
					throw new IllegalStateException("MD5 checksum verification failed for " + artifact.getName());
				}
			}

			log.info(String.format("Checksum verification of %s completed successfully", module.getId()));
		}
	}

	private boolean verify(String reportedByRepository, String checksumFile, String computed) {

		if (!reportedByRepository.equals(computed)) {
			return false;
		}

		if (!checksumFile.contains(computed)) {
			return false;
		}

		return true;
	}

	private Mono<Void> download(File downloadDirectory, String uri, String name) {

		Flux<DataBuffer> buffers = webClient.get().uri(uri).retrieve().bodyToFlux(DataBuffer.class);
		File localFileName = new File(downloadDirectory, name);

		return DataBufferUtils.write(buffers, localFileName.toPath());
	}

	private static String computeSha1(File moduleDirectory, Artifact artifact) throws IOException {
		try (InputStream inputStream = getArtifactInputStream(moduleDirectory, artifact)) {
			return org.apache.commons.codec.digest.DigestUtils.sha1Hex(inputStream);
		}
	}

	private static String computeMd5(File moduleDirectory, Artifact artifact) throws IOException {

		try (InputStream inputStream = getArtifactInputStream(moduleDirectory, artifact)) {
			return org.apache.commons.codec.digest.DigestUtils.md5Hex(inputStream);
		}
	}

	private static InputStream getArtifactInputStream(File moduleDirectory, Artifact artifact)
			throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(new File(moduleDirectory, artifact.getName())));
	}

	private static String readChecksumFile(File moduleDirectory, Artifact artifact, String type) throws IOException {
		return FileUtils.readFileToString(new File(moduleDirectory, artifact.getName() + "." + type),
				StandardCharsets.US_ASCII);
	}

}
