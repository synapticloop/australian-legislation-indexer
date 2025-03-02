package com.synapticloop.legislation;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

public class Main {
	private static final String REPEALED = "[REPEALED]";

	public static void main(String[] args) throws IOException, XMLStreamException, ParserConfigurationException, SAXException, DocumentException {
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

	private static void downloadAndParse(String name, String type, String xmlUrl) throws IOException, XMLStreamException, ParserConfigurationException, SAXException, DocumentException {
		new File("./cached").mkdirs();

		String act = name + "-" + xmlUrl.substring(xmlUrl.lastIndexOf("/") + 1);
		System.out.println("Downloading " + type + " from " + xmlUrl);

		File outputFile = new File("./cached/" + act + ".xml");

		if (!outputFile.exists()) {

			FileUtils.copyURLToFile(new URL(xmlUrl), outputFile);
			String xml = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);

			FileUtils.writeStringToFile(outputFile, prettyPrintByDom4j(xml, 2, true), StandardCharsets.UTF_8);
			;
		} else {
			System.out.println("Already downloaded - ignoring");
		}

		// now we are going to parse it

//		indexFile(name, type, outputFile);
//		jsoupParse(name, type, outputFile);
		String xml = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
		parseDom4j(name, type, xml);
	}

	private static void jsoupParse(String name, String type, File inputFile) throws IOException {
		Document document = Jsoup.parse(inputFile);
		// first up the body and definitions
		Elements bodyElements = document.select("content[type=body] level");
		for (Element bodyElement : bodyElements) {
//			System.out.println(bodyElement.text());
			Element headNoB = bodyElement.selectFirst("no b");
			System.out.println(headNoB.text());
		}

	}

	private static void parseDom4j(String name, String type, String xmlString) throws IOException, DocumentException {
		org.dom4j.Document document = DocumentHelper.parseText(xmlString);
		List<Node> nodes = document.selectNodes("//content[@type='body']/level");
		// now we have the body nodes
		for (Node node : nodes) {
			String part = getClauseNumber(node);
			String partTitle = getClauseTitle(node);

			System.out.println("[     PART] " + part + " " + partTitle);
			// now go through the clauses
			List<Node> clauseNodes = node.selectNodes("./level[@type='clause']");
			boolean isDivision = false;
			if (clauseNodes.isEmpty()) {
				isDivision = true;
				clauseNodes = node.selectNodes("./level[@type='division']/level[@type='clause']");
			}

			for (Node clauseNode : clauseNodes) {
				String clause = getClauseNumber(clauseNode);
				String clauseTitle = getClauseTitle(clauseNode);

				if(isDivision) {
					String division = getClauseNumber(clauseNode.getParent());
					String divisionTitle = getClauseTitle(clauseNode.getParent());
					System.out.println("[ DIVISION] " + division + " " + divisionTitle);
				}
				System.out.println("[   CLAUSE] " + clause + " " + clauseTitle);

				// now for the subclauses
				List<Node> subclauses = clauseNode.selectNodes(".//tier[@type='subclause']");
				for (Node subClause : subclauses) {
					String subClauseNumber = getClauseNumber(subClause);
					// see if it is repealed
					String subClauseTitle = getClauseTitle(subClause);
					String subClauseContents = "";
					if(subClauseTitle.equals(REPEALED)) {
						subClauseContents = REPEALED;
					} else {
						subClauseContents = getSingleLine(subClause.selectSingleNode("./block").getStringValue());
					}
					System.out.println("[SUBCLAUSE] " + subClauseNumber + " " + subClauseContents);

				}

				List<Node> noteNodes = clauseNode.selectNodes("./note");
				for (Node noteNode : noteNodes) {
					String noteHeading = getNoteHeading(noteNode);
					String noteContents = getSingleLine(noteNode.selectSingleNode("./block").getStringValue());
					System.out.println("[    NOTE] " + noteHeading + " " + noteContents);
				}


			}
		}
	}

	private static String getClauseNumber(Node clauseNode) {
		return(getSingleLine(clauseNode.selectSingleNode("./head/no").getStringValue()));
	}

	private static String getNoteHeading(Node clauseNode) {
		Node node = clauseNode.selectSingleNode("./heading/b");
		return(getSingleLine(node.getStringValue()));
	}

	private static String getClauseTitle(Node clauseNode) {
		Node node = clauseNode.selectSingleNode("./head/heading/b");
		if(null != node) {
			return(getSingleLine(node.getStringValue()));
		} else {
			node = clauseNode.selectSingleNode("./repealed/repealedtxt");
			if(null == node) {
				return("");
			}
			if(!node.getStringValue().trim().isEmpty()) {
				return(getSingleLine(node.getStringValue()));
			} else {
				return(REPEALED);
			}
		}
	}

	private static String getSingleLine(String input) {
		return(input.replaceAll("\\n", " ").replaceAll(" +", " ").trim());
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
						}
						break;
					case "head":
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
						if (isList) {
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
						heading.setLength(0);
						break;
					case "block":
						String contentTrim = content.toString().trim();
						if (!contentTrim.isBlank()) {
							System.out.println("[CONTENT] " + contentTrim);
						}
						content.setLength(0);
						break;
					case "list", "deflist":
						isList = false;
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
			for (Node selectNode : document.selectNodes("//content[type=content]")) {
				System.out.println(selectNode.asXML());
			}

			StringWriter sw = new StringWriter();
			XMLWriter writer = new XMLWriter(sw, format);
			writer.write(document);
			return sw.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
		}
	}
}