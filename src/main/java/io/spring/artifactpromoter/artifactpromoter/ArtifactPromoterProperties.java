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

import java.io.File;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Mark Paluch
 */
@ConfigurationProperties(prefix = "artifactpromoter")
@Data
public class ArtifactPromoterProperties {

	private File workingDirectory;

	private final Artifactory artifactory = new Artifactory();

	private final Nexus nexus = new Nexus();

	private final Pgp pgp = new Pgp();

	@Data
	public static class Artifactory{
		private String username, password;
		private String address;
	}

	@Data
	public static class Nexus {
		private String address = "https://oss.sonatype.org/";
		private String username, password;
	}


	@Data
	public static class Pgp{
		private String key, passphrase;
		private File keyring;
	}
}
