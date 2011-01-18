package org.jinzora.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.teleal.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.teleal.cling.support.contentdirectory.ContentDirectoryException;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.container.StorageFolder;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.common.util.MimeType;

import android.util.Log;

/**
 * This class is intended to provide improved compatibility to
 * the UPnP media services found in WMP and the XBox360.
 *
 */
public class WMPContentDirectory extends AbstractContentDirectoryService {
	private static final String TAG = "jinzora";
	private final UpnpConfiguration mConfig = UpnpService.getConfig();
	public static class ID {
		public static final String ROOT = "0";
		public static final String MUSIC = "1";
		public static final String MUSIC_ALL_MUSIC = "4";
		public static final String MUSIC_GENRE = "5";
		public static final String MUSIC_ARTIST = "6";
		public static final String MUSIC_ALBUM = "7";
	}
	
	private JinzoraApi mApi = new JinzoraApi(mConfig);
	
	private class BrowseParameters {
		String objectId;
		BrowseFlag browseFlag;
		String filter;
		long startingIndex;
		long requestedCount;
		SortCriterion[] sort;
		
		BrowseParameters(String objectId, BrowseFlag browseFlag, String filter, 
				long startingIndex, long requestedCount, SortCriterion[] sort) {
			
			this.objectId = objectId;
			this.browseFlag = browseFlag;
			this.filter = filter;
			this.startingIndex = startingIndex;
			this.requestedCount = requestedCount;
			this.sort = sort;
		}
	}
	
	@Override
	public BrowseResult search(String containerId, String searchCriteria,
			String filter, long firstResult, long maxResults,
			SortCriterion[] orderBy) throws ContentDirectoryException {
		Log.d(TAG, "SEARCH: " + searchCriteria);
		
		// This is a very hacky search method that covers the basics of what
		// the XBox and WMP require.
		
		// can also determine query type by containerId.
		
		if (searchCriteria == null || searchCriteria.equalsIgnoreCase(
				"upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false")
				|| searchCriteria.equalsIgnoreCase("(upnp:class derivedfrom \"object.item.audioItem\")")) {
			
			return mApi.getAllTracks(firstResult, maxResults);
		}
		
		if (searchCriteria.equalsIgnoreCase("(upnp:class = \"object.container.album.musicAlbum\")")) {
			return mApi.getAllAlbums(firstResult, maxResults);
		}
		
		if (searchCriteria.equalsIgnoreCase("(upnp:class = \"object.container.person.musicArtist\")")) {
			return mApi.getAllArtists(firstResult, maxResults);
		}
		
		if (searchCriteria.equalsIgnoreCase("(upnp:class = \"object.container.genre.musicGenre\")")) {
			return mApi.getAllGenres(firstResult, maxResults);
		}
		
		if (searchCriteria.startsWith("(upnp:class = \"object.container.album.musicAlbum\") and (upnp:artist = \"")) {
			String query = searchCriteria;
			int lastQuote = query.lastIndexOf('"');
			query = query.substring(0, lastQuote);
			int secondToLast = query.lastIndexOf('"');
			String artist = query.substring(secondToLast + 1);
			return mApi.getAlbumsForArtist(artist, firstResult, maxResults);
		}

		return super.search(containerId, searchCriteria, filter, firstResult,
				maxResults, orderBy);
	}
	
