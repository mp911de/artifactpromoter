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
package io.spring.artifactpromoter.artifactpromoter;

import io.spring.artifactpromoter.artifactpromoter.artifactory.ArtifactoryBuild;
import io.spring.artifactpromoter.artifactpromoter.artifactory.ArtifactoryClient;
import io.spring.artifactpromoter.artifactpromoter.nexus.NexusClient;
import io.spring.artifactpromoter.artifactpromoter.nexus.StagingProfile;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * @author Mark Paluch
 */
public class PromotionOperations {

	private final File workingDirectory;
	private final ArtifactoryClient artifactory;

	private final NexusClient nexusClient;

	public PromotionOperations(File workingDirectory, ArtifactoryClient artifactory, NexusClient nexusClient) {

		this.workingDirectory = workingDirectory;
		this.artifactory = artifactory;
		this.nexusClient = nexusClient;
	}

	/**
	 * Promote an Artifactory build to a Nexus staging repository. Also creates PGP signatures for each artifact.
	 *
	 * @param buildName
	 * @param buildNumber
	 * @throws IOException
	 */
	public void promote(String buildName, int buildNumber) throws IOException {

		ArtifactoryBuild context = ArtifactoryBuild.of(buildName, buildNumber);
		FileUtils.deleteDirectory(WorkspaceUtils.getContextDirectory(workingDirectory, context));

		// TODO
		String keyId = "";
		String keyRing = "";
		char[] passPhrase = new char[0];
		boolean closeStagingRepository = true;

		// TODO
		boolean promoteRepository = false;

		artifactory.resolveModules(context, s -> !s.endsWith(".zip")).doOnNext(it -> {

			try {
				// blocking call with exceptions
				artifactory.prepareDirectories(it, context);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}).flatMap(modules -> {

			Mono<Void> download = artifactory.download(modules, context);

			// TODO Multiple keys/key rings?
			// This one is also blocking, requires own scheduler
			// PgpClient.createSignatures(workingDirectory, context, )
			GAVC id = modules.getModules().get(0).getId();
			Mono<StagingProfile> stagingProfile = nexusClient.selectStagingProfile(id);

			return stagingProfile
					.flatMap(profileId -> nexusClient.createStagingRepository(profileId, String.format("Promotion of %s", id)))
					.flatMap(repositoryId -> {

						Mono<Void> upload = nexusClient.upload(repositoryId, modules, context);

						if (closeStagingRepository) {
							return upload.then(nexusClient.closeStagingRepository(repositoryId));
						} else {
							return upload;
						}
					});

		}).block();
	}
}
