package com.synapticloop.legislation.bean;

public class ContentText {
	private final boolean isList;
	private final String content;

	public ContentText(boolean isList, String content) {
		this.isList = isList;
		this.content = content;
	}

	@Override public String toString() {
		return (
				(isList ? "List > " : "Text > ")
						+ content);
	}
}
