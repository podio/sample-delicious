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

/**
 * Imports bookmarks from delicious to a Podio app. The feed, app id and
 * authentication configuration must be given in a configuration file.
 * 
 * The app in Podio must have 3 fields with the labels Title, URL and Notes.
 */
public final class Reader {

	private final String feed;

	private final int appId;

	private final BaseAPI podioAPI;

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

	/**
	 * Performs the loading and saving of bookmarks
	 * 
	 * @throws Exception
	 *             If any error occurs during loading or saving
	 */
	public void run() throws Exception {
		List<Bookmark> bookmarks = loadBookmarks();
		saveBookmarks(bookmarks);
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

	/**
	 * Creates the app mapping for the app id from the configuration
	 * 
	 * @return The app mapping created
	 */
	private AppMapping getAppMapping() {
		Application app = new AppAPI(podioAPI).getApp(appId);

		return AppMapping.get(app);
	}

	/**
	 * Saves the bookmarks as items in Podio
	 * 
	 * @param bookmarks
	 *            The bookmarks to save
	 */
	private void saveBookmarks(List<Bookmark> bookmarks) {
		AppMapping mapping = getAppMapping();
		ItemAPI itemAPI = new ItemAPI(podioAPI);

		for (Bookmark bookmark : bookmarks) {
			// Check that the bookmark has not already been added
			List<ItemBadge> items = itemAPI.getItemsByExternalId(appId,
					bookmark.getId()).getItems();
			if (items.size() == 0) {
				// No items exists, so add the item
				itemAPI.addItem(appId, mapping.map(bookmark), true);
			}
			// TODO: Update the existing bookmark with new title, notes and tags
		}
	}

	/**
	 * Start the importer with the given configuration file
	 * 
	 * @param args
	 *            The first parameter must be the path to the configuration file
	 * @throws Exception
	 *             If any error occurs during execution
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException(
					"Expected exactly one argument which should be the path of the configuration file");
		}

		new Reader(args[0]).run();
	}

	/**
	 * Maintans a mapping for the individual fields in the app
	 */
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

		/**
		 * Returns the create object to be used when creating the object in
		 * Podio
		 * 
		 * @param bookmark
		 *            The bookmark to map
		 * @return The mapped object
		 */
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

		/**
		 * Creates a mapping configuration based on the app
		 * 
		 * @param app
		 *            The app to create a mapping for
		 * @return The mapping created
		 */
		public static AppMapping get(Application app) {
			List<ApplicationField> fields = app.getFields();

			return new AppMapping(getField(fields, "Title"), getField(fields,
					"URL"), getField(fields, "Notes"));
		}

		/**
		 * Finds a field in the list of fields with the given name
		 * 
		 * @param fields
		 *            The fields to search through
		 * @param label
		 *            The label to search for
		 * @return The id of the matching field
		 */
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
