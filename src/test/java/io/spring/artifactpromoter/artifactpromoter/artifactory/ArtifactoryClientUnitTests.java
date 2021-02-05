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

import static org.assertj.core.api.Assertions.*;

import io.spring.artifactpromoter.artifactpromoter.GAVC;
import io.spring.artifactpromoter.artifactpromoter.Module;
import io.spring.artifactpromoter.artifactpromoter.Modules;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jfrog.build.api.Build;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for {@link ArtifactoryClient}.
 *
 * @author Mark Paluch
 */
class ArtifactoryClientUnitTests {


	String[] downloadURIs = new String[] {
			"/io/projectreactor/netty/reactor-netty/1.0.4-SNAPSHOT/reactor-netty-1.0.4-20210205.081845-33-docs.zip",
			"/io/projectreactor/netty/reactor-netty/1.0.4-SNAPSHOT/reactor-netty-1.0.4-20210205.081845-33-sources.jar",
			"/io/projectreactor/netty/reactor-netty-core/1.0.4-SNAPSHOT/reactor-netty-core-1.0.4-20210205.081845-33-original.jar",
			"/io/projectreactor/netty/reactor-netty/1.0.4-SNAPSHOT/reactor-netty-1.0.4-20210205.081845-33-javadoc.jar",
			"/io/projectreactor/netty/reactor-netty-http-brave/1.0.4-SNAPSHOT/reactor-netty-http-brave-1.0.4-20210205.081845-33.jar",
			"/io/projectreactor/netty/reactor-netty-http/1.0.4-SNAPSHOT/reactor-netty-http-1.0.4-20210205.081845-33.jar",
			"/io/projectreactor/netty/reactor-netty-http-brave/1.0.4-SNAPSHOT/reactor-netty-http-brave-1.0.4-20210205.081845-33-sources.jar",
			"/io/projectreactor/netty/reactor-netty-core/1.0.4-SNAPSHOT/reactor-netty-core-1.0.4-20210205.081845-33.pom",
			"/io/projectreactor/netty/reactor-netty-core/1.0.4-SNAPSHOT/reactor-netty-core-1.0.4-20210205.081845-33-sources.jar",
			"/io/projectreactor/netty/reactor-netty/1.0.4-SNAPSHOT/reactor-netty-1.0.4-20210205.081845-33.pom",
			"/io/projectreactor/netty/reactor-netty-http-brave/1.0.4-SNAPSHOT/reactor-netty-http-brave-1.0.4-20210205.081845-33.pom",
			"/io/projectreactor/netty/reactor-netty-http/1.0.4-SNAPSHOT/reactor-netty-http-1.0.4-20210205.081845-33-sources.jar",
			"/io/projectreactor/netty/reactor-netty-http/1.0.4-SNAPSHOT/reactor-netty-http-1.0.4-20210205.081845-33-javadoc.jar",
			"/io/projectreactor/netty/reactor-netty-core/1.0.4-SNAPSHOT/reactor-netty-core-1.0.4-20210205.081845-33-javadoc.jar",
			"/io/projectreactor/netty/reactor-netty/1.0.4-SNAPSHOT/reactor-netty-1.0.4-20210205.081845-33.jar",
			"/io/projectreactor/netty/reactor-netty-http/1.0.4-SNAPSHOT/reactor-netty-http-1.0.4-20210205.081845-33.pom",
			"/io/projectreactor/netty/reactor-netty-http-brave/1.0.4-SNAPSHOT/reactor-netty-http-brave-1.0.4-20210205.081845-33-javadoc.jar",
			"/io/projectreactor/netty/reactor-netty-core/1.0.4-SNAPSHOT/reactor-netty-core-1.0.4-20210205.081845-33.jar"
	};

