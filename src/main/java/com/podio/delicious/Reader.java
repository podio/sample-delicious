package com.podio.delicious;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.podio.BaseAPI;
import com.podio.item.FieldValuesUpdate;
import com.podio.item.ItemAPI;
import com.podio.item.ItemBadge;
import com.podio.item.ItemCreate;
import com.podio.oauth.OAuthClientCredentials;
import com.podio.oauth.OAuthUsernameCredentials;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.fetcher.FeedFetcher;
import com.sun.syndication.fetcher.impl.FeedFetcherCache;
import com.sun.syndication.fetcher.impl.HashMapFeedInfoCache;
import com.sun.syndication.fetcher.impl.HttpURLFeedFetcher;

public class Reader {

	private static final int APP_ID = 13938;

	private static final int TITLE = 76687;
	private static final int URL = 76688;
	private static final int NOTES = 76689;

	private final String feed;

	private final String space;

	private BaseAPI podioAPI;

	public Reader(String configFile) throws IOException {
		Properties config = new Properties();
		config.load(new FileInputStream(configFile));

		this.podioAPI = new BaseAPI("api.nextpodio.dk", "upload.nextpodio.dk",
				443, true, false, new OAuthClientCredentials(
						config.getProperty("podio.client.mail"),
						config.getProperty("podio.client.secret")),
				new OAuthUsernameCredentials(config
						.getProperty("podio.user.mail"), config
						.getProperty("podio.user.password")));

		this.feed = config.getProperty("delicious.feed");
		this.space = config.getProperty("podio.space");
	}

	public void run() throws Exception {
		List<Bookmark> bookmarks = loadBookmarks();
		saveItems(bookmarks);
	}

	private SyndFeed getFeed() throws Exception {
		FeedFetcherCache feedInfoCache = HashMapFeedInfoCache.getInstance();
		FeedFetcher feedFetcher = new HttpURLFeedFetcher(feedInfoCache);
		return feedFetcher.retrieveFeed(new URL(feed));
	}

	private List<Bookmark> loadBookmarks() throws Exception {
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

	private void saveItems(List<Bookmark> bookmarks) {
		ItemAPI itemAPI = new ItemAPI(podioAPI);

		for (Bookmark bookmark : bookmarks) {
			List<ItemBadge> items = itemAPI.getItemsByExternalId(APP_ID,
					bookmark.getId()).getItems();
			if (items.size() == 0) {
				List<FieldValuesUpdate> fields = new ArrayList<FieldValuesUpdate>();
				fields.add(new FieldValuesUpdate(TITLE, "value", bookmark
						.getTitle()));
				fields.add(new FieldValuesUpdate(URL, "value", bookmark
						.getLink()));
				fields.add(new FieldValuesUpdate(NOTES, "value", bookmark
						.getNotes()));

				itemAPI.addItem(
						APP_ID,
						new ItemCreate(bookmark.getId(), fields, Collections
								.<Integer> emptyList(), bookmark.getTags()),
						true);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new Reader(args[0]).run();
	}
}
