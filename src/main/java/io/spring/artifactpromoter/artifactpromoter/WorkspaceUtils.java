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

import org.apache.commons.lang.SystemUtils;

/**
 * Utility methods to manage the workspace directory.
 *
 * @author Mark Paluch
 */
public class WorkspaceUtils {

	private WorkspaceUtils() {}

	public static String getSafeFileName(PromotionContext context) {
		return getSafeFileName(context.getName());
	}

	public static String getSafeFileName(String buildName) {

		StringBuilder builder = new StringBuilder(buildName.length());
		for (char c : buildName.toCharArray()) {
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
				builder.append(c);
			}
		}

		return builder.toString();
	}

	public static File getContextDirectory(File workingDirectory, PromotionContext context) {
		return new File(workingDirectory, WorkspaceUtils.getSafeFileName(context));
	}

	public static File getModuleDirectory(File buildDirectory, Module module) {

		GAVC gavc = module.getId();

		return new File(buildDirectory, gavc.toString(SystemUtils.FILE_SEPARATOR));
	}
}