	String buildInfoJson = "{\n"
			+ "  \"buildInfo\": {\n"
			+ "    \"version\": \"1.0.1\",\n"
			+ "    \"name\": \"Project Reactor - Reactor Netty - Netty\",\n"
			+ "    \"number\": \"2409\",\n"
			+ "    \"type\": \"GRADLE\",\n"
			+ "    \"buildAgent\": {\n"
			+ "      \"name\": \"Gradle\",\n"
			+ "      \"version\": \"6.6.1\"\n"
			+ "    },\n"
			+ "    \"agent\": {\n"
			+ "      \"name\": \"Bamboo\",\n"
			+ "      \"version\": \"6.10.4-build-61009\"\n"
			+ "    },\n"
			+ "    \"started\": \"2021-02-05T08:19:12.084+0000\",\n"
			+ "    \"durationMillis\": 497730,\n"
			+ "    \"principal\": \"auto\",\n"
			+ "    \"artifactoryPrincipal\": \"buildmaster\",\n"
			+ "    \"artifactoryPluginVersion\": \"3.0.1\",\n"
			+ "    \"url\": \"https://build.spring.io/browse/REACTOR-RNETTY-NETTY-2409\",\n"
			+ "    \"vcs\": [\n"
			+ "      {\n"
			+ "        \"revision\": \"d222ebffa516f6377f5c1f7f9006c159467e3b81\",\n"
			+ "        \"url\": \"https://github.com/reactor/reactor-netty.git\",\n"
			+ "        \"empty\": false\n"
			+ "      }\n"
			+ "    ],\n"
			+ "    \"vcsRevision\": \"d222ebffa516f6377f5c1f7f9006c159467e3b81\",\n"
			+ "    \"vcsUrl\": \"https://github.com/reactor/reactor-netty.git\",\n"
			+ "    \"licenseControl\": {\n"
			+ "      \"runChecks\": false,\n"
			+ "      \"includePublishedArtifacts\": false,\n"
			+ "      \"autoDiscover\": false,\n"
			+ "      \"licenseViolationsRecipientsList\": \"\",\n"
			+ "      \"scopesList\": \"\"\n"
			+ "    },\n"
			+ "    \"modules\": [\n"
			+ "      {\n"
			+ "        \"id\": \"io.projectreactor.netty:reactor-netty-core:1.0.4-SNAPSHOT\",\n"
			+ "        \"artifacts\": [\n"
			+ "          {\n"
			+ "            \"type\": \"javadoc-jar\",\n"
			+ "            \"sha1\": \"9395c3ebb4da770888a66b65460c889a27978880\",\n"
			+ "            \"sha256\": \"76c7f4b9344ef3f85aa2cb51b42b9d485486be71c637a121f3c1d15c609a198b\",\n"
			+ "            \"md5\": \"9259598dcfad971a1bd77ff9bc90c090\",\n"
			+ "            \"name\": \"reactor-netty-core-1.0.4-SNAPSHOT-javadoc.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"original-jar\",\n"
			+ "            \"sha1\": \"28432b8ff1795698624527fe480580ad5bb0704f\",\n"
			+ "            \"sha256\": \"ba2a580b21c93104343240ff732dab89bb112e0050fd0b425bd3de9654a0dd7f\",\n"
			+ "            \"md5\": \"879ebfab917874e048e60796aea661a2\",\n"
			+ "            \"name\": \"reactor-netty-core-1.0.4-SNAPSHOT-original.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"sources-jar\",\n"
			+ "            \"sha1\": \"6e8aec04b3b3f0967ca5f8aa633162f8df0ad965\",\n"
			+ "            \"sha256\": \"b8e8d7544d315a3847761026729b39981721455ecf9939a1ab57fa3deeec5807\",\n"
			+ "            \"md5\": \"1098c421a9a6abc43ae52ecc27019e1c\",\n"
			+ "            \"name\": \"reactor-netty-core-1.0.4-SNAPSHOT-sources.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"jar\",\n"
			+ "            \"sha1\": \"2e4a55a9c266f7c3dbd5d0d8d621a59d2b637342\",\n"
			+ "            \"sha256\": \"9e1e6ec43303bce53bd325187b20f1f49bcebbca3826ebe6f0293a96c18957b0\",\n"
			+ "            \"md5\": \"d864b45281ed2f5c9aa11bad9d82573e\",\n"
			+ "            \"name\": \"reactor-netty-core-1.0.4-SNAPSHOT.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"pom\",\n"
			+ "            \"sha1\": \"a49757b73ade13106559bd8d3e3b840e95348880\",\n"
			+ "            \"sha256\": \"343592fdb4417e065ef9529a5aae2ef3c14cb3fa04701934b6d1cb0d8b5be8c1\",\n"
			+ "            \"md5\": \"b8756d373b0a7f070936917cf8847d84\",\n"
			+ "            \"name\": \"reactor-netty-core-1.0.4-SNAPSHOT.pom\"\n"
			+ "          }\n"
			+ "        ],\n"
			+ "        \"excludedArtifacts\": [\n"
			+ "\n"
			+ "        ]\n"
			+ "      },\n"
			+ "      {\n"
			+ "        \"id\": \"io.projectreactor.netty:reactor-netty-http-brave:1.0.4-SNAPSHOT\",\n"
			+ "        \"artifacts\": [\n"
			+ "          {\n"
			+ "            \"type\": \"javadoc-jar\",\n"
			+ "            \"sha1\": \"b2237baaa691e79706eb860c482af4a2f76e161e\",\n"
			+ "            \"sha256\": \"13e95b4b5bed04b28b12b55c0d498f7afe9e53eab6f37c3f8da36443a9fa0f6b\",\n"
			+ "            \"md5\": \"1009c42dcdacdbe3ac6f0ee21d3888ca\",\n"
			+ "            \"name\": \"reactor-netty-http-brave-1.0.4-SNAPSHOT-javadoc.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"sources-jar\",\n"
			+ "            \"sha1\": \"55243e9c9405a44e263450679ba1ff874a2c4a57\",\n"
			+ "            \"sha256\": \"0902f9296bb19a6465e051dddc099674680dbdc6da579687fdc2575de6c6cedb\",\n"
			+ "            \"md5\": \"2baedbe96e5cc6d89459c947232507fc\",\n"
			+ "            \"name\": \"reactor-netty-http-brave-1.0.4-SNAPSHOT-sources.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"jar\",\n"
			+ "            \"sha1\": \"ae52493ce37315c16be03f08a6ca0f9e8028b895\",\n"
			+ "            \"sha256\": \"6b42ae7c121b4f89777f92f9888801092fb01194b6846f6f06ffa3c65b8b058c\",\n"
			+ "            \"md5\": \"b3942c6c40ae7a574fb7e776b80c7ee5\",\n"
			+ "            \"name\": \"reactor-netty-http-brave-1.0.4-SNAPSHOT.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"pom\",\n"
			+ "            \"sha1\": \"ab09376fe44eede461152253328a69ff0f40404c\",\n"
			+ "            \"sha256\": \"82fa67c3a8a8ec4c0773f4d4a96b5ff7bb23efb70b1626dfe014e29139fc1cb3\",\n"
			+ "            \"md5\": \"12fc8bf5960a0b886abf752b907de5e2\",\n"
			+ "            \"name\": \"reactor-netty-http-brave-1.0.4-SNAPSHOT.pom\"\n"
			+ "          }\n"
			+ "        ],\n"
			+ "        \"excludedArtifacts\": [\n"
			+ "\n"
			+ "        ]\n"
			+ "      },\n"
			+ "      {\n"
			+ "        \"id\": \"io.projectreactor.netty:reactor-netty-http:1.0.4-SNAPSHOT\",\n"
			+ "        \"artifacts\": [\n"
			+ "          {\n"
			+ "            \"type\": \"javadoc-jar\",\n"
			+ "            \"sha1\": \"c7edb36a14a4e1192a1303f67be601efa65d8a7e\",\n"
			+ "            \"sha256\": \"7faf2e7895e6c63f0a1b6148c17191bb886403b7b3dca2ab93504d5d0d782c37\",\n"
			+ "            \"md5\": \"a67d8932fb782485a032dbd4c890eec2\",\n"
			+ "            \"name\": \"reactor-netty-http-1.0.4-SNAPSHOT-javadoc.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"sources-jar\",\n"
			+ "            \"sha1\": \"babcb4d0ffdb420e59aea59c9a8e084134658f5a\",\n"
			+ "            \"sha256\": \"097dba1a49339020fcd8b4eddfc177808320616f767b927c98ba08a7dca5b1f6\",\n"
			+ "            \"md5\": \"175567caf20c7705947abad7fb394488\",\n"
			+ "            \"name\": \"reactor-netty-http-1.0.4-SNAPSHOT-sources.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"jar\",\n"
			+ "            \"sha1\": \"fe0e50c74c78f92ff3cd686a5ac1c158d03c54fd\",\n"
			+ "            \"sha256\": \"3fa479f131f67946ebb2decb6d2d6905521b915e295375c15c7f5471da6fc28a\",\n"
			+ "            \"md5\": \"11804017e6f168b3401f80224a7ce28f\",\n"
			+ "            \"name\": \"reactor-netty-http-1.0.4-SNAPSHOT.jar\"\n"
			+ "          },\n"
			+ "          {\n"
			+ "            \"type\": \"pom\",\n"
			+ "            \"sha1\": \"9aea59cc9a39169645405ab70ed469bf9fa2f243\",\n"
			+ "            \"sha256\": \"92148b24733a99326615b07b2b246efb1bf8146723d80514285e71d76b2fb70d\",\n"
			+ "            \"md5\": \"9fa62c531e57d053dd623ca9f7a34057\",\n"
			+ "            \"name\": \"reactor-netty-http-1.0.4-SNAPSHOT.pom\"\n"
			+ "          }\n"
			+ "        ],\n"
			+ "        \"excludedArtifacts\": [\n"
			+ "\n"
			+ "        ]\n"
			+ "      }\n"
			+ "    ],\n"
			+ "    \"governance\": {\n"
			+ "      \"blackDuckProperties\": {\n"
			+ "        \"runChecks\": false,\n"
			+ "        \"includePublishedArtifacts\": false,\n"
			+ "        \"autoCreateMissingComponentRequests\": false,\n"
			+ "        \"autoDiscardStaleComponentRequests\": false\n"
			+ "      }\n"
			+ "    }\n"
			+ "  }\n"
			+ "}";


	@Test
	void shouldCreateModules() throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		Build buildInfo = mapper.readValue(buildInfoJson, BuildInfoRepresentation.class).getBuildInfo();

		List<DownloadUri> downloadUris = Arrays.stream(downloadURIs).map(DownloadUri::new).collect(Collectors.toList());

		Modules modules = DownloadableBuild.from(buildInfo, downloadUris).toModules(s -> !s.contains("-original.jar"));

		assertThat(modules.getModules()).hasSize(3);

		Module module = modules.getModules().get(0);
		assertThat(module.getId()).isEqualTo(GAVC.of("io.projectreactor.netty:reactor-netty-core:1.0.4-SNAPSHOT"));
		assertThat(module.getArtifacts()).hasSize(4);
	}
}
