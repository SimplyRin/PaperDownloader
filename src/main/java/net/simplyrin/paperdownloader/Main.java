package net.simplyrin.paperdownloader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.simplyrin.rinstream.RinStream;

/**
 * Created by SimplyRin on 2021/04/13.
 *
 * Copyright (c) 2021 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class Main {

	public static void main(String[] args) {
		new RinStream();
		new Main().run();
	}

	public void run() {
		File file = new File("paperdownloader.json");
		if (!file.exists()) {
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("Paper-Version", "1.19.2");
			jsonObject.addProperty("Save-As", "paperclip.jar");
			jsonObject.addProperty("Current-Build", 0);
			jsonObject.addProperty("Current-Version", "unknown");

			this.saveJsonObjectToFile(jsonObject, file);
		}

		JsonObject jsonObject = this.getJsonObject(file);

		String paperVersion = jsonObject.get("Paper-Version").getAsString();

		int currentBuild = jsonObject.get("Current-Build").getAsInt();
		String currentVersion = jsonObject.get("Current-Version").getAsString();

		// Paper の最新バージョンを確認
		String url = "https://papermc.io/api/v2/projects/paper/versions/" + paperVersion + "/builds";
		JsonObject builds = JsonParser.parseString(this.getUrl(url)).getAsJsonObject();

		String latestVersion = builds.get("version").getAsString();
		int latestBuild = 0;

		JsonArray buildsArray = builds.get("builds").getAsJsonArray();
		for (int i = 0; i < buildsArray.size(); i++) {
			JsonObject build = buildsArray.get(i).getAsJsonObject();

			latestBuild = Math.max(latestBuild, build.get("build").getAsInt());
		}

		System.out.println("現在のビルドバージョン: v" + currentBuild + " (MC: " + currentVersion + ")");
		System.out.println("最新のビルドバージョン: v" + latestBuild + " (MC: " + latestVersion + ")");

		if (currentBuild != latestBuild) {
			System.out.println("最新のファイルをダウンロードしています...");

			// https://api.papermc.io/v2/projects/paper/versions/1.19.1/builds
			String paperJarUrl = "https://papermc.io/api/v2/projects/paper/versions/" + latestVersion + "/builds/" + latestBuild
					+ "/downloads/paper-" + latestVersion + "-" + latestBuild + ".jar";

			File paperJar = new File(jsonObject.get("Save-As").getAsString());
			if (paperJar.exists()) {
				String base = FilenameUtils.getBaseName(paperJar.getName());
				String ext = FilenameUtils.getExtension(paperJar.getName());

				File targetJar = new File(base + "-v" + currentBuild + "." + ext);

				boolean bool = paperJar.renameTo(targetJar);
				if (bool) {
					System.out.println(paperJar.getName() + " を " + targetJar.getName() + " に変更しました。");
				} else {
					System.out.println(paperJar.getName() + " を " + targetJar.getName() + " に変更できませんでした。");
					System.exit(1);
					return;
				}
			}

			InputStream inputStream = this.getInputStream(paperJarUrl);
			if (inputStream != null) {
				try {
					FileUtils.copyInputStreamToFile(inputStream, paperJar);

					jsonObject.addProperty("Current-Build", latestBuild);
					jsonObject.addProperty("Current-Version", latestVersion);
					this.saveJsonObjectToFile(jsonObject, file);
					System.out.println("ファイルをダウンロードしました。");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("最新のビルドを使用しています。v" + currentBuild + " (MC: " + currentVersion + ")");
		}
	}

	public InputStream getInputStream(String url) {
		try {
			HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
			connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.114 Safari/537.36");
			connection.connect();
			return connection.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// 面倒だった
	public String getUrl(String url) {
		try {
			return IOUtils.toString(this.getInputStream(url), StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void saveJsonObjectToFile(JsonObject jsonObject, File file) {
		try {
			FileWriter fileWriter = new FileWriter(file);
			fileWriter.write(jsonObject.toString());
			fileWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JsonObject getJsonObject(File file) {
		String value = null;
		try {
			value = Files.lines(Paths.get(file.getPath()), StandardCharsets.UTF_8).collect(Collectors.joining(System.getProperty("line.separator")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (value != null) {
			return JsonParser.parseString(value).getAsJsonObject();
		}
		return null;
	}

}
