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

/*
 * This file has been modified by Legacy-Fabric to make use of fabric-loader-1.8.9
 * with minecraft 1.8.9 and use legacy-maven urls.
 */
package net.fabricmc.installer.client;

import net.fabricmc.installer.util.*;

import java.io.File;
import java.io.IOException;
import java.sql.Ref;

public class ClientInstaller {

	public static String install(File mcDir, String gameVersion, String loaderVersion, InstallerProgress progress) throws IOException {
		System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion);
		
		String loader = gameVersion.equals("1.8.9") ? Reference.LEGACY_LOADER_NAME : Reference.LOADER_NAME;
		String profileName = String.format("%s-%s-%s", loader, loaderVersion, gameVersion);

		MinecraftLaunchJson launchJson = Utils.getLaunchMeta(loaderVersion, gameVersion);
		launchJson.id = profileName;
		launchJson.inheritsFrom = gameVersion;

		//Adds loader and the mappings
		String maven = gameVersion.equals("1.8.9") ? Reference.legacyMavenServerUrl : Reference.mavenServerUrl;
		launchJson.libraries.add(new MinecraftLaunchJson.Library(Reference.PACKAGE.replaceAll("/", ".") + ":" + Reference.MAPPINGS_NAME + ":" + gameVersion, Reference.legacyMavenServerUrl));
		launchJson.libraries.add(new MinecraftLaunchJson.Library(Reference.PACKAGE.replaceAll("/", ".") + ":" + loader + ":" + loaderVersion, maven));

		File versionsDir = new File(mcDir, "versions");
		File profileDir = new File(versionsDir, profileName);
		File profileJson = new File(profileDir, profileName + ".json");

		if (!profileDir.exists()) {
			profileDir.mkdirs();
		}

		/*

		This is a fun meme

		The vanilla launcher assumes the profile name is the same name as a maven artifact, how ever our profile name is a combination of 2
		(mappings and loader). The launcher will also accept any jar with the same name as the profile, it doesnt care if its empty

		 */
		File dummyJar = new File(profileDir, profileName + ".jar");
		dummyJar.createNewFile();

		Utils.writeToFile(profileJson, Utils.GSON.toJson(launchJson));

		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}
}
