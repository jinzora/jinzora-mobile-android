package org.jinzora.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jinzora.Jinzora;
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
import org.teleal.cling.support.model.PersonWithRole;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.teleal.cling.support.model.DIDLObject.Property.UPNP.GENRE;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.container.MusicAlbum;
import org.teleal.cling.support.model.container.MusicArtist;
import org.teleal.cling.support.model.container.MusicGenre;
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
	
	public static class ID {
		public static final String ROOT = "0";
		public static final String MUSIC = "1";
		public static final String MUSIC_ALL_MUSIC = "4";
		public static final String MUSIC_GENRE = "5";
		public static final String MUSIC_ARTIST = "6";
		public static final String MUSIC_ALBUM = "7";
	}
	
	private UpnpConfiguration upnpConfig = new UpnpConfiguration(null);
	
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
				DIDLContent didl = getBrowseDidl(params);
				int count = didl.getContainers().size() + didl.getItems().size();
				Log.d(TAG, "response: " + new DIDLParser().generate(didl));
				
				return new BrowseResult(new DIDLParser().generate(didl), count, count);
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
		
		String urlStr = Jinzora.getBaseURL();
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
		didl.addItem(jsonToMusicTrack(json));
		return didl;
	}
	
	private DIDLContent getBrowseDidl(BrowseParameters params) {
		String base;
		boolean isHome = true;
		String objectId = params.objectId;
		
		if (ID.ROOT.equals(objectId)) {
			return didlForRoot();
		}
		
		if (ID.MUSIC.equals(objectId)) {
			return didlForMusic();
		}
		
		if (ID.MUSIC_GENRE.equals(objectId)) {
			return didlForGenre();
		}
		
		if (ID.MUSIC_ARTIST.equals(objectId)) {
			return didlForArtist();
		}
		
		if (ID.MUSIC_ALBUM.equals(objectId)) {
			return didlForAlbum(params);
		}
				
		if (objectId != null && objectId.startsWith("http")) {
			base = objectId;
			isHome = false;
		} else {
			base = upnpConfig.getJinzoraEndpoint();
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

			return didl;
		} catch (IOException e) {
			Log.w(TAG, "Error reading content", e);
			return null;
		} catch (JSONException e) {
			Log.d(TAG, "Json content not found", e);
			return null;
		}
	}
	
	private DIDLContent didlForMusic() {
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
		
		return didl;
	}
	
	private DIDLContent didlForRoot() {
		DIDLContent didl = new DIDLContent();
		Container c = new Container(ID.MUSIC, ID.ROOT, "Music", "JZWMP", new DIDLObject.Class("object.container"), 3);
		didl.addContainer(c);
		return didl;
	}
	
	private DIDLContent didlForGenre() {
		JSONObject json;
		try {
			String base = upnpConfig.getJinzoraEndpoint();
			base += "&request=browse&output=json&resulttype=genre&jz_path=%2F";
			String jsonStr = contentFromURL(new URL(base));
			json = new JSONObject(jsonStr);
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON", e);
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Unknown error", e);
			return null;
		}
		
		DIDLContent didl = new DIDLContent();
		try {
			String parent = ID.MUSIC_GENRE;
			String creator = "JZWMP";
			JSONArray nodes = json.getJSONArray("nodes");
			for (int i = 0; i < nodes.length(); i++) {
				JSONObject a = nodes.getJSONObject(i);
				String id = a.getString("browse");
				String title = a.getString("name");
				int childCount = 0;
				MusicGenre genre = new MusicGenre(id, parent, title, creator, childCount);
				didl.addContainer(genre);
			}
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON", e);
			return null;
		}
		
		return didl;
	}

	private DIDLContent didlForArtist() {
		JSONObject json;
		try {
			String base = upnpConfig.getJinzoraEndpoint();
			base += "&request=browse&output=json&resulttype=artist&jz_path=%2F";
			String jsonStr = contentFromURL(new URL(base));
			json = new JSONObject(jsonStr);
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON", e);
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Unknown error", e);
			return null;
		}
		
		DIDLContent didl = new DIDLContent();
		try {
			String parent = ID.MUSIC_ARTIST;
			String creator = "JZWMP";
			JSONArray nodes = json.getJSONArray("nodes");
			for (int i = 0; i < nodes.length(); i++) {
				JSONObject a = nodes.getJSONObject(i);
				String id = a.getString("browse");
				String title = a.getString("name");
				int childCount = 0;
				MusicArtist artist = new MusicArtist(id, parent, title, creator, childCount);
				didl.addContainer(artist);
			}
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON", e);
			return null;
		}
		
		return didl;
	}

	private DIDLContent didlForAlbum(BrowseParameters params) {
		JSONObject json;
		try {
			String base = upnpConfig.getJinzoraEndpoint();
			base += "&request=browse&output=json&resulttype=album&jz_path=%2F";
			if (params.startingIndex != 0 || params.requestedCount != 0) {
				base += "&offset=" + params.startingIndex + "&limit=" + params.requestedCount;
			}
			String jsonStr = contentFromURL(new URL(base));
			json = new JSONObject(jsonStr);
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON", e);
			return null;
		} catch (Exception e) {
			Log.e(TAG, "Unknown error", e);
			return null;
		}
		
		DIDLContent didl = new DIDLContent();
		try {
			String parent = ID.MUSIC_ALBUM;
			String creator = "JZWMP";
			JSONArray nodes = json.getJSONArray("nodes");
			for (int i = 0; i < nodes.length(); i++) {
				JSONObject a = nodes.getJSONObject(i);
				String id = a.getString("browse");
				String title = a.getString("name");
				String thumbnail = a.optString("image");
				
				int childCount = 0;
				MusicAlbum album = new MusicAlbum(id, parent, title, creator, childCount);
				if (thumbnail != null && thumbnail.length() > 0) {
		        	try {
		        		URI uri = new URI(thumbnail);
		        		ALBUM_ART_URI albumArt = new ALBUM_ART_URI(uri);
		        		album.addProperty(albumArt);
		        	} catch (URISyntaxException e) {
		        		Log.w(TAG, "Found album art but bad URI", e);
		        	}
		        }
				
				didl.addContainer(album);
			}
		} catch (JSONException e) {
			Log.e(TAG, "Invalid JSON", e);
			return null;
		}
		
		return didl;
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
		/*
		 * {
		 * "image":"..." [full size]
		 * "thumbnail":"..." [75x75]
		 * "name":"Vaka",
		 * "album":"Hvarf-Heim",
		 * "artist":"Sigur Ros",
		 * "genre":"Alternative",
		 * "playlink":"...", [m3u]
		 * "download":"..." [mp3]
		 * "metadata":
		 *   {"title":"Vaka","bitrate":"192","frequency":"44.1",
		 *   "filename":"03 Voka.mp3","size":"7.76","year":"2007",
		 *   "comment":"","length":"321","number":"03","genre":"-",
		 *   "artist":null,"album":"Hvarf-Heim [Disc Two] \"Heim\"","lyrics":""
		 *   ,"type":"mp3"},
		 * "path":"Alternative\/Sigur Ros\/Hvarf-Heim\/03 Voka.mp3",
		 * "type":"Track"
		 * }
		 */
		for (int i = 0; i < browse.length(); i++) {
			try {
				JSONObject track = browse.getJSONObject(i);
				MusicTrack musicTrack = jsonToMusicTrack(track);
		        didl.addItem(musicTrack);
			} catch (Exception e) {
				Log.w(TAG, "Bad json entry", e);
			}
		}
	}
	
	private MusicTrack jsonToMusicTrack(JSONObject track) {
		String genre = track.optString("genre");
		String album = track.optString("album");
        String creator = track.optString("artist"); // Required
        PersonWithRole artist = new PersonWithRole(creator, "Performer");
        MimeType mimeType = new MimeType("audio", "mpeg");
        
        String trackId = track.optString("id");
        if (trackId == null || trackId.length() == 0) {
        	trackId = track.optString("path");
        }
        String parentId = "0";
        String trackTitle = track.optString("name");
        String trackUrl = track.optString("download");
        String duration = "";
        String thumbnail = track.optString("image");
        	// TODO: bigger image in "thumbnail" (75x75 too small)
        long bitrate = 0;
        long size = 0;
        Integer trackNumber = null;
        
        if (track.has("metadata")) {
        	JSONObject meta = track.optJSONObject("metadata");
        	if (meta.has("bitrate")) {
        		try {
	        		double br = Double.parseDouble(meta.getString("bitrate"));
	        		bitrate = Math.round(br*128);
        		} catch (Exception e) {}
        	}
        	if (meta.has("size")) {
        		try {
	        		double sz = Double.parseDouble(meta.getString("size"));
	        		size = Math.round(sz*1024*1024);
        		} catch (Exception e) {}
        	}
        	if (meta.has("length")) {
        		try {
	        		int len = Integer.parseInt(meta.getString("length"));
	        		duration += (len / 60);
	        		len = (len % 60);
	        		if (len == 0) {
	        			duration += ":00";
	        		} else if (len < 10) {
	        			duration += ":0" + len;
	        		} else {
	        			duration += ":" + len;
	        		}
        		} catch (Exception e) {}
        	}
        	if (meta.has("number")) {
        		try {
        			trackNumber = Integer.parseInt(meta.optString("number"));
        		} catch (Exception e) {}
        	}
        }
        MusicTrack musicTrack = new MusicTrack(
                trackId, parentId,
                trackTitle,
                creator, album, artist,
                new Res(mimeType, size, duration, bitrate, trackUrl
    		));
        
        if (trackNumber != null) {
        	musicTrack.setOriginalTrackNumber(trackNumber);
        }
        if (thumbnail != null && thumbnail.length() > 0) {
        	try {
        		URI uri = new URI(thumbnail);
        		ALBUM_ART_URI albumArt = new ALBUM_ART_URI(uri);
        		musicTrack.addProperty(albumArt);
        	} catch (URISyntaxException e) {
        		Log.w(TAG, "Found album art but bad URI", e);
        	}
        }
        if (genre != null && genre.length() > 0) {
        	GENRE propGenre = new GENRE(genre);
        	musicTrack.addProperty(propGenre);
        }
        
        return musicTrack;
	}
}