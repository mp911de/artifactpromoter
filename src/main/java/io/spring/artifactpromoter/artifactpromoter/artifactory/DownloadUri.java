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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.spring.artifactpromoter.artifactpromoter.ArtifactType;
import io.spring.artifactpromoter.artifactpromoter.GAVC;
import lombok.Getter;

/**
 * @author Mark Paluch
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class DownloadUri {

	private final String downloadUri;

	public DownloadUri(@JsonProperty("downloadUri") String downloadUri) {
		this.downloadUri = downloadUri;
	}

	public boolean matches(GAVC gavc, ArtifactType artifactType) {

		if (!downloadUri.contains(gavc.toString(true, "/"))) {
			return false;
		}

		if (artifactType.equals(ArtifactType.JAR)) {

			if (ArtifactType.knownClassifiers().stream()
					.anyMatch(it -> it.matches(downloadUri))) {
				return false;
			}
		}

		return artifactType.matches(downloadUri);
	}

}
