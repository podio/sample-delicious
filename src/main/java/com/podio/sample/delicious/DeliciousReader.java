package com.podio.sample.delicious;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

/**
 * Class for reading feeds from delicious
 */
public final class DeliciousReader {

	private final String feed;

	/**
	 * Createa new reader for the given feed
	 * 
	 * @param feed
	 *            The feed to read
	 */
	public DeliciousReader(String feed) {
		super();
		this.feed = feed;
	}

	/**
	 * Retrieves the feed from delicious as an RSS feed
	 * 
	 * @return The given feed
	 * @throws Exception
	 *             If any error occurs during communication with delicious
	 */
	private SyndFeed getFeed() throws Exception {
		FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
		FeedFetcher feedFetcher = new HttpURLFeedFetcher(feedInfoCache);
		return feedFetcher.retrieveFeed(new URL(feed));
	}

	/**
	 * Loads the bookmarks from delicious by parsing the RSS feed
	 * 
	 * @return The loaded bookmarks
	 * @throws Exception
	 *             If any error occurs while loading the bookmarks
	 */
	public List<Bookmark> read() throws Exception {
		SyndFeed syndFeed = getFeed();

		List<Bookmark> bookmarks = new ArrayList<Bookmark>();
		List<SyndEntry> entries = syndFeed.getEntries();
		for (SyndEntry entry : entries) {
			List<String> tags = new ArrayList<String>();
			List<SyndCategory> categories = entry.getCategories();
			for (SyndCategory category : categories) {
				tags.add(category.getName());
			}

			String id = entry.getUri().substring(
					entry.getUri().lastIndexOf('/') + 1,
					entry.getUri().lastIndexOf('#'));

			bookmarks.add(new Bookmark(id, entry.getTitle(), entry.getLink(),
					tags, entry.getDescription().getValue()));
		}

		return bookmarks;
	}
}
