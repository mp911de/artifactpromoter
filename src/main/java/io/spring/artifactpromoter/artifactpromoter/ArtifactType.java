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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.EqualsAndHashCode;

/**
 * Value object representing an artifact type.
 *
 * @author Mark Paluch
 */
@EqualsAndHashCode
public abstract class ArtifactType {

	public static final ArtifactType JAVADOC_JAR = create("javadoc-jar");

	public static final ArtifactType SOURCES_JAR = create("sources-jar");

	public static final ArtifactType ORIGINAL_JAR = create("original-jar");

	public static final ArtifactType POM = create("pom");

	public static final ArtifactType JAR = create("jar");

	static Map<String, ArtifactType> types = Map
			.of(JAVADOC_JAR.getCanonicalName(), JAVADOC_JAR, //
					SOURCES_JAR.getCanonicalName(), SOURCES_JAR, //
					ORIGINAL_JAR.getCanonicalName(), ORIGINAL_JAR, //
					POM.getCanonicalName(), POM, //
					JAR.getCanonicalName(), JAR //
			);


	private final String canonicalName;

	private ArtifactType(String canonicalName) {
		this.canonicalName = canonicalName;
	}

	/**
	 * Return the canonical artifact type name.
	 * @return     the canonical artifact type name.
	 */
	public String getCanonicalName() {
		return canonicalName;
	}

	/**
	 * Test whether the {@link ArtifactType} matches the given {@code filename}.
	 * @param filename the filename to test.
	 * @return
	 */
	public boolean matches(String filename) {
		return doMatch(filename.toLowerCase(Locale.ROOT));
	}

	abstract boolean doMatch(String filename);

	/**
	 * Return a list of known classifiers that are special types of JARs.
	 * @return
	 */
	public static List<ArtifactType> knownClassifiers() {
		return Arrays.asList(JAVADOC_JAR, SOURCES_JAR, ORIGINAL_JAR);
	}

	/**
	 * Retrieve or create an {@link ArtifactType}.
	 * @param type
	 * @return
	 */
	public static ArtifactType of(String type) {

		ArtifactType artifactType = types.get(type);

		if (artifactType != null) {
			return artifactType;
		}


		return create(type);
	}

	private static ArtifactType create(String type) {

		return new ArtifactType(type) {

			@Override
			boolean doMatch(String filename) {

				if (type.contains("-")) {
					return filename.endsWith("-" + type.replace('-', '.'));
				}

				return filename.endsWith("." + type);
			}

		};
	}

	@Override
	public String toString() {
		return getCanonicalName();
	}
}
