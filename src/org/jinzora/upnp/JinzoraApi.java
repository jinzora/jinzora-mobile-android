package org.jinzora.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.PersonWithRole;
import org.teleal.cling.support.model.Protocol;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.teleal.cling.support.model.DIDLObject.Property.UPNP.GENRE;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.container.MusicAlbum;
import org.teleal.cling.support.model.container.MusicArtist;
import org.teleal.cling.support.model.container.MusicGenre;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.common.util.MimeType;

import android.util.Log;

public class JinzoraApi {
	public static final String TAG = "jinzora";
	private final UpnpConfiguration mConfig;
	
	private static final DIDLObject.Class CONTAINER_CLASS = new DIDLObject.Class("object.container");
	// artist has a bug in cling.
	private static final DIDLObject.Class ARTIST_CLASS = new DIDLObject.Class("object.container.person.musicArtist");
	private static final DIDLObject.Class PLAYLIST_CLASS = new DIDLObject.Class("object.container.playlistContainer");
	
	public JinzoraApi(UpnpConfiguration config) {
		mConfig = config;
	}
	
	public BrowseResult getAllGenres(long offset, long limit) {
		return getResultForType("genre", offset, limit);
	}

	public BrowseResult getAllArtists(long offset, long limit) {
		return getResultForType("artist", offset, limit);
	}
	
	public BrowseResult getAllAlbums(long offset, long limit) {
		return getResultForType("album", offset, limit);
	}
	
	public BrowseResult getAllTracks(long offset, long limit) {
		return getResultForType("track", offset, limit);
	}
	
	public BrowseResult getAlbumsForArtist(String artist, long offset, long limit) {
		String base = mConfig.getJinzoraEndpoint();
		if (base == null) {
			Log.w(TAG, "No jinzora service configured");
			return null;
		}
		
		String query = "";
		try {
			query = URLEncoder.encode("@album @artist " + artist + " @limit " + 99999, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Ignored
		}
		
		URL url;
		try {
			base += "&request=search&output=json&&query=" + query;
			base += "&jz_path="+URLEncoder.encode("/","UTF-8");
			url = new URL(base);
		} catch (Exception e) {
			return null;
		}
		
		return getResultForUrl(url);
	}
	
	private BrowseResult getResultForType(String type, long offset, long limit) {
		String base = mConfig.getJinzoraEndpoint();
		if (base == null) {
			Log.w(TAG, "No jinzora service configured");
			return null;
		}
		
		URL url;
		try {
			base += "&request=browse&output=json&resulttype=" + type;
			base += "&jz_path="+URLEncoder.encode("/","UTF-8");
			base += "&limit="+ limit + "&offset=" + offset;
			url = new URL(base);
		} catch (Exception e) {
			return null;
		}
		
		return getResultForUrl(url);
	}
	
	private BrowseResult getResultForUrl(URL url) {
		String content;
		try {
			content = contentFromURL(url);
		} catch (IOException e) {
			Log.e(TAG, "Error grabbing json", e);
			return null;
		}
		
		DIDLContent didl;
		int totalMatches = -1;
		int size = 0;
		try {
			didl = new DIDLContent();
			JSONObject obj = new JSONObject(content);
			JSONArray nodes = obj.optJSONArray("nodes");
			if (nodes != null) {
				size = nodes.length();
				addDidlNodes(didl, nodes);
			}
			
			nodes = obj.optJSONArray("tracks");
			if (nodes != null) {
				size += nodes.length();
				addDidlTracks(didl, nodes);
			}
			
			JSONObject meta = obj.optJSONObject("meta");
			if (meta != null) {
				if (meta.has("totalMatches")) {
					totalMatches = Integer.parseInt(meta.getString("totalMatches"));
				}
			}
			if (totalMatches == -1) {
				totalMatches = size;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error creating didl", e);
			return null;
		}
		
		try {
			Log.d(TAG, "search returned " + size);
			String result = new DIDLParser().generate(didl);
			Log.d(TAG, "returning didl.");
			return new BrowseResult(result, size, totalMatches);
		} catch (Exception e) {
			Log.e(TAG, "Error generating didl", e);
			return null;
		}
	}
	
	public static MusicTrack jsonToMusicTrack(JSONObject track) {
		String genre = track.optString("genre");
		String album = track.optString("album");
        String creator = track.optString("artist"); // CREATOR; // Required
        String artistStr = track.optString("artist");
        PersonWithRole artist = new PersonWithRole(artistStr, "Performer");
        MimeType mimeType = new MimeType("audio", "mpeg");
        
        String trackId = track.optString("id");
        if (trackId == null || trackId.length() == 0) {
        	trackId = track.optString("path");
        }
        String parentId = "0";
        String trackTitle = track.optString("name");
        String trackUrl = track.optString("download") + "&extension=file.mp3";
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

        String flagsStr = "01700000000000000000000000000000";
        String network = ProtocolInfo.WILDCARD;
        String additionalInfo = "DLNA.ORG_PN=MP3;DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=" + flagsStr;
        
        ProtocolInfo protocolInfo = new ProtocolInfo(Protocol.HTTP_GET, network, mimeType.toString(), additionalInfo);
        Res res = new Res(protocolInfo, null /*size*/, trackUrl);
        res.setBitrate(bitrate);
        res.setDuration(duration);
        
        MusicTrack musicTrack = new MusicTrack(
                trackId, parentId,
                trackTitle,
                creator, album, artist,
                res
                );
        
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
	
	public static String contentFromURL(URL url) throws IOException {
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
	
	public void addDidlNodes(DIDLContent didl, JSONArray browse) throws JSONException {
		for (int i = 0; i < browse.length(); i++) {
			JSONObject obj = browse.getJSONObject(i);
			String url = obj.getString("browse");
			String label = obj.getString("name");
			String thumbnail = obj.optString("image");
			String type = obj.optString("type");
			
			Integer subCount = Integer.parseInt(obj.optString("count", "10"));

			// Hack so we can easily get container label in request for metadata.
			try {
				url += "&label=" + URLEncoder.encode(label,"UTF-8");
			} catch (UnsupportedEncodingException e) {}
			
			
			// TODO: Use Album/Artist containers where appropriate. Check "type" field.
			//Container folder = new StorageFolder(url,"0",label,"Me", subCount, storageUsed);
			DIDLObject.Class containerClass;
			if (type == null) type = "";
				
			type = type.toLowerCase();
			if (type.equals("album")) {
				containerClass = MusicAlbum.CLASS;
			} else if (type.equals("artist")) {
				containerClass = ARTIST_CLASS;
			} else if (type.equals("genre")) {
				containerClass = MusicGenre.CLASS;
			} else {
				containerClass = CONTAINER_CLASS;
			}
			
			Container folder = new Container(url, "0", label, "System", containerClass, subCount);
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
	
	public void addDidlTracks(DIDLContent didl, JSONArray browse) {
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