	@Override
	public BrowseResult browse(String objectId, BrowseFlag browseFlag, String filter,
			long startingIndex, long requestedCount, SortCriterion[] sort)
			throws ContentDirectoryException {
		
		Log.d(TAG, "Browse request: "
				+ objectId
				+ ", " + browseFlag
				+ ", " + filter
				+ ", " + startingIndex
				+ ", " + requestedCount
				+ ", " + sort.length);
		
		BrowseParameters params = new BrowseParameters(objectId, browseFlag, filter, startingIndex, requestedCount, sort);
		
		if (BrowseFlag.DIRECT_CHILDREN.equals(browseFlag)) {
			Log.d(TAG, "request to browse children: " + objectId);
			try {
				return getBrowseResult(params);
			} catch (Exception ex) {
				throw new ContentDirectoryException(
						ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
			}
		} else if (BrowseFlag.METADATA.equals(browseFlag)) {
			Log.d(TAG, "request for metadata: " + objectId);
			try {
				// not yet supported
				DIDLContent didl = getMetadataDidl(objectId);
				Log.d(TAG, "response: " + new DIDLParser().generate(didl));
				
				return new BrowseResult(new DIDLParser().generate(didl), 1, 1);
			} catch (Exception ex) {
				Log.e(TAG, "Error processing request", ex);
				throw new ContentDirectoryException(
						ContentDirectoryErrorCode.CANNOT_PROCESS, ex.toString());
			}
		}
		Log.d(TAG, "Unknown request: " + objectId + ", " + browseFlag);
		throw new ContentDirectoryException(ContentDirectoryErrorCode.CANNOT_PROCESS,
				"Cannot handle browse request " + browseFlag);
	}
	
	private DIDLContent getMetadataDidl(String objectId) {
		// TODO: currently only supports tracks.
		DIDLContent didl = new DIDLContent();
		
		String urlStr = mConfig.getJinzoraEndpoint();
		if (urlStr == null) {
			Log.w(TAG, "No jinzora service configured");
			return null;
		}
		urlStr += "&request=trackinfo&output=json&jz_path=" + objectId;
		URL url;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Bad URL", e);
			return null;
		}
		String content;
		JSONObject json;
		try {
			content = contentFromURL(url);
		} catch (IOException e) {
			Log.e(TAG, "Error accessing api", e);
			return null;
		}
		try {
			// gross api!
			json = new JSONObject(content);
			JSONArray tracks = json.getJSONArray("tracks");
			json = tracks.getJSONObject(0);
		} catch (JSONException e) {
			Log.e(TAG, "Return type not json", e);
			return null;
		}
		didl.addItem(JinzoraApi.jsonToMusicTrack(json));
		return didl;
	}
	
	private BrowseResult getBrowseResult(BrowseParameters params) {
		String base;
		boolean isHome = true;
		String objectId = params.objectId;
		
		if (ID.ROOT.equals(objectId)) {
			return resultForRoot();
		}
		
		if (ID.MUSIC.equals(objectId)) {
			return resultForMusic();
		}
		
		if (ID.MUSIC_GENRE.equals(objectId)) {
			return mApi.getAllGenres(params.startingIndex, params.requestedCount);
		}
		
		if (ID.MUSIC_ARTIST.equals(objectId)) {
			return mApi.getAllArtists(params.startingIndex, params.requestedCount);
		}
		
		if (ID.MUSIC_ALBUM.equals(objectId)) {
			return mApi.getAllAlbums(params.startingIndex, params.requestedCount);
		}
				
		if (objectId != null && objectId.startsWith("http")) {
			base = objectId;
			isHome = false;
		} else {
			base = mConfig.getJinzoraEndpoint();
			if (base == null) {
				Log.w(TAG, "No jinzora service configured");
				return null;
			}
			base += "&request=home&output=json";
		}
		
		URL url;
		try {
			url = new URL(base);
		} catch (MalformedURLException e) {
			return null;
		}
		try {
			DIDLContent didl = new DIDLContent();
			String content = contentFromURL(url);
			
			if (isHome) {
				JSONArray home = new JSONArray(content);
				addDidlNodes(didl, home);
			} else {
				JSONObject obj = new JSONObject(content);
				JSONArray nodes = obj.optJSONArray("nodes");
				
				if (nodes != null) {
					addDidlNodes(didl, nodes);
				}
				nodes = obj.optJSONArray("tracks");
				if (nodes != null) {
					addDidlTracks(didl, nodes);
				}
			}

			int count = didl.getContainers().size() + didl.getItems().size();
			return new BrowseResult(new DIDLParser().generate(didl), count, count);
		} catch (JSONException e) {
			Log.d(TAG, "Json content not found", e);
			return null;
		} catch (Exception e) {
			Log.w(TAG, "Error reading content", e);
			return null;
		}
	}
	
