/*
 * Copyright (c) 2021 Legacy Fabric/Quilt
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

package net.fabricmc.installer;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Utils;

public abstract class Handler implements InstallerProgress {

	public JButton buttonInstall;

	public JComboBox<String> gameVersionComboBox;
	public JComboBox<String> loaderVersionComboBox;
	public JTextField installLocation;
	public JButton selectFolderButton;
	public JLabel statusLabel;

	public JCheckBox snapshotCheckBox;

	private JPanel pane;

	public abstract String name();

	public abstract void install();

	public abstract void installCli(ArgumentParser args) throws Exception;

	public abstract String cliHelp();

	//this isnt great, but works
	public abstract void setupPane1(JPanel pane, InstallerGui installerGui);

	public abstract void setupPane2(JPanel pane, InstallerGui installerGui);

	public JPanel makePanel(InstallerGui installerGui) {
		pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

		setupPane1(pane, installerGui);

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(Utils.BUNDLE.getString("prompt.game.version")));
			jPanel.add(gameVersionComboBox = new JComboBox<>());
			gameVersionComboBox.addActionListener(e -> {
				if (Main.LOADER_META.isComplete()) {
					updateLoaderVersions();
				}
			});
			jPanel.add(snapshotCheckBox = new JCheckBox(Utils.BUNDLE.getString("option.show.snapshots")));
			snapshotCheckBox.setSelected(false);
			snapshotCheckBox.addActionListener(e -> {
				if (Main.GAME_VERSION_META.isComplete()) {
					updateGameVersions();
				}
			});
		});

		Main.GAME_VERSION_META.onComplete(versions -> {
			updateGameVersions();
		});

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(Utils.BUNDLE.getString("prompt.loader.version")));
			jPanel.add(loaderVersionComboBox = new JComboBox<>());
		});

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(Utils.BUNDLE.getString("prompt.select.location")));
			jPanel.add(installLocation = new JTextField());
			jPanel.add(selectFolderButton = new JButton());

			selectFolderButton.setText("...");
			selectFolderButton.addActionListener(e -> InstallerGui.selectInstallLocation(() -> installLocation.getText(), s -> installLocation.setText(s)));
		});

		setupPane2(pane, installerGui);

		addRow(pane, jPanel -> {
			jPanel.add(statusLabel = new JLabel());
			statusLabel.setText(Utils.BUNDLE.getString("prompt.loading.versions"));
		});

		addRow(pane, jPanel -> {
			jPanel.add(buttonInstall = new JButton(Utils.BUNDLE.getString("prompt.install")));
			buttonInstall.addActionListener(e -> {
				buttonInstall.setEnabled(false);
				install();
			});
		});

		
		Main.LOADER_META.onComplete(versions -> {
			updateLoaderVersions();
			statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
		});

		return pane;
	}

	private static int compareVersions(String a, String b) {
		if (a == null) return -1;
		if (b == null) return 1;
		if (a.equals(b)) return 0;
		Pattern pattern = Pattern.compile("^\\d+(\\.\\d+)+");
		Matcher matcherA = pattern.matcher(a);
		Matcher matcherB = pattern.matcher(b);
		if (!matcherA.find() || !matcherB.find()) {
			throw new IllegalArgumentException("Invalid version");
		}
		String[] aParts =  matcherA.group(0).split("\\.");
		String[] bParts =  matcherB.group(0).split("\\.");
		for (int i = 0; i < aParts.length || i < bParts.length; i++) {
			if (i >= aParts.length && !"0".equals(bParts[i])) return -1;
			if (i >= bParts.length && !"0".equals(aParts[i])) return 1;
			int cmp = Integer.compare(Integer.parseInt(aParts[i]), Integer.parseInt(bParts[i]));
			if (cmp != 0) return cmp;
		}
		return 0;
	}
	
	private void updateLoaderVersions()
	{
		loaderVersionComboBox.removeAllItems();
		for (MetaHandler.GameVersion version : Main.LOADER_META.getVersions())
		{
			boolean guavaLoader = ((MetaHandler.LoaderVersion) version).getName().equals("fabric-loader-1.8.9");
			String gameVersion = (String) gameVersionComboBox.getSelectedItem();
			String loaderVersion = version.getVersion();
			try {
				// Versions < 1.12 ship Guava 17 so they need the fix
				if (compareVersions(gameVersion, "1.12") < 0) {
					if (guavaLoader) {
						loaderVersionComboBox.addItem(loaderVersion);
						loaderVersionComboBox.setSelectedItem(version.isStable() ? loaderVersion : 0);
					}
					continue;
				}
				// 1.12 needs a fix that's implemented since loader 0.11.3
				if (compareVersions(gameVersion, "1.12.2") <= 0) {
					if (compareVersions(loaderVersion, "0.11.3") >= 0) {
						loaderVersionComboBox.addItem(loaderVersion);
						loaderVersionComboBox.setSelectedItem(version.isStable() ? loaderVersion : 0);
					}
					continue;
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
			if (!guavaLoader) loaderVersionComboBox.addItem(loaderVersion);
			loaderVersionComboBox.setSelectedItem(version.isStable() ? loaderVersion : 0);
		}
	}

	private void updateGameVersions() {
		gameVersionComboBox.removeAllItems();
		for (MetaHandler.GameVersion version : Main.GAME_VERSION_META.getVersions()) {
			if (!snapshotCheckBox.isSelected() && !version.isStable()) {
				continue;
			}
			gameVersionComboBox.addItem(version.getVersion());
		}
		gameVersionComboBox.setSelectedIndex(0);
	}

	@Override
	public void updateProgress(String text) {
		statusLabel.setText(text);
		statusLabel.setForeground(UIManager.getColor("Label.foreground"));
	}

	protected String buildEditorPaneStyle() {
		JLabel label = new JLabel();
		Font font = label.getFont();
		Color color = label.getBackground();
		return String.format(
				"font-family:%s;font-weight:%s;font-size:%dpt;background-color: rgb(%d,%d,%d);",
				font.getFamily(), (font.isBold() ? "bold" : "normal"), font.getSize(), color.getRed(), color.getGreen(), color.getBlue()
				);
	}

	@Override
	public void error(Throwable throwable) {
		StringWriter sw = new StringWriter(800);

		try (PrintWriter pw = new PrintWriter(sw)) {
			throwable.printStackTrace(pw);
		}

		String st = sw.toString().trim();
		System.err.println(st);

		String html = String.format("<html><body style=\"%s\">%s</body></html>",
				buildEditorPaneStyle(),
				st.replace(System.lineSeparator(), "<br>").replace("\t", "&ensp;"));
		JEditorPane textPane = new JEditorPane("text/html", html);
		textPane.setEditable(false);

		statusLabel.setText(throwable.getLocalizedMessage());
		statusLabel.setForeground(Color.RED);

		JOptionPane.showMessageDialog(
				pane,
				textPane,
				Utils.BUNDLE.getString("prompt.exception.occurrence"),
				JOptionPane.ERROR_MESSAGE
				);
	}

	protected void addRow(Container parent, Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		parent.add(panel);
	}

	protected String getGameVersion(ArgumentParser args) {
		return args.getOrDefault("mcversion", () -> {
			System.out.println("Using latest game version");
			try {
				Main.GAME_VERSION_META.load();
			} catch (IOException e) {
				throw new RuntimeException("Failed to load latest versions", e);
			}
			return Main.GAME_VERSION_META.getLatestVersion(args.has("snapshot")).getVersion();
		});
	}

	protected String getLoaderVersion(ArgumentParser args) {
		return args.getOrDefault("loader", () -> {
			System.out.println("Using latest loader version");
			try {
				Main.LOADER_META.load();
			} catch (IOException e) {
				throw new RuntimeException("Failed to load latest versions", e);
			}
			return Main.LOADER_META.getLatestVersion(false).getVersion();
		});
	}

}
