package org.jinzora.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.types.csv.CSV;
import org.teleal.cling.model.types.csv.CSVString;
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
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.common.util.MimeType;

import android.util.Log;

public class JinzoraContentDirectory extends AbstractContentDirectoryService {
	private static final String TAG = "jinzora";
	private static final DIDLObject.Class CONTAINER_CLASS = new DIDLObject.Class("object.container");
	final private CSV<String> mSearchCapabilities;
	private final UpnpConfiguration mConfig;
	
	public JinzoraContentDirectory() {
		super();
		
		mConfig = new UpnpConfiguration(UpnpService.getContext());
		mSearchCapabilities = new CSVString();
		//mSearchCapabilities.add("dc:title");
		//mSearchCapabilities.add("upnp:class");
		//mSearchCapabilities.add("upnp:artist");
	}
	
	@Override
	public CSV<String> getSearchCapabilities() {
        return mSearchCapabilities;
    }
	
	public List<String> getJinzoraSortCaps() {
		List<String> caps = new ArrayList<String>();
		return caps;
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
		/*Log.d(TAG, "request: " + ReceivingAction.getRequestMessage().getBodyString());
		Log.d(TAG, "action: " + ReceivingAction.getRequestMessage().getAction());
		Log.d(TAG, "headers: " + ReceivingAction.getRequestMessage().getHeaders().toString());
		Log.d(TAG, "extras: " + ReceivingAction.getExtraResponseHeaders().size());
		Log.d(TAG, "");
		*/
		
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
	
	@Override
    public BrowseResult search(String containerId, String searchCriteria, String filter, 
    		long firstResult, long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException
             {
   
	   Log.d(TAG, "searching.");
	   Log.d(TAG, "container: " + containerId);
	   Log.d(TAG, "criteria: " + searchCriteria);
	   Log.d(TAG, "filter: " + filter);
	   Log.d(TAG, "index / request: " + firstResult + ", " + maxResults);
	   // UPnPlay
	   // searching: 0: (upnp:class derivedfrom "object.item.audioItem") and (dc:title contains "oy")
	   // searching: 0: (upnp:class derivedfrom "object.container.person.musicArtist") and (upnp:artist contains "pearl jam")
	   /*
	    * 01-02 19:01:37.483: DEBUG/jinzora(1409): container: 0
01-02 19:01:37.483: DEBUG/jinzora(1409): criteria: (upnp:class derivedfrom "object.item.audioItem") and (dc:title contains "lazy")
01-02 19:01:37.483: DEBUG/jinzora(1409): filter: *
01-02 19:01:37.483: DEBUG/jinzora(1409): index / request: 0, 100
	    */
	   
	   // WMP
	   // searching: 0: upnp:class derivedfrom "object.container.playlistContainer" and @refID exists false
	   // searching: 0: upnp:class derivedfrom "object.item.audioItem" and @refID exists false
	   
	   /*
	    * 01-02 19:02:27.123: DEBUG/jinzora(1409): container: 0
			01-02 19:02:27.123: DEBUG/jinzora(1409): criteria: upnp:class derivedfrom "object.item.audioItem" and @refID exists false
			01-02 19:02:27.123: DEBUG/jinzora(1409): filter: *
			01-02 19:02:27.123: DEBUG/jinzora(1409): index / request: 0, 200
	    */
	   
	   // TODO
	   // special case: only WMP scraping supported right now.
	   if (searchCriteria == null || searchCriteria.equalsIgnoreCase("upnp:class derivedfrom \"object.item.audioItem\" and @refID exists false")) {
		   return trackSlice(firstResult, maxResults);
	   }
	   
	   return null;
   }
   
   private BrowseResult trackSlice(long offset, long limit) {
		String base = mConfig.getJinzoraEndpoint();
		if (base == null) {
			Log.w(TAG, "No jinzora service configured");
			return null;
		}
		
		URL url;
		try {
			base += "&request=browse&output=json&resulttype=track";
			base += "&jz_path="+URLEncoder.encode("/","UTF-8");
			base += "&limit="+ limit + "&offset=" + offset;
			url = new URL(base);
		} catch (Exception e) {
			return null;
		}
		
		String content;
		try {
			content = contentFromURL(url);
		} catch (IOException e) {
			Log.e(TAG, "Error grabbing json", e);
			return null;
		}
		
		DIDLContent didl;
		int size = 0;
		try {
			didl = new DIDLContent();
			JSONObject obj = new JSONObject(content);
			JSONArray nodes = obj.optJSONArray("tracks");
			size = nodes.length();
			if (nodes != null) {
				addDidlTracks(didl, nodes);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error creating didl", e);
			return null;
		}
		
		try {
			Log.d(TAG, "search returned " + size);
			return new BrowseResult(new DIDLParser().generate(didl), size, 20000);
		} catch (Exception e) {
			Log.e(TAG, "Error generating didl", e);
			return null;
		}
   }
	
	private DIDLContent getMetadataDidl(String objectId) {
		// TODO: currently only supports tracks and root container.
		DIDLContent didl = new DIDLContent();
		
		String urlStr = mConfig.getJinzoraEndpoint();
		if (urlStr == null) {
			Log.w(TAG, "No jinzora service configured");
			return didl;
		}
		
		if ("0".equals(objectId)) {
			// PS3 requests metadata of root.
			Container root = new Container("0", "-1", "Root", "System", CONTAINER_CLASS, 1);
			//root.setWriteStatus(WriteStatus.NOT_WRITABLE);
			didl.addContainer(root);
			return didl;
		}
		
		if (null == objectId || objectId.startsWith("http://")) {
			// not a track, have to hack.
			Log.w(TAG, "Metadata requested for non-track.");
			String label = "Unknown";
			try {
				if (objectId.contains("label=")) {
					label = objectId.substring(objectId.indexOf("label=")+6);
					if (label.contains("&")) {
						label = label.substring(0,label.indexOf("&"));
					}
					label = URLDecoder.decode(label,"UTF-8");
				}
				else if (objectId.contains("jz_path=")) {
					label = objectId.substring(objectId.indexOf("jz_path=")+8);
					if (label.contains("&")) {
						label = label.substring(0,label.indexOf("&"));
					}
					if (label.contains("/")) {
						label = label.substring(label.indexOf("/")+1);
					}
					label = URLDecoder.decode(label,"UTF-8");
				}
			} catch (Exception e) {}
			
			Container root = new Container(objectId, "0", label, "System", CONTAINER_CLASS, 1);
			didl.addContainer(root);
			return didl;
		}
		
		urlStr += "&request=trackinfo&output=json&jz_path=" + objectId;
		URL url;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Bad URL", e);
			return didl;
		}
		String content;
		JSONObject json;
		try {
			content = contentFromURL(url);
		} catch (IOException e) {
			Log.e(TAG, "Error accessing api", e);
			return didl;
		}
		try {
			// gross api!
			json = new JSONObject(content);
			JSONArray tracks = json.getJSONArray("tracks");
			json = tracks.getJSONObject(0);
		} catch (JSONException e) {
			Log.e(TAG, "Return type not json", e);
			return didl;
		}
		didl.addItem(jsonToMusicTrack(json));
		return didl;
	}
	
	private BrowseResult getBrowseResult(BrowseParameters params) {
		String base;
		boolean isHome = true;
		
		String objectId = params.objectId;
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
		
		if (params.startingIndex != 0 || params.requestedCount != 0) {
			base += "&offset=" + params.startingIndex + "&limit=" + params.requestedCount;
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
			int totalMatches = -1;
			
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
				JSONObject meta = obj.optJSONObject("meta");
				if (meta != null) {
					if (meta.has("totalMatches")) {
						totalMatches = Integer.parseInt(meta.getString("totalMatches"));
					}
				}
			}

			try {
				int count = didl.getContainers().size() + didl.getItems().size();
				if (totalMatches == -1) totalMatches = count;
				Log.d(TAG, "response: " + new DIDLParser().generate(didl));
				return new BrowseResult(new DIDLParser().generate(didl), count, totalMatches);
			} catch (Exception e) {
				Log.e(TAG, "Error producing BrowseResult", e);
				return null;
			}
		} catch (IOException e) {
			Log.w(TAG, "Error reading content", e);
			return null;
		} catch (JSONException e) {
			Log.d(TAG, "Json content not found", e);
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
			String thumbnail = obj.optString("image");
			Integer subCount = Integer.parseInt(obj.optString("count", "10"));

			// Hack so we can easily get container label in request for metadata.
			try {
				url += "&label=" + URLEncoder.encode(label,"UTF-8");
			} catch (UnsupportedEncodingException e) {}
			
			
			// TODO: Use Album/Artist containers where appropriate. Check "type" field.
			//Container folder = new StorageFolder(url,"0",label,"Me", subCount, storageUsed);
			Container folder = new Container(url, "0", label, "System", CONTAINER_CLASS, subCount);
			String playlist = obj.optString("playlink");
			if (playlist != null && playlist.length() > 0) {
				MimeType mimeType = new MimeType("audio", "m3u");
				Res res = new Res();
				res.setProtocolInfo(new ProtocolInfo(mimeType));
				res.setValue(playlist);
				folder.addResource(res);
				
				if (thumbnail != null && thumbnail.length() > 0) {
		        	try {
		        		URI uri = new URI(thumbnail);
		        		ALBUM_ART_URI albumArt = new ALBUM_ART_URI(uri);
		        		// TODO: DLNA requires xml attribute:
		        		// dlna:profileID="JPEG_TN" for jpeg thumbnails.
		        		folder.addProperty(albumArt);
		        	} catch (URISyntaxException e) {
		        		Log.w(TAG, "Found album art but bad URI", e);
		        	}
		        }
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
}