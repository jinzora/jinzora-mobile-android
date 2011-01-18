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
import org.teleal.cling.binding.annotations.UpnpAction;
import org.teleal.cling.binding.annotations.UpnpInputArgument;
import org.teleal.cling.binding.annotations.UpnpOutputArgument;
import org.teleal.cling.binding.annotations.UpnpStateVariable;
import org.teleal.cling.binding.annotations.UpnpStateVariables;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.model.types.csv.CSV;
import org.teleal.cling.model.types.csv.CSVString;
import org.teleal.cling.protocol.sync.ReceivingAction;
import org.teleal.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.teleal.cling.support.contentdirectory.ContentDirectoryErrorCode;
import org.teleal.cling.support.contentdirectory.ContentDirectoryException;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.PersonWithRole;
import org.teleal.cling.support.model.Protocol;
import org.teleal.cling.support.model.ProtocolInfo;
import org.teleal.cling.support.model.Res;
import org.teleal.cling.support.model.SortCriterion;
import org.teleal.cling.support.model.WriteStatus;
import org.teleal.cling.support.model.DIDLObject.Property.UPNP.ALBUM_ART_URI;
import org.teleal.cling.support.model.DIDLObject.Property.UPNP.GENRE;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.container.StorageFolder;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.common.util.MimeType;

import android.util.Log;

public class JinzoraContentDirectory extends AbstractContentDirectoryService {
	private static final String TAG = "jinzora";
	private static final String CREATOR = "jinzora";
	private static final DIDLObject.Class CONTAINER_CLASS = new DIDLObject.Class("object.container");
	final private CSV<String> mSearchCapabilities;
	private final UpnpConfiguration mConfig = UpnpService.getConfig();
	
	private final JinzoraApi mApi = new JinzoraApi(mConfig);
	
    public static final class DLNAFlags {
  	  public static final int SENDER_PACED               = (1 << 31);
  	  public static final int TIME_BASED_SEEK            = (1 << 30);
  	  public static final int BYTE_BASED_SEEK            = (1 << 29);
  	  public static final int FLAG_PLAY_CONTAINER        = (1 << 28);
  	  public static final int S0_INCREASE                = (1 << 27);
  	  public static final int SN_INCREASE                = (1 << 26);
  	  public static final int RTSP_PAUSE                 = (1 << 25);
  	  public static final int STREAMING_TRANSFER_MODE    = (1 << 24);
  	  public static final int INTERACTIVE_TRANSFERT_MODE = (1 << 23);
  	  public static final int BACKGROUND_TRANSFERT_MODE  = (1 << 22);
  	  public static final int CONNECTION_STALL           = (1 << 21);
  	  public static final int DLNA_V15                   = (1 << 20);
  	  
  	  public static final String TRAILING_ZEROS = "000000000000000000000000";
  }
	
	public JinzoraContentDirectory() {
		super();
		
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
	   
	   return super.search(containerId, searchCriteria, filter, firstResult, maxResults, orderBy);
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
			content = JinzoraApi.contentFromURL(url);
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
		didl.addItem(JinzoraApi.jsonToMusicTrack(json));
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
			String content = JinzoraApi.contentFromURL(url);
			int totalMatches = -1;
			
			if (isHome) {
				JSONArray home = new JSONArray(content);
				mApi.addDidlNodes(didl, home);
			} else {
				JSONObject obj = new JSONObject(content);
				JSONArray nodes = obj.optJSONArray("nodes");
				
				if (nodes != null) {
					mApi.addDidlNodes(didl, nodes);
				}
				nodes = obj.optJSONArray("tracks");
				if (nodes != null) {
					mApi.addDidlTracks(didl, nodes);
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