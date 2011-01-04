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
	@Override
	public BrowseResult browse(String arg0, BrowseFlag arg1, String arg2,
			long arg3, long arg4, SortCriterion[] arg5)
			throws ContentDirectoryException {
		
		String ua = ReceivingAction.getRequestMessage().getHeaders().getFirstHeader("User-agent");
		if (ua != null && ua.toLowerCase().contains("windows-media-player")) {
			Log.d(TAG, "Browsing with WMP compat");
			return wmpService.browse(arg0, arg1, arg2, arg3, arg4, arg5);
		}
		
		Log.d(TAG, "Browsing default compat");
		return defaultService.browse(arg0, arg1, arg2, arg3, arg4, arg5);
	}
	
	private AbstractContentDirectoryService wmpService = new WMPContentDirectory();
	private AbstractContentDirectoryService defaultService = new JinzoraContentDirectory();
}
