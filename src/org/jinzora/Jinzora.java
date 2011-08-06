package org.jinzora;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.List;

import mobisocial.nfc.NdefFactory;
import mobisocial.nfc.Nfc;

import org.jinzora.android.R;
import org.jinzora.download.DownloadActivity;
import org.jinzora.playback.PlaybackInterface;
import org.jinzora.playback.PlaybackService;
import org.jinzora.playback.PlaybackService.Intents;
import org.json.JSONObject;

import com.google.zxing.integration.IntentIntegrator;
import com.google.zxing.integration.IntentResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TabHost;
import android.widget.TextView;

public class Jinzora extends TabActivity {
	public static final String PACKAGE = "org.jinzora.android";
	private static final String ASSET_WELCOME = "welcome.txt";
	private static final String TAG = "jinzora";
	static Nfc mNfc;
	
	protected class MenuItems { 
		final static int HOME = 1;
		final static int QUIT = 2;
		final static int ADDWHERE = 3;
		final static int SCAN = 4;
		final static int SEARCH = 5;
		final static int DOWNLOADS = 6;
	}
	
	protected class RequestCodes {
		final static int SCAN_EVENT = 0;
	}
	
	public static String EXTRA_SWITCH_TAB = "org.jinzora.switch_tab";
	
	protected static Jinzora instance = null;
	private static SharedPreferences sSessionPreferences = null;
	private static SharedPreferences sAppPreferences = null;
	
	private static String baseurl;
	
