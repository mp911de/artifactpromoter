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

import java.util.StringJoiner;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object capturing GroupId/ArtifactId/Version and an optional classifier.
 *
 * @author Mark Paluch
 */
@Value
public class GAVC {

	String groupId;
	String artifactId;
	String version;

	@Nullable String classifier;

	/**
	 * Create a {@link GAVC} from a composite string in the form of {@code groupId:artifactId:version[:classifier]}.
	 *
	 * @param gavc the string to parse.
	 * @return
	 */
	public static GAVC of(String gavc) {

		Assert.notNull(gavc, "GAV(C) must not be null");

		String[] split = gavc.split(":");

		if (split.length == 3) {
			return new GAVC(split[0], split[1], split[2], null);
		}

		return new GAVC(split[0], split[1], split[2], split[3]);
	}

	/**
	 * Render a composite representation where the individual components are separated by {@code delimiter}.
	 *
	 * @param delimiter
	 * @return
	 */
	public String toString(String delimiter) {
		return toString(false, delimiter);
	}

	/**
	 * Render a composite representation where the individual components are separated by {@code delimiter}.
	 *
	 * @param mavenRepositoryLayout {@code true} to use the Maven repository format that replaces dots in the
	 *          {@code groupId} with slashes.
	 * @param delimiter
	 * @return
	 */

	public String toString(boolean mavenRepositoryLayout, String delimiter) {

		StringJoiner joiner = new StringJoiner(delimiter);

		return joiner.add(mavenRepositoryLayout ? groupId.replace('.', '/') : groupId).add(artifactId).add(version)
				.toString();
	}

	@Override
	public String toString() {
		return toString(":");
	}
}
