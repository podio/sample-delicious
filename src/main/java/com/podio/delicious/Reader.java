package com.podio.delicious;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.podio.BaseAPI;
import com.podio.app.AppAPI;
import com.podio.app.Application;
import com.podio.app.ApplicationField;
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

	private final String feed;

	private final int appId;

	private BaseAPI podioAPI;

	public Reader(String configFile) throws IOException {
		Properties config = new Properties();
		config.load(new FileInputStream(configFile));

		String endpoint = config.getProperty("podio.endpoint");

		this.podioAPI = new BaseAPI("api." + endpoint, "upload." + endpoint,
				443, true, false, new OAuthClientCredentials(
						config.getProperty("podio.client.mail"),
						config.getProperty("podio.client.secret")),
				new OAuthUsernameCredentials(config
						.getProperty("podio.user.mail"), config
						.getProperty("podio.user.password")));

		this.feed = config.getProperty("delicious.feed");
		this.appId = Integer.parseInt(config.getProperty("podio.app"));
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

	private AppMapping getAppMapping() {
		Application app = new AppAPI(podioAPI).getApp(appId);

		return AppMapping.get(app);
	}

	private void saveItems(List<Bookmark> bookmarks) {
		AppMapping mapping = getAppMapping();
		ItemAPI itemAPI = new ItemAPI(podioAPI);

		for (Bookmark bookmark : bookmarks) {
			List<ItemBadge> items = itemAPI.getItemsByExternalId(appId,
					bookmark.getId()).getItems();
			if (items.size() == 0) {
				itemAPI.addItem(appId, mapping.map(bookmark), true);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException(
					"Expected exactly one argument which should be the path of the configuration file");
		}

		new Reader(args[0]).run();
	}

	private static final class AppMapping {

		private final int title;

		private final int url;

		private final int notes;

		private AppMapping(int title, int url, int notes) {
			super();
			this.title = title;
			this.url = url;
			this.notes = notes;
		}

		public ItemCreate map(Bookmark bookmark) {
			List<FieldValuesUpdate> fields = new ArrayList<FieldValuesUpdate>();
			fields.add(new FieldValuesUpdate(title, "value", bookmark
					.getTitle()));
			fields.add(new FieldValuesUpdate(url, "value", bookmark.getLink()));
			fields.add(new FieldValuesUpdate(notes, "value", bookmark
					.getNotes()));

			return new ItemCreate(bookmark.getId(), fields,
					Collections.<Integer> emptyList(), bookmark.getTags());
		}

		public static AppMapping get(Application app) {
			List<ApplicationField> fields = app.getFields();

			return new AppMapping(getField(fields, "Title"), getField(fields,
					"URL"), getField(fields, "Notes"));
		}

		private static int getField(List<ApplicationField> fields, String label) {
			for (ApplicationField field : fields) {
				if (field.getConfiguration().getLabel().equals(label)) {
					return field.getId();
				}
			}

			throw new IllegalArgumentException("No field found with the label "
					+ label);
		}
	}
}
