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
import io.spring.artifactpromoter.artifactpromoter.ArtifactType;
import io.spring.artifactpromoter.artifactpromoter.GAVC;
import io.spring.artifactpromoter.artifactpromoter.Modules;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.jfrog.build.api.Build;

/**
 * Value object capturing raw {@link Build} and {@link DownloadUri} details.
 *
 * @author Mark Paluch
 */
@RequiredArgsConstructor
class DownloadableBuild {

	private final Build build;
	private final List<DownloadUri> downloadUris;

	/**
	 * Create a new {@link DownloadableBuild} from {@link Build} and {@link DownloadUri}s.
	 *
	 * @param build the build info
	 * @param downloadUris list of download URIs
	 */
	static DownloadableBuild from(Build build, List<DownloadUri> downloadUris) {
		return new DownloadableBuild(build, downloadUris);
	}

	/**
	 * Construct {@link Modules} from an Artifactory {@link Build build info} and {@code downloadUris}. Requires that each
	 * valid artifact maps to a download URI.
	 *
	 * @param build the build info
	 * @param downloadUris list of download URIs
	 * @param artifactFilter filter to exclude artifacts by name (where the name is e.g.
	 *          {@code reactor-netty-core-1.0.4-SNAPSHOT-original.jar}
	 * @return the built modules.
	 */
	Modules toModules(Predicate<String> artifactFilter) {

		List<io.spring.artifactpromoter.artifactpromoter.Module> modules = new ArrayList<>();

		for (org.jfrog.build.api.Module module : build.getModules()) {

			io.spring.artifactpromoter.artifactpromoter.Module builtModule = toModule(artifactFilter, module);

			modules.add(builtModule);
		}

		return Modules.of(modules);
	}

	private io.spring.artifactpromoter.artifactpromoter.Module toModule(Predicate<String> artifactFilter,
			org.jfrog.build.api.Module module) {

		List<Artifact> artifacts = new ArrayList<>();
		GAVC gavc = GAVC.of(module.getId());
		io.spring.artifactpromoter.artifactpromoter.Module builtModule = io.spring.artifactpromoter.artifactpromoter.Module
				.of(gavc, artifacts);

		for (org.jfrog.build.api.Artifact artifact : module.getArtifacts()) {

			if (!artifactFilter.test(artifact.getName())) {
				continue;
			}

			ArtifactType artifactType = ArtifactType.of(artifact.getType());
			String downloadUri = getRequiredDownloadUri(downloadUris, gavc, artifactType);

			Artifact builtArtifact = new Artifact(artifact.getName(), artifact.getSha1(), artifact.getMd5(), downloadUri,
					artifactType);

			artifacts.add(builtArtifact);
		}

		if (artifacts.isEmpty()) {
			throw new IllegalStateException(String.format("Empty module %s", gavc));
		}

		return builtModule;
	}

	private static String getRequiredDownloadUri(List<DownloadUri> downloadUris, GAVC gavc, ArtifactType artifactType) {

		for (DownloadUri downloadUri : downloadUris) {

			if (downloadUri.matches(gavc, artifactType)) {
				return downloadUri.getDownloadUri();
			}
		}

		throw new IllegalStateException(String.format("Cannot find Download URI for %s, type %s", gavc, artifactType));
	}
}
