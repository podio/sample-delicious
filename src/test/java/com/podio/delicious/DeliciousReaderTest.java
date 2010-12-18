package com.podio.delicious;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class DeliciousReaderTest {

	@Test
	public void readBookmarks() throws Exception {
		DeliciousReader reader = new DeliciousReader(
				"http://feeds.delicious.com/v2/rss/chrholm2003");
		List<Bookmark> bookmarks = reader.read();

		Assert.assertEquals(bookmarks.size(), 1);
		Bookmark bookmark = bookmarks.get(0);
		Assert.assertEquals(bookmark.getId(),
				"1300b39b849a84f637af0d46dbaf9995");
		Assert.assertEquals(bookmark.getTitle(), "Podio");
		Assert.assertEquals(bookmark.getLink(), "http://podio.com/");
		Assert.assertEquals(bookmark.getNotes(), "My note");
		Assert.assertEquals(bookmark.getTags().size(), 1);
		Assert.assertEquals(bookmark.getTags().get(0), "podio");
	}
}
