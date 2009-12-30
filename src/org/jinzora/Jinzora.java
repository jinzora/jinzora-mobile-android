package org.jinzora;



import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;

import org.jinzora.playback.PlaybackInterface;
import org.jinzora.playback.PlaybackService;
import org.jinzora.playback.PlaybackServiceConnection;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.google.zxing.integration.IntentIntegrator;
import com.google.zxing.integration.IntentResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TabHost;

public class Jinzora extends TabActivity {
	
	
	protected class MenuItems { 
		final static int HOME = 1;
		final static int QUIT = 2;
		final static int ADDWHERE = 3;
		final static int SCAN = 4;
		final static int SEARCH = 5;
	}
	
	protected class RequestCodes {
		final static int SCAN_EVENT = 0;
	}
	
	private static Jinzora instance = null;
	private static SharedPreferences preferences = null;
	private static String baseurl;
	
	private static boolean sServiceStarted = false;
    public static PlaybackServiceConnection sPbConnection = new PlaybackServiceConnection();
	
	private static String[] addTypes = {"Replace current playlist","End of list","After current track"};
    private static int selectedAddType = 0;
	private static DialogInterface.OnClickListener addTypeClickListener 
		= new DialogInterface.OnClickListener () {
			@Override
			public void onClick(DialogInterface dialog, int pos) {
				if (pos == selectedAddType) return;
				try {
					Jinzora.setAddType(pos);
					selectedAddType = pos;
					dialog.dismiss();
				} catch (Exception e) {
					Log.e("jinzora","Error setting add-type",e);
				}
			}
		};
    
    
	public static void resetBaseURL() {
		baseurl = null;
	}
	
	public static String getBaseURL() {
		if (baseurl == null) {
    		setBaseURL(preferences);
    	}
		return baseurl;
	}
	
	public static void setAddType(int type) {
		selectedAddType = type;
	}
	
	public static int getAddType() {
		return selectedAddType;
	}
	
	private static void setBaseURL(SharedPreferences preferences) {
		String site = preferences.getString("site", null);
		String username = preferences.getString("username",null);
		String password = preferences.getString("password",null);
		
		if (site == null || username == null || password == null) {
			baseurl = null;
		}
		
		baseurl = site + "/api.php?user=" + username;
		
		// disable until enough people upgrade.
		boolean pwPrehash=true;
		if (pwPrehash) {
			try {
			    MessageDigest md5=MessageDigest.getInstance("MD5");
			    md5.update(password.getBytes(),0,password.length());
			    
			    baseurl += "&pass=" + new BigInteger(1,md5.digest()).toString(16);
				baseurl += "&pw_hashed=true";
			} catch (Exception e) {
				Log.w("jinzora","Error computing password hash");
				baseurl += "&pass=" + password;
			}
		} else {
			baseurl += "&pass=" + password;
		}
		
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
	public void onResume() {
		super.onResume();
		Intent bindIntent = new Intent(this,PlaybackService.class);
		bindService(bindIntent,sPbConnection,BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(sPbConnection);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		/* Start the playback service */
    	if (!sServiceStarted) {
    		startService(new Intent(this, PlaybackService.class));
    		sServiceStarted = true;
    	}
		
		instance = this;
		//playbackBinding = sPbConnection.playbackBinding;
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
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
	        
	        Intent intBrowse = new Intent(this,Browser.class);
	        
	        
	        final Intent queryIntent = getIntent();
	        final String queryAction = queryIntent.getAction();
	        if (Intent.ACTION_SEARCH.equals(queryAction)) {
	        	String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
	        	//Log.d("jinzora","got query " + queryString);
	        	
	        	
	        	// Track history
	        	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
	                    SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);
	            suggestions.saveRecentQuery(queryString, null);
	            
	        	
	            // look for keywords
	        	boolean quickplay=false;
	        	if (queryString != null && queryString.startsWith("play ")) {
	        		quickplay=true;
	        		queryString=queryString.substring(5);
	        	}
	        	
	        	// TODO: This does not work with the server search
	        	// parameters. Format in a way that does, preferably using different fields.
	        	//Log.d("jinzora","unformatted query: " + queryString);
	        	if (queryString.contains(" by artist ")) {
	        		queryString.replace(" by artist ", " @artist ");
	        	} else if (queryString.contains(" artist ")) {
	        		queryString.replace(" artist ", " @artist " );
	        	} else if (queryString.startsWith("artist ")) {
	        		queryString = "@artist " + queryString.substring(7);
	        	} else if (queryString.contains(" by ")) {
	        		queryString.replace(" by ", " @artist ");
	        	}
	        	
	        	if (queryString.contains(" album ")) {
	        		queryString.replace(" album ", " @album " );
	        	} else if (queryString.startsWith("album ")) {
	        		queryString = "@album " + queryString.substring(6);
	        	}
	        	
	        	//Log.d("jinzora","reformatted query: " + queryString + " (quickplay " + quickplay + ")");
	        	
	            
	            // Handle search
	            
	            // Quick search
	            if (quickplay) {
	            	this.quickplay(queryString);
	            	finish();
	            } else {
		            // Open Browse activity
		            intBrowse.putExtra(getPackageName()+".browse",
		            		 			Jinzora.getBaseURL()+"&request=search&query="+URLEncoder.encode(queryString, "UTF-8"));
	            }
	        } else if (getIntent() != null && getIntent().getExtras() != null) {
	        	intBrowse.putExtras(getIntent().getExtras());
	        }

	        host.addTab(host.newTabSpec("browse")
	        		.setIndicator(getString(R.string.browser)/*,icon.getDrawable()*/)
	        		.setContent(intBrowse));
	        
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
	        
	       
	        /*
	        if (AndroidJunctionMaker.getInstance().isJoinable(this)) {
	        	Log.d("jinzora","setting playback target from Junction");
	        	this.sPbConnection.playbackBinding
	        		.setPlaybackDevice(JunctionBox.class.getName(), 
	        							AndroidJunctionMaker.getInstance().getInvitationForActivity(this).toString());
	        }
	        */
	        
        } catch (Exception e) {
        	Log.e("jinzora", "error", e);
        }
        
    }    