	private BrowseResult resultForMusic() {
		DIDLContent didl = new DIDLContent();
		Container c;
		
		c = new Container(ID.MUSIC_ALL_MUSIC, ID.MUSIC, "All Music", "JZWMP", new DIDLObject.Class("object.container"), 0);
		didl.addContainer(c);
		
		c = new Container(ID.MUSIC_GENRE, ID.MUSIC, "Genre", "JZWMP", new DIDLObject.Class("object.container"), 0);
		didl.addContainer(c);
		
		c = new Container(ID.MUSIC_ARTIST, ID.MUSIC, "Artist", "JZWMP", new DIDLObject.Class("object.container"), 0);
		didl.addContainer(c);
		
		c = new Container(ID.MUSIC_ALBUM, ID.MUSIC, "Album", "JZWMP", new DIDLObject.Class("object.container"), 0);
		didl.addContainer(c);
		
		int count = didl.getContainers().size() + didl.getItems().size();
		try {
		return new BrowseResult(new DIDLParser().generate(didl), count, count);
		} catch (Exception e) {
			Log.e(TAG, "Error generating result from didl", e);
			return null;
		}
	}
	
	private BrowseResult resultForRoot() {
		DIDLContent didl = new DIDLContent();
		Container c = new Container(ID.MUSIC, ID.ROOT, "Music", "JZWMP", new DIDLObject.Class("object.container"), 3);
		didl.addContainer(c);
		
		try {
			int count = didl.getContainers().size() + didl.getItems().size();
			return new BrowseResult(new DIDLParser().generate(didl), count, count);
		} catch (Exception e) {
			Log.e(TAG, "Error generating result from didl", e);
			return null;
		}
	}

	private String contentFromURL(URL url) throws IOException {
		Log.d(TAG, "getting content from " + url);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setConnectTimeout(20000);
		InputStream in = conn.getInputStream();
		conn.connect();
		//String encoding = conn.getContentEncoding();
		
		StringBuffer stringBuffer = new StringBuffer();
		byte[] buffer = new byte[2048];
		int bytes;
		while (true) {
			bytes = in.read(buffer);
			if (bytes == -1) break;
			stringBuffer.append(new String(buffer, 0, bytes));
		}
		
		return stringBuffer.toString();
	}
	
	private void addDidlNodes(DIDLContent didl, JSONArray browse) throws JSONException {
		for (int i = 0; i < browse.length(); i++) {
			JSONObject obj = browse.getJSONObject(i);
			String url = obj.getString("browse");
			String label = obj.getString("name");
			int subCount = 0;
			long storageUsed = 0;
			
			Container folder = new StorageFolder(url,"0",label,"Me", subCount, storageUsed);
			String playlist = obj.optString("playlink");
			if (playlist != null && playlist.length() > 0) {
				MimeType mimeType = new MimeType("audio", "m3u");
				Res res = new Res();
				res.setProtocolInfo(new ProtocolInfo(mimeType));
				res.setValue(playlist);
				folder.addResource(res);
			}
			didl.addContainer(folder);
		}
	}
	
	private void addDidlTracks(DIDLContent didl, JSONArray browse) {
		for (int i = 0; i < browse.length(); i++) {
			try {
				JSONObject track = browse.getJSONObject(i);
				MusicTrack musicTrack = JinzoraApi.jsonToMusicTrack(track);
		        didl.addItem(musicTrack);
			} catch (Exception e) {
				Log.w(TAG, "Bad json entry", e);
			}
		}
	}
}