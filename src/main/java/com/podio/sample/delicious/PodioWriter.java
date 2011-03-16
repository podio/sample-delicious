package com.podio.sample.delicious;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.podio.ResourceFactory;
import com.podio.app.AppAPI;
import com.podio.app.Application;
import com.podio.app.ApplicationField;
import com.podio.item.FieldValuesUpdate;
import com.podio.item.ItemAPI;
import com.podio.item.ItemBadge;
import com.podio.item.ItemCreate;

/**
 * Writer to save bookmarks to a Podio app.
 * 
 * The app in Podio must have 3 fields with the labels Title, URL and Notes.
 */
public final class PodioWriter {

	private final int appId;

	private final ResourceFactory podioAPI;

	/**
	 * Creates a new writer that will write to the given app using the API
	 * 
	 * @param appId
	 *            The id of the app
	 * @param podioAPI
	 *            The API class to use
	 */
	public PodioWriter(int appId, ResourceFactory podioAPI) {
		super();
		this.appId = appId;
		this.podioAPI = podioAPI;
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
	public void write(List<Bookmark> bookmarks) {
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
			if (bookmark.getNotes() != null) {
				fields.add(new FieldValuesUpdate(notes, "value", bookmark
						.getNotes()));
			}

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
