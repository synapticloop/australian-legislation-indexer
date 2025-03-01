package com.synapticloop.legislation.handle;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LegislationHandler extends DefaultHandler {
	@Override public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
	}

	@Override public void startElement(
			String uri,
			String localName,
			String qName,
			Attributes attributes) throws SAXException {

		super.startElement(uri, localName, qName, attributes);
		System.out.println("new element " + qName);
	}

	@Override public void endElement(
			String uri,
			String localName,
			String qName) throws SAXException {

		super.endElement(uri, localName, qName);
	}
}
