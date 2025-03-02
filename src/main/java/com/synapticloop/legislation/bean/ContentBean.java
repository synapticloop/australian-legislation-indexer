package com.synapticloop.legislation.bean;

import java.util.ArrayList;
import java.util.List;

public class ContentBean {
	private String heading;
	private String type;
	private String id;

	private List<ContentText> contents = new ArrayList<>();

	public ContentBean() {
	}

	public void setHeading(String heading) {
		this.heading = heading;
	}

	public void addContent(boolean isList, String content) {
		contents.add(new ContentText(isList, content));
	}


	public void setType(String type) {
		this.type = type;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(heading).append("\n");
		sb.append(type).append("\n");
		sb.append(id).append("\n");
		for(ContentText contentText : contents) {
			sb.append(contentText).append("\n");
		}
		return(sb.toString());
	}
}
