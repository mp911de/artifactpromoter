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

/**
 * Value object to capture multiple modules. A module can contain multiple artifacts (files) that map to one or more
 * files.
 *
 * @author Mark Paluch
 */
@Value(staticConstructor = "of")
public class Modules {

	List<Module> modules;

}