    protected static void initContext(Activity activity) {
    	activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
    
    protected static boolean createMenu(Menu menu) {
    	menu.add(0,MenuItems.HOME,1,R.string.home)
    	.setIcon(android.R.drawable.ic_menu_revert)
    	.setAlphabeticShortcut('h');
    	
    	menu.add(0,MenuItems.ADDWHERE,2,"Queue Mode")
    	.setIcon(android.R.drawable.ic_menu_add)
    	.setAlphabeticShortcut('a');
    	
    	menu.add(0,MenuItems.SEARCH,3,R.string.search)
    	.setIcon(android.R.drawable.ic_search_category_default)
    	.setAlphabeticShortcut(SearchManager.MENU_KEY);
    	
    	menu.add(0,MenuItems.QUIT,3,R.string.quit)
    	.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
    	.setAlphabeticShortcut('q');
    	
    	/*
    	menu.add(0,MenuItems.SCAN,3,R.string.scan)
    	.setIcon(android.R.drawable.ic_menu_search)
    	.setAlphabeticShortcut('s');
    	*/
    	
    	return true;
    }
    
    protected static void menuItemSelected(int featureId, MenuItem item, Activity activity) {
    	switch (item.getItemId()) {
    	case MenuItems.HOME:
    		Browser.clearBrowsing();
    		Intent goHome = 
    			new Intent(instance,Jinzora.class);
    					//.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    					//.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		instance.startActivity(goHome);
    		break;
    		
    	case MenuItems.SCAN:
    		IntentIntegrator.initiateScan(instance);
    		break;
    		
    	case MenuItems.QUIT:
    		try {
    			Browser.clearBrowsing();
    			
    			Intent etphonehome = new Intent(Intent.ACTION_MAIN);
    			etphonehome.addCategory(Intent.CATEGORY_HOME);
    			instance.startActivity(etphonehome);
    			
    			//activity.finish();
    		} catch (Exception e) {
    			
    		}
    		break;
    		
    	case MenuItems.ADDWHERE:
    		
    		ArrayAdapter<String> addTypeAdapter = new ArrayAdapter<String>(activity,
    				android.R.layout.simple_spinner_dropdown_item,
    		        addTypes );
    		
    		AlertDialog dialog = 
    			new AlertDialog.Builder(activity)
    				.setSingleChoiceItems(addTypeAdapter, selectedAddType, addTypeClickListener)
    				.setTitle(R.string.add_to)
    				.create();
    			
    			dialog.show();
    		break;
    	case MenuItems.SEARCH:
    		activity.onSearchRequested();
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
	        		 sPbConnection.playbackBinding.playlist( xpp.getText(), Jinzora.getAddType() );
	        		 return;
	        	 }
	        	 eventType = xpp.next();
	         }
	    	
	    	
		} catch (Exception e) {
			Log.e("jinzora","Error during quicksearch",e);
		}
	}
}