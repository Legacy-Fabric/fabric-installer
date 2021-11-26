/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.installer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoaderVersionHandler extends CompletableHandler<List<String>> {

	private final String metaUrl;
	private List<String> versions;

	public LoaderVersionHandler(String url) {
		this.metaUrl = url;
	}

	public void load() throws IOException {
		URL url = new URL(metaUrl);
		URLConnection conn = url.openConnection();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			boolean[] started = {false};
			versions = new ArrayList<>();
			reader.lines().forEach(line -> {
				String trimmed = line.trim();
				if (trimmed.equals("<versions>")) started[0] = true;
				else if (trimmed.equals("</versions>")) started[0] = false;
				else if (started[0]) {
					if (!trimmed.startsWith("<version>") || !trimmed.endsWith("</version>"))
						throw new IllegalStateException("Invalid Line: " + trimmed);
					versions.add(0, trimmed.replace("<version>", "").replace("</version>", ""));
				}
			});
			complete(versions);
		}
	}

	public List<String> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public String getLatestVersion() {
		return versions.get(0);
	}

}
