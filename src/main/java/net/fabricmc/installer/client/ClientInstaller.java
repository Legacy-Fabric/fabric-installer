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

package net.fabricmc.installer.client;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import mjson.Json;

import net.fabricmc.installer.LoaderVersion;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Library;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ClientInstaller {
	public static String install(Path mcDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress) throws IOException {
		System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion.name);

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion.name, gameVersion);

		Path versionsDir = mcDir.resolve("versions");
		Path profileDir = versionsDir.resolve(profileName);
		Path profileJson = profileDir.resolve(profileName + ".json");

		if (!Files.exists(profileDir)) {
			Files.createDirectories(profileDir);
		}

		/*

		This is a fun meme

		The vanilla launcher assumes the profile name is the same name as a maven artifact, how ever our profile name is a combination of 2
		(mappings and loader). The launcher will also accept any jar with the same name as the profile, it doesnt care if its empty

		 */
		Path dummyJar = profileDir.resolve(profileName + ".jar");
		Files.deleteIfExists(dummyJar);
		Files.createFile(dummyJar);

		boolean legacyLoader = loaderVersion.name.length() > 10;

//		URL profileUrl = new URL(Reference.getMetaServerEndpoint(String.format("v2/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion.name)));
//		Utils.downloadFile(profileUrl, profileJson);

		URL downloadUrl;
		if (legacyLoader) {
			downloadUrl = new URL(String.format("https://maven.legacyfabric.net/net/fabricmc/fabric-loader-1.8.9/%s/fabric-loader-1.8.9-%s.json", loaderVersion.name, loaderVersion.name));
		} else {
			downloadUrl = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-loader/%s/fabric-loader-%s.json", loaderVersion.name, loaderVersion.name));
		}

		Json json = Json.read(Utils.readTextFile(downloadUrl));

		Json libraries = Json.array(
				Json.object()
						.set("name", String.format(legacyLoader ? "net.fabricmc:fabric-loader-1.8.9:%s" : "net.fabricmc:fabric-loader:%s", loaderVersion.name))
						.set("url", legacyLoader ? "https://maven.legacyfabric.net/" : "https://maven.fabricmc.net/"),
				Json.object()
						.set("name", String.format("net.fabricmc:intermediary:%s", gameVersion))
						.set("url", "https://maven.legacyfabric.net/")
		);

		if (legacyLoader) {
			libraries.add(
					Json.object()
							.set("name", "com.google.guava:guava:21.0")
							.set("url", "https://maven.fabricmc.net/")
			);
		}

		if (!Objects.equals(gameVersion, "1.13.2") && !Objects.equals(gameVersion, "1.12.2")) {
			libraries.add(
					Json.object()
							.set("name", "org.apache.logging.log4j:log4j-api:2.8.1")
							.set("url", "https://libraries.minecraft.net/")
			);
			libraries.add(
					Json.object()
							.set("name", "org.apache.logging.log4j:log4j-core:2.8.1")
							.set("url", "https://libraries.minecraft.net/")
			);
		}

		for (Json libraryJson : json.at("libraries").at("common").asJsonList()) {
			libraries.add(
					Json.object()
							.set("name", libraryJson.at("name").asString())
							.set("url", libraryJson.at("url").asString())
			);
		}

		Json versionJson = Json.object()
				.set("id", profileName)
				.set("inheritsFrom", gameVersion)
				.set("type", "release")
				.set("mainClass", "net.fabricmc.loader.launch.knot.KnotClient")
				.set("libraries", libraries);

		FileWriter writer = new FileWriter(profileJson.toFile());
		writer.write(versionJson.toString());
		writer.close();


		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}
}