	private static boolean sServiceStarted = false;
    public static PlaybackServiceConnection sPbConnection = null;
    
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
    		setBaseURL(sSessionPreferences);
    	}
		return baseurl;
	}
	
	public static void setAddType(int type) {
		selectedAddType = type;
	}
	
	public static int getAddType() {
		return selectedAddType;
	}
	
	private boolean isFirstRun() {
		return (sAppPreferences.getAll().size() == 0 || sSessionPreferences.getString("site", null) == null);
	}
	
	private void handleFirstRun() {
		/*
		final String JZ_LIVE_SITE = "http://live.jinzora.org";
		Preferences.addProfile(sAppPreferences,JZ_LIVE_SITE,"","");
		Preferences.loadSettingsFromProfile(sAppPreferences, sSessionPreferences, JZ_LIVE_SITE);
		*/
	    //showDialog(R.id.launch_music_app);
	}
	
	private static CharSequence readAsset(Activity activity, String asset) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(activity.getAssets().open(asset)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null) buffer.append(line).append('\n');
            return buffer;
        } catch (IOException e) {
            return "";
        } finally {
        	if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }   
    }
	
	
	private static void setBaseURL(SharedPreferences preferences) {
		String site = preferences.getString("site", null);
		String username = preferences.getString("username",null);
		String password = preferences.getString("password",null);
		
		if (username != null && username.length() == 0) {
			username = null;
		}
		
		if (site == null) {
			Log.w("jinzora","no server specified.");
			return;
		}
		
		if (site.endsWith("/")) {
			site = site.substring(0, site.length()-1);
		}
		if (username == null) {
			baseurl = site + "/api.php?1=1";
		} else {
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
		mNfc.onResume(this);
		instance = this;
		Intent bindIntent = new Intent(this,PlaybackService.class);
		bindService(bindIntent, sPbConnection, BIND_AUTO_CREATE);
		
		String curTab = "browse";
		// View M3U?
        final Intent inboundIntent = getIntent();
        if (inboundIntent == null) {
            return;
        }
        if (Intent.ACTION_VIEW.equals(inboundIntent.getAction())) {
        	// hack!
        	new Thread() {
        		public void run() {
        			try {
        				Thread.sleep(5000);
        			} catch (InterruptedException e) {}
                	Jinzora.doPlaylist( inboundIntent.getData().toString(), Jinzora.getAddType() );
        		};
        	}.start();
        	curTab = "playback";
        }
        
        if (inboundIntent.hasExtra(EXTRA_SWITCH_TAB)) {
           curTab = inboundIntent.getStringExtra(EXTRA_SWITCH_TAB);
           inboundIntent.removeExtra(EXTRA_SWITCH_TAB);
        }

        getTabHost().setCurrentTabByTag(curTab);
        inboundIntent.setAction("");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mNfc.onPause(this);
		unbindService(sPbConnection);
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		//playbackBinding = sPbConnection.playbackBinding;
	}

	@Override
	protected void onNewIntent(Intent intent) {
	    mNfc.onNewIntent(this, intent);
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPbConnection = new PlaybackServiceConnection();
        instance = this;
        try {
    		if (sSessionPreferences == null) {
        		sSessionPreferences = getSharedPreferences("main", 0);
        		sAppPreferences = getSharedPreferences("profiles", 0);
        	}
    		
    		if (isFirstRun()) {
    			handleFirstRun();
    		}
    		
    		if (baseurl == null) {
        		setBaseURL(sSessionPreferences);
        	}
        	mNfc = new Nfc(this);
        	setContentView(R.layout.tabs);
	        
	        //ImageView icon = new ImageView(this);
	        //icon.setImageResource(android.R.drawable.ic_menu_compass);
	        
	        Intent intBrowse = new Intent(this,Browser.class);
	        Intent playbackIntent = new Intent(this,Player.class);
	        
	        final Intent queryIntent = getIntent();
	        final String queryAction = queryIntent.getAction();
	        final String MEDIA_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
	        if (Intent.ACTION_SEARCH.equals(queryAction) || MEDIA_PLAY_FROM_SEARCH.equals(queryAction)) {
	        	String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
	        	// Track history
	        	SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, 
	                    SearchHistoryProvider.AUTHORITY, SearchHistoryProvider.MODE);
	            suggestions.saveRecentQuery(queryString, null);
	            
	        	
	            // look for keywords
	        	boolean quickplay=false;
	        	if (queryString != null && isPlayRequest(queryString)) {
	        		quickplay=true;
	        		queryString = stripPlayRequest(queryString);
	        	}
	        	// did the query come from android?
	        	if (MEDIA_PLAY_FROM_SEARCH.equals(queryAction)) {
	        		quickplay = true;
	        		getIntent().putExtra(EXTRA_SWITCH_TAB, "playback");
	        	}

	        	// TODO: use 'power search' to populate appropriate fields.
	        	
	        	if (queryString.contains(" by artist ")) {
	        		queryString.replace(" by artist ", " ");
	        	} else if (queryString.contains(" artist ")) {
	        		queryString.replace(" artist ", " " );
	        	} else if (queryString.startsWith("artist ")) {
	        		queryString = "@artist " + queryString.substring(7);
	        	} else if (queryString.contains(" by ")) {
	        		queryString.replace(" by ", " ");
	        	}
	        	
	        	if (queryString.contains(" album ")) {
	        		queryString.replace(" album ", " " );
	        	} else if (queryString.startsWith("album ")) {
	        		queryString = "@album " + queryString.substring(6);
	        	}
	     
	            // Handle search
	            
	            // Quick search
	            if (quickplay) {
	            	Intent qp = PlaybackService.getQuickplayIntent();
	            	qp.putExtra("query", queryString);
	            	startService(qp);
	            } else {
		            // Open Browse activity
		            intBrowse.putExtra(getPackageName()+".browse",
		            		 			Jinzora.getBaseURL()+"&request=search&query="+URLEncoder.encode(queryString, "UTF-8"));
	            }
	        } else if (getIntent() != null && getIntent().getExtras() != null) {
	        	intBrowse.putExtras(getIntent().getExtras());
	        }

	        TabHost host = this.getTabHost();

	        host.addTab(host.newTabSpec("browse")
	        		.setIndicator(getString(R.string.browser)/*,icon.getDrawable()*/)
	        		.setContent(intBrowse));
	        
	        host.addTab(host.newTabSpec("playback")
	        		.setIndicator(getString(R.string.player))
	        		.setContent(playbackIntent));
	        
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
    
    private boolean isPlayRequest(String queryString) {
    	return (queryString.startsWith("play ") || queryString.startsWith("listen to "));
    }
    
    private String stripPlayRequest(String query) {
    	if (query.startsWith("play ")) return query.substring(5);
    	if (query.startsWith("listen to ")) return query.substring(10);
    	return query;
    }
    

    protected static void initContext(Activity activity) {
    	activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
    
    protected static boolean createMenu(Menu menu) {
    	menu.add(0,MenuItems.HOME,1,R.string.home)
    	.setIcon(R.drawable.ic_menu_home)
    	.setAlphabeticShortcut('h');
    	
    	menu.add(0,MenuItems.ADDWHERE,2,"Queue Mode")
    	.setIcon(android.R.drawable.ic_menu_add)
    	.setAlphabeticShortcut('a');
    	
    	menu.add(0,MenuItems.SEARCH,3,R.string.search)
    	.setIcon(android.R.drawable.ic_search_category_default)
    	.setAlphabeticShortcut(SearchManager.MENU_KEY);
    	
    	menu.add(0,MenuItems.DOWNLOADS,4,R.string.downloads)
    	.setIcon(android.R.drawable.ic_menu_save)
    	.setAlphabeticShortcut('d');
    	
    	
    	menu.add(0,MenuItems.QUIT,5,R.string.quit)
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
    	case MenuItems.DOWNLOADS:
    		Intent dlLaunch = new Intent(activity,DownloadActivity.class);
    		activity.startActivity(dlLaunch);
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
            	Intent qp = PlaybackService.getQuickplayIntent();
            	qp.putExtra("query", "@id " + contents);
            	startService(qp);
            }            
        }
    }

	public static void doPlaylist(String playlist, int addtype) {
		// New-school way of doing it: send an intent.
		// Can then make this remotable.
		instance.mNfc.share(NdefFactory.fromUri(playlist));
		Intent plIntent = new Intent(PlaybackService.Intents.ACTION_PLAYLIST);
		plIntent.addCategory(PlaybackService.Intents.CATEGORY_REMOTABLE);
		plIntent.putExtra("playlist", playlist);
		plIntent.putExtra("addtype", addtype);
		Log.d(TAG, "sending playlist playback broadcast");
		instance.sendOrderedBroadcast(plIntent,null);
		
		// Old method: service binding.
		
		try {
			sPbConnection.getPlaybackBinding().updatePlaylist(playlist, addtype);
		} catch (RemoteException e) {
			Log.e("jinzora","Error sending playlist",e);
		}
	}
	
	private void doJukeboxConnectionPrompt() {
		
		// Connect
		if ("junction.intent.action.JOIN".equalsIgnoreCase(getIntent().getAction())) {
			// avoid re-posting on backwards navigation
			getIntent().setAction("");
			
			try {
				AlertDialog.Builder builder =
				new AlertDialog.Builder(Jinzora.this)
					.setTitle("Found a jukebox");
				
				List<String>urls = sPbConnection.getPlaybackBinding().getPlaylistURLs();
				if (urls != null && urls.size()>0) {
					// Have a current playlist
					builder
						.setItems(new String[] {"Send my current playlist"
							,"Connect without my playlist"
							,"Do not connect"}
								, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int selection) {
										switch (selection) {
										case 0:
											connectToJukebox(true);
											break;
											
										case 1:
											connectToJukebox(false);
											break;
										case 2:
											// do nothing
											break;
										}
									}
							});
				} else {
					// No playlist loaded
					builder.setMessage("Would you like to connect to this jukebox?")
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							connectToJukebox(false);
						}
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// Do nothing
						}
					});
				}
				
				builder.create().show();
			
			} catch (Exception e) {
				Log.e("jinzora","could not connect to jukebox",e);
			}
		}
		
		// Disconnect
		if (PlaybackService.Intents.ACTION_DISCONNECT_JUKEBOX.equalsIgnoreCase(getIntent().getAction())) {
			final Intent bounce = new Intent(getIntent());
			getIntent().setAction("");
			
			new AlertDialog.Builder(Jinzora.this)
				.setTitle("Disconnect from jukebox?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						bounce.setClassName(getPackageName(), PlaybackService.class.getName());
						startService(bounce);
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// Do nothing
					}
				}).create().show();
		}
	}
	
	
	/**
	 * Connect Jinzora to a remote jukebox.
	 * @param transferPlaylist whether or not to bring the current playlist
	 */
	private void connectToJukebox(boolean transferPlaylist) {
		// TODO: remove remote intents.
			// currently, this is using the edu.stanford.junction.remoteintents stuff
			// which was experimental and should be removed
		// TODO: support different playlist transfer modes (replace, queue, etc.)
		Intent jbIntent = getIntent();
		jbIntent.setAction(PlaybackService.Intents.ACTION_CONNECT_JUKEBOX);
		jbIntent.putExtra("transfer", transferPlaylist);
		jbIntent.setClassName(Jinzora.this, PlaybackService.class.getName());

		startService(jbIntent);
	}
	
	
	
	
	public class PlaybackServiceConnection implements ServiceConnection {
		private PlaybackInterface playbackBinding;
		private String baseurl;

		public boolean hasPlaybackBinding() {
			return playbackBinding != null;
		}
		public PlaybackInterface getPlaybackBinding() {
			if (playbackBinding == null) {

				synchronized (PlaybackServiceConnection.this) {
					while (playbackBinding == null) {
						try {
							PlaybackServiceConnection.this.wait();
						} catch (InterruptedException e) {}
					}
				}
			}
			return playbackBinding;
		}
		
		public synchronized void setBaseURL(String url) {
			baseurl=url;
			if (playbackBinding != null) {
				try {
					playbackBinding.setBaseURL(url);
				} catch (RemoteException e) {
					Log.e("jinzora","error setting baseurl on binding object",e);
				}
			}
		}
		
		// service connection methods
		public synchronized void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(TAG, "connected to playback service");
			if (playbackBinding == null) {
				Log.d("jinzora","playback interface is null; creating instance.");
				playbackBinding = PlaybackInterface.Stub.asInterface((IBinder)service);
				
				try {
					if (baseurl != null) {
						playbackBinding.setBaseURL(baseurl);
					} else {
						playbackBinding.setBaseURL(Jinzora.getBaseURL());
					}
				} catch (RemoteException e) {
					Log.e("jinzora","Error setting remote baseURL",e);
				}
				
				PlaybackServiceConnection.this.notify();
			}
			
			try {
				sPbConnection.playbackBinding.registerRemoteControl();
			} catch (RemoteException e) {}

			doJukeboxConnectionPrompt();
		}
		
		@Override
		public void onServiceDisconnected(ComponentName className) {
			Log.w("jinzora", "service disconnected.");
			playbackBinding = null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
	    final AlertDialog.Builder builder = new AlertDialog.Builder(Jinzora.this);
        TextView content = new TextView(this);
        content.setText(readAsset(this, ASSET_WELCOME));
        dialog.setTitle(R.string.jinzora_welcome);
        dialog.setContentView(content);
	}

	public static boolean doKeyUp(Context context, int keyCode, KeyEvent event) {
	    switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_PLAYPAUSE);
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_NEXT);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_PREV);
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_STOP);
                return true;
            }
	    return false;
	}
}