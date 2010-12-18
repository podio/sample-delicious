package com.podio.delicious;

import java.util.List;

/**
 * Represents a bookmark in delicious
 */
public final class Bookmark {

	private final String id;

	private final String title;

	private final String link;

	private final List<String> tags;

	private final String notes;

	public Bookmark(String id, String title, String link, List<String> tags,
			String notes) {
		super();
		this.id = id;
		this.title = title;
		this.link = link;
		this.tags = tags;
		this.notes = notes;
	}

	@Override
	public String toString() {
		return "Bookmark [id=" + id + ", title=" + title + ", link=" + link
				+ ", tags=" + tags + ", notes=" + notes + "]";
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getLink() {
		return link;
	}

	public List<String> getTags() {
		return tags;
	}

	public String getNotes() {
		return notes;
	}
}
