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
package io.spring.artifactpromoter.artifactpromoter.nexus;

/**
 * Exception thrown during staging operations.
 *
 * @author Mark Paluch
 */
public class StagingException extends RuntimeException {

	public StagingException() {
		super();
	}

	public StagingException(String message) {
		super(message);
	}

	public StagingException(String message, Throwable cause) {
		super(message, cause);
	}

	public StagingException(Throwable cause) {
		super(cause);
	}

	protected StagingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
