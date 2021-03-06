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

import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a deployable artifact within the scope of a module. A build artifact maps typically to one or more files
 * (the actual file, MD5 file, SHA1 file, ASC signature).
 *
 * @author Mark Paluch
 */
@Value
public class Artifact {

	String name, sha1, md5, binaryDownloadUri;
	ArtifactType type;

	/**
	 * List of files that should be distributed including signatures and checksums.
	 *
	 * @return
	 */
	public List<String> getDistributionFileNames() {

		return Stream.of("", ".asc", ".md5", ".sha1") //
				.map(name::concat) //
				.collect(Collectors.toList());
	}

}
