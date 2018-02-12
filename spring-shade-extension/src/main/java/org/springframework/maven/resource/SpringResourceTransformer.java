/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.maven.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

/**
 * Maven Shade Plugin ResourceTransformer for Spring Metadata.
 * Merges properties in certain Spring metadata files.
 *
 * @author Gary Russell
 */
public class SpringResourceTransformer implements ResourceTransformer {

	private static final Set<String> META_INF_SPRING = new HashSet<>(Arrays.asList(
			"META-INF/spring.factories",
			"META-INF/spring.schemas",
			"META-INF/spring.handlers",
			"META-INF/spring.tooling"));

	private final Map<String, Map<String, String>> resources = new HashMap<>();

	@Override
	public boolean canTransformResource(String resource) {
		return META_INF_SPRING.contains(resource);
	}

	@Override
	public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
		Map<String, String> merged = this.resources.computeIfAbsent(resource, v -> new HashMap<>());
		Properties props = new Properties();
		props.load(is);
		props.forEach((k, v) -> {
			String value = merged.get(k);
			if (value == null) {
				merged.put((String) k, (String) v);
			}
			else {
				merged.put((String) k, value + "," + v);
			}
		});
	}

	@Override
	public boolean hasTransformedResource() {
		return this.resources.size() > 0;
	}

	@Override
	public void modifyOutputStream(JarOutputStream jos) throws IOException {
		for (Entry<String, Map<String, String>> entry : this.resources.entrySet()) {
			jos.putNextEntry(new JarEntry(entry.getKey()));
			PrintWriter pw = new PrintWriter(jos);
			for (Entry<String, String> prop : entry.getValue().entrySet()) {
				pw.write(prop.getKey() + "=");
				pw.write(prop.getValue().replaceAll(",", ",\\\\\n") + "\n");
			}
			pw.write("\n");
			pw.flush();
		}
	}

}
