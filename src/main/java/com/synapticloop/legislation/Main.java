package com.synapticloop.legislation;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Main {
	public static void main(String[] args) throws IOException {
		// This is where you can kick off the indexing project
		// load the JSON file
		JSONArray jsonArray = new JSONArray(FileUtils.readFileToString(
				new File("./legislation.json"),
				StandardCharsets.UTF_8));

		Iterator<Object> iterator = jsonArray.iterator();
		while (iterator.hasNext()) {
			JSONObject jsonObject = (JSONObject)iterator.next();
			String name = jsonObject.getString("name");
			String type = jsonObject.getString("type");
			String xmlUrl = jsonObject.getString("xml_url");
			downloadAndParse(name, type, xmlUrl);
		}
	}

	private static void downloadAndParse(String name, String type, String xmlUrl) throws IOException {
		new File("./cached").mkdirs();

		String act = name + "-" + xmlUrl.substring(xmlUrl.lastIndexOf("/") +1);
		System.out.println("Downloading " + type + " from " + xmlUrl);

		File outputFile = new File("./cached/" + act + ".xml");

		if(!outputFile.exists()) {
			FileUtils.copyURLToFile(new URL(xmlUrl), outputFile);
		} else {
			System.out.println("Already downloaded - ignoring");
		}

		// now we are going to parse it

		indexFile(name, type, outputFile);
	}

	private static void indexFile(String name, String type, File outputFile) {
	}
}