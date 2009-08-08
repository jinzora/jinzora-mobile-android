package org.jinzora;



import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jinzora.playback.PlaybackInterface;
import org.jinzora.playback.PlaybackService;
import org.jinzora.playback.PlaybackServiceConnection;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class Jinzora extends TabActivity {
	protected class MenuItems { 
		final static int HOME = 1;
		final static int QUIT = 2;
		final static int SCAN = 3;	
	}
	
	protected class RequestCodes {
		final static int SCAN_EVENT = 0;
	}
	
	private static int instanceCount = 0;
	private static Jinzora instance = null;
	private static SharedPreferences preferences = null;
	private static String baseurl;
	
	private static boolean sServiceStarted = false;
    private static PlaybackServiceConnection sPbConnection = new PlaybackServiceConnection();
	protected static PlaybackInterface playbackBinding;
	
	public static void resetBaseURL() {
		baseurl = null;
	}
	
	public static String getBaseURL() {
		if (baseurl == null) {
    		setBaseURL(preferences);
    	}
		return baseurl;
	}
	
	private static void setBaseURL(SharedPreferences preferences) {
		String site = preferences.getString("site", null);
		String username = preferences.getString("username",null);
		String password = preferences.getString("password",null);
		
		if (site == null || username == null || password == null) {
			baseurl = null;
		}
			
		baseurl = site + "/api.php?user=" + username + "&pass=" + password;
		if (sPbConnection != null) {
			try {
				sPbConnection.setBaseURL(baseurl);
			} catch (Exception e) {
				Log.e("jinzora","Problem setting baseurl in PlaybackService",e);
			}
		} else {
			Log.w("jinzora","Playback connection null when setting base URL");
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		instance = this;
		instanceCount++;
		
		this.bindService(new Intent(this,PlaybackService.class), sPbConnection, Context.BIND_AUTO_CREATE);
		playbackBinding = sPbConnection.playbackBinding;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		instanceCount--;
		if (instanceCount == 0) {
			this.unbindService(sPbConnection);
			playbackBinding = null;
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
        	/* Start the playback service */
        	if (!sServiceStarted) {
        		startService(new Intent(this, PlaybackService.class));
        		sServiceStarted = true;
        	}
        	
        	
        	instance = this;
        	this.bindService(new Intent(this,PlaybackService.class), sPbConnection, Context.BIND_AUTO_CREATE);
    		
    		if (preferences == null) {
        		preferences = getSharedPreferences("main", 0);
        	}
    		
    		if (baseurl == null) {
        		setBaseURL(preferences);
        	}
        	
        	setContentView(R.layout.tabs);
	        TabHost host = this.getTabHost();
	        
	        //ImageView icon = new ImageView(this);
	        //icon.setImageResource(android.R.drawable.ic_menu_compass);
	        host.addTab(host.newTabSpec("browse")
	        		.setIndicator(getString(R.string.browser)/*,icon.getDrawable()*/)
	        		.setContent(new Intent(this, Browser.class)));
	        
	        host.addTab(host.newTabSpec("playback")
	        		.setIndicator(getString(R.string.player))
	        		.setContent(new Intent(this, Player.class)));
	        
	        //icon.setImageResource(android.R.drawable.ic_menu_search);
	        host.addTab(host.newTabSpec("search")
	        		.setIndicator(getString(R.string.search)/*,icon.getDrawable()*/)
	        		.setContent(new Intent(this, Search.class)));
	        
	        host.addTab(host.newTabSpec("settings")
	        		.setIndicator(getString(R.string.settings))
	        		.setContent(new Intent(this, Preferences.class)));
	        
	       
        } catch (Exception e) {
        	Log.e("jinzora", "error", e);
        }
        
    }    

    
    protected static boolean createMenu(Menu menu) {
    	menu.add(0,MenuItems.HOME,1,R.string.home)
    	.setIcon(android.R.drawable.ic_menu_revert)
    	.setAlphabeticShortcut('h');
    	
    	menu.add(0,MenuItems.QUIT,2,R.string.quit)
    	.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
    	.setAlphabeticShortcut('q');
    	
    	menu.add(0,MenuItems.SCAN,3,R.string.scan)
    	.setIcon(android.R.drawable.ic_menu_search)
    	.setAlphabeticShortcut('s');
    	
    	return true;
    }
    
    protected static void menuItemSelected(int featureId, MenuItem item) {
    	switch (item.getItemId()) {
    	case MenuItems.HOME:
    		Browser.clearBrowsing();
    		instance.startActivity(new Intent(instance,Jinzora.class));
    		break;
    		
    	case MenuItems.SCAN:
    		IntentIntegrator.initiateScan(instance);
    		break;
    		
    	case MenuItems.QUIT:
    		try {
    			Browser.clearBrowsing();
    			playbackBinding.stop();
    			
    			Intent etphonehome = new Intent(Intent.ACTION_MAIN);
    			etphonehome.addCategory(Intent.CATEGORY_HOME);
    			instance.startActivity(etphonehome);
    			
    		} catch (Exception e) {
    			
    		}
    		break;
    	}
    }
    
    
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
    		 
        if (scanResult != null) {
    		String contents = scanResult.getContents().replace("\\\"", "\"");
            if ("QR_CODE".equals(scanResult.getFormatName())) {
            	JSONObject json = null;
            	
                try {
                	json = new JSONObject(contents);
                	// we have a JSON object: it's a jukebox.
                } catch (Exception e) {
                	Log.w("jinzora","JSON not found in QR Code: " + contents, e);
                	return;
                }	
                
                try {
                	String jburl = json.getString("host") + "?user="
                				 + URLEncoder.encode(json.getString("username")) + "&pass="
                				 + URLEncoder.encode(json.getString("password")) + "&jb_id="
                				 + URLEncoder.encode(json.getString("jb_id"));

                	sPbConnection.playbackBinding.setPlaybackDevice("org.jinzora.playback.players.ForeignJukeboxDevice",jburl);
                } catch (Exception e) {
                	Log.e("jinzora","Failed to set playback device",e);
                	return;
                }
            } else {
            	// maybe a CD?
            	
            	// hacky:
            	// double hacky cut&paste:
            	quickplay("@id " + contents);
            }            
        }
    }
    
    /**
     * Performs a search and sends the
     * result directly to the player.
     */
	private void quickplay(String query) {
		try {
			String urlStr = Jinzora.getBaseURL()+"&request=search&query="+URLEncoder.encode(query);
	    	URL url = new URL(urlStr);
	        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
	        //conn.setConnectTimeout(5000);
			InputStream inStream = conn.getInputStream();
			conn.connect();
	
			 XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	         factory.setNamespaceAware(true);
	         XmlPullParser xpp = factory.newPullParser();
	         xpp.setInput(inStream, conn.getContentEncoding());
	
	         int eventType = xpp.getEventType();
	         while (eventType != XmlPullParser.END_DOCUMENT) {
	        	 if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("playlink")) {
	        		 eventType = xpp.next();
	        		 
	        		 // found a match; play it.
	        		 sPbConnection.playbackBinding.playlist( xpp.getText() );
	        		 return;
	        	 }
	        	 eventType = xpp.next();
	         }
	    	
	    	
		} catch (Exception e) {
			Log.e("jinzora","Error during quicksearch",e);
		}
	}
    
}