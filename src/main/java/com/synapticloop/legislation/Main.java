package com.synapticloop.legislation;

import com.synapticloop.legislation.handle.LegislationHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class Main {
	public static void main(String[] args) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
		// This is where you can kick off the indexing project
		// load the JSON file
		JSONArray jsonArray = new JSONArray(FileUtils.readFileToString(
				new File("./legislation.json"),
				StandardCharsets.UTF_8));

		Iterator<Object> iterator = jsonArray.iterator();
		while (iterator.hasNext()) {
			JSONObject jsonObject = (JSONObject) iterator.next();
			String name = jsonObject.getString("name");
			String type = jsonObject.getString("type");
			String xmlUrl = jsonObject.getString("xml_url");
			downloadAndParse(name, type, xmlUrl);
			return;
		}
	}

	private static void downloadAndParse(String name, String type, String xmlUrl) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
		new File("./cached").mkdirs();

		String act = name + "-" + xmlUrl.substring(xmlUrl.lastIndexOf("/") + 1);
		System.out.println("Downloading " + type + " from " + xmlUrl);

		File outputFile = new File("./cached/" + act + ".xml");

		if (!outputFile.exists()) {
			FileUtils.copyURLToFile(new URL(xmlUrl), outputFile);
		} else {
			System.out.println("Already downloaded - ignoring");
		}

		// now we are going to parse it

		indexFile(name, type, outputFile);
	}

	private static void indexFile(String name, String type, File outputFile) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
		//		CloudSolrClient client = new CloudHttp2SolrClient.Builder(
		//				List.of("http://localhost:8983/solr/")).build();

		// do the XML parsing
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(outputFile));


		String contentType = "";
		String level = "";
		String id = "";
		String heading = "";
		String content = "";

		StringBuilder text = new StringBuilder();

		while (reader.hasNext()) {
			boolean shouldIndex = (contentType.equals("body") || contentType.equals("schedules"));
			XMLEvent nextEvent = reader.nextEvent();
			if (nextEvent.isStartElement()) {
				StartElement startElement = nextEvent.asStartElement();
				String element = startElement.getName().getLocalPart();
				switch (element) {
					case "content":
						contentType = startElement.getAttributeByName(new QName("type")).getValue();
						System.out.println("found contentType of '" + contentType + "'");
						break;
					case "level":
						if (shouldIndex) {
							level = startElement.getAttributeByName(new QName("type")).getValue();
							id = startElement.getAttributeByName(new QName("id")).getValue();
							System.out.println(text);
							System.out.println("found level of '" + level + "', with id of '" + id + "'");
							text.setLength(0);
						}
						break;
				}
			}

			if(nextEvent.isCharacters()) {
				if(shouldIndex) {
					Characters characters = nextEvent.asCharacters();
					String data = characters.getData();
					if(!data.isEmpty()) {
						text.append(data.trim() + " ");
					}
				}
			}
		}
		//				SolrInputDocument doc = new SolrInputDocument();
		//		doc.addField();

	}
}