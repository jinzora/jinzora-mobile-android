package org.jinzora.upnp;

import org.teleal.cling.protocol.sync.ReceivingAction;
import org.teleal.cling.support.contentdirectory.AbstractContentDirectoryService;
import org.teleal.cling.support.contentdirectory.ContentDirectoryException;
import org.teleal.cling.support.model.BrowseFlag;
import org.teleal.cling.support.model.BrowseResult;
import org.teleal.cling.support.model.SortCriterion;

import android.util.Log;

/**
 * A compatibility layer for managing ContentDirectory requests.
 *
 */
public class CompatContentDirectory extends AbstractContentDirectoryService {
	public static final String TAG = "jinzora";
	
	protected AbstractContentDirectoryService getDirectoryImpl() {
		String ua = ReceivingAction.getRequestMessage().getHeaders().getFirstHeader("User-agent");
		Log.d(TAG, "agent: " + ua);
		if (ua != null) {
			ua = ua.toLowerCase();
			if (ua.contains("windows-media-player")
					|| ua.contains("xbox")) {
				
				Log.d(TAG, "using WMP compat");
				return wmpService;
			}
		}
		
		Log.d(TAG, "using default compat");
		return defaultService;
	}
	
	@Override
	public BrowseResult browse(String arg0, BrowseFlag arg1, String arg2,
			long arg3, long arg4, SortCriterion[] arg5)
			throws ContentDirectoryException {
		
		return getDirectoryImpl().browse(arg0, arg1, arg2, arg3, arg4, arg5);
	}
	
	@Override
	public BrowseResult search(String containerId, String searchCriteria,
			String filter, long firstResult, long maxResults,
			SortCriterion[] orderBy) throws ContentDirectoryException {

		return getDirectoryImpl().search(containerId, searchCriteria, filter, firstResult,
				maxResults, orderBy);
	}
	
	private AbstractContentDirectoryService wmpService = new WMPContentDirectory();
	private AbstractContentDirectoryService defaultService = new JinzoraContentDirectory();
}
