package com.synapticloop.legislation;

import com.synapticloop.legislation.bean.ContentBean;
import com.synapticloop.legislation.handle.LegislationHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.solr.client.solrj.impl.CloudHttp2SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
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
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main {
	public static List<ContentBean> contentBeans = new ArrayList<>();
	public static void main(String[] args) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
		// This is where you can kick off the indexing project
		// load the JSON file
		JSONArray jsonArray = new JSONArray(FileUtils.readFileToString(
				new File("./legislation.json"),
				StandardCharsets.UTF_8));

		for (Object o : jsonArray) {
			contentBeans.clear();

			JSONObject jsonObject = (JSONObject) o;
			String name = jsonObject.getString("name");
			String type = jsonObject.getString("type");
			String xmlUrl = jsonObject.getString("xml_url");
			downloadAndParse(name, type, xmlUrl);
			for (ContentBean contentBean : contentBeans) {
				System.out.println("------------------------");
				System.out.println(contentBean);
			}
			return;
		}
	}

	private static void downloadAndParse(String name, String type, String xmlUrl) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
		new File("./cached").mkdirs();

		String act = name + "-" + xmlUrl.substring(xmlUrl.lastIndexOf("/") + 1);
		System.out.println("Downloading " + type + " from " + xmlUrl);

		File outputFile = new File("./cached/" + act + ".xml");

		if (!outputFile.exists()) {
			// we are cleaning up the XML to fix newline issues
			FileUtils.copyURLToFile(new URL(xmlUrl), outputFile);
			String xml = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);

			FileUtils.writeStringToFile(outputFile, prettyPrintByDom4j(xml, 2, true), StandardCharsets.UTF_8);
		} else {
			System.out.println("Already downloaded - ignoring");
		}

		// now we are going to parse it

		indexFile(name, type, outputFile);
		// indexToSolr
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

		StringBuilder heading = new StringBuilder();
		StringBuilder content = new StringBuilder();
		boolean isHeading = false;
		boolean isContent = false;
		boolean isList = false;
		ContentBean currentContentBean = new ContentBean();

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
					case "level", "tier":
						if (shouldIndex) {
							level = startElement.getAttributeByName(new QName("type")).getValue();
							id = startElement.getAttributeByName(new QName("id")).getValue();
							System.out.println("found level of '" + level + "', with id of '" + id + "'");

							currentContentBean.setId(id);
							currentContentBean.setType(level);
						}
						break;
					case "head":
						currentContentBean = new ContentBean();
						isHeading = true;
						isContent = false;
						break;
					case "block":
						isContent = true;
						isHeading = false;
						break;
					case "list", "deflist":
						isList = true;
						break;

				}
			}

			if (nextEvent.isCharacters()) {
				if (shouldIndex) {
					Characters characters = nextEvent.asCharacters();
					String data = characters.getData().replaceAll("\\n", " ").trim();
					if (isHeading) {
						if (!data.isBlank()) {
							heading.append(data)
									.append(" ");
						}
					}
					if (isContent) {
						if(isList) {
							content.append("\n");
							isList = false;
						}

						if (!data.isBlank()) {
							content.append(data)
									.append(" ");
						}
					}
				}
			}

			if (nextEvent.isEndElement()) {
				EndElement endElement = nextEvent.asEndElement();
				String element = endElement.getName().getLocalPart();
				switch (element) {
					case "head":
						String trim = heading.toString().trim();
						if (!trim.isBlank()) {
							System.out.println("[HEADING] " + trim);
						}
						currentContentBean.setHeading(heading.toString());
						heading.setLength(0);
						break;
					case "block":
						String contentTrim = content.toString().trim();
						if (!contentTrim.isBlank()) {
							System.out.println("[CONTENT] " + contentTrim);
						}
						currentContentBean.addContent(isList, contentTrim);
						content.setLength(0);
						break;
					case "list", "deflist":
						isList = false;
						break;
					case "level":
						contentBeans.add(currentContentBean);
//						currentContentBean = new ContentBean();
						break;
				}
			}
		}
		//				SolrInputDocument doc = new SolrInputDocument();
		//		doc.addField();

	}

	public static String prettyPrintByDom4j(String xmlString, int indent, boolean skipDeclaration) {
		try {
			OutputFormat format = OutputFormat.createPrettyPrint();
			format.setIndentSize(indent);
			format.setSuppressDeclaration(skipDeclaration);
			format.setEncoding("UTF-8");

			org.dom4j.Document document = DocumentHelper.parseText(xmlString);
			StringWriter sw = new StringWriter();
			XMLWriter writer = new XMLWriter(sw, format);
			writer.write(document);
			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
		}
	}
}