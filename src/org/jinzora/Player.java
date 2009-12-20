package org.jinzora;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jinzora.Jinzora.MenuItems;
import org.jinzora.playback.PlaybackService;
import org.jinzora.playback.PlaybackServiceConnection;
import org.jinzora.playback.players.PlaybackDevice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class Player extends ListActivity {
	private static Map<Integer,String>jukeboxes = null;
	private static int selectedPlaybackDevice = 0;
	private static int selectedAddType = 0;
	private static List<String[]> staticDeviceList;
	private static String[] addTypes = {"Replace current playlist","End of list","After current track"};

	ArrayAdapter<String> playlistAdapter;
	
	static {
		staticDeviceList = new ArrayList<String[]>();
		staticDeviceList.add(new String[] { "Local Device","org.jinzora.playback.players.LocalDevice" });
		staticDeviceList.add(new String[] { "Junction Jukebox","org.jinzora.playback.players.JunctionBox" });
		//staticDeviceList.add(new String[] { "Download List","org.jinzora.playback.players.DownloadPlaylist" });
		//staticDeviceList.add(new String[] { "Pocket Jukebox","org.jinzora.playback.players.JukeboxReceiver" });
		//staticDeviceList.add(new String[] { "Pocket Jukebox 2","org.jinzora.playback.players.JukeboxReceiver" });
		//staticDeviceList.add(new String[] { "http://prpl.stanford.edu/music/api.php?jb_id=quickbox&request=jukebox&user=prpl&pass=ppleaters","org.jinzora.playback.players.ForeignJukeboxDevice" });
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		playlistAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		
		this.setContentView(R.layout.player);
		setListAdapter(playlistAdapter);
		((ListView)findViewById(android.R.id.list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				
				try {
					Jinzora.sPbConnection.playbackBinding.jumpTo(pos);
				} catch (Exception e) {
					Log.e("jinzora","Failed jumping in playlist",e);
				}
				
			}
			
		});
		
		
		
		this.findViewById(R.id.prevbutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.sPbConnection.playbackBinding.prev();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
		});
		
		this.findViewById(R.id.nextbutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.sPbConnection.playbackBinding.next();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
		});
		
		this.findViewById(R.id.playbutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.sPbConnection.playbackBinding.pause();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
		});
		
		this.findViewById(R.id.pausebutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.sPbConnection.playbackBinding.pause();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
		});
		
		/*
		try {
			if (null == jukeboxes) {
				refreshJukeboxList();
			}
			
			setJukeboxSpinner();
			
			// jukebox refresh
			Button button = (Button)this.findViewById(R.id.refreshJukeboxes);
			button.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					refreshJukeboxList();
					setJukeboxSpinner();
				}
				
			});
			
		} catch (Exception e) {
			Log.e("jinzora","Error creating jukebox list",e);
		}
		
		// JB extra features
		Button button = (Button)this.findViewById(R.id.jbfeatures);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				jukeboxFeatures();
			}
		});
		
		*/
		
	}
	
	/*
	protected static void refreshJukeboxList() {
		try {
			jukeboxes = new HashMap<Integer,String>();
			String request = Jinzora.getBaseURL();
			request += "&request=jukebox&action=list";
			URL url = new URL(request);
			
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			InputStream inStream = conn.getInputStream();
			conn.connect();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
			String line = null;

			while ((line = br.readLine()) != null) {
				int pos = line.indexOf(':');
				Integer key  = Integer.parseInt(line.substring(0,pos));
				jukeboxes.put(key, line.substring(pos+1));
			}
		} catch (Exception e) {
			Log.d("jinzora","Error getting jukebox list",e);
		}
	}
	
	private void setJukeboxSpinner() {
		// List of players
		Spinner spinner = (Spinner)this.findViewById(R.id.player_jb_list);
		 
		ArrayList<String> jba = new ArrayList<String>();
		for (String[] device : staticDeviceList) {
			jba.add(device[0]);
		}
		
		if (jukeboxes != null && jukeboxes.size() > 0) {
			String[] values = jukeboxes.values().toArray(new String[]{});
			for (int i = 0; i < values.length; i++) {
				if (!jba.contains(values[i])) {
					jba.add(values[i]);
				}
			}
		}
		
		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_dropdown_item,
		        jba.toArray(new String[]{}) );
		    spinner.setAdapter(spinnerArrayAdapter);
		spinner.setVisibility(View.VISIBLE);
		spinner.setSelection(selectedPlaybackDevice);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener () {

			@Override
			public void onItemSelected(AdapterView<?> parent, View v,
					int pos, long id) {
				
				if (pos == selectedPlaybackDevice) return;
				try {
					
					if (pos < staticDeviceList.size()) {
						Jinzora.sPbConnection.playbackBinding.setPlaybackDevice(staticDeviceList.get(pos)[1],staticDeviceList.get(pos)[0]);
					} else {
						// set jb_id to pos-1 somehow in Jinzora.
						Jinzora.sPbConnection.playbackBinding.setPlaybackDevice("org.jinzora.playback.players.JukeboxDevice",""+(pos-staticDeviceList.size()));
					}
					selectedPlaybackDevice = pos;
				} catch (Exception e) {
					Log.e("jinzora","Error setting player",e);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
				
			}
			
		});
		
	}
	
	
	private void jukeboxFeatures() {
		
		final int pos = ((Spinner)this.findViewById(R.id.player_jb_list)).getSelectedItemPosition();
		if (pos < staticDeviceList.size()) {
			final String playerClass= staticDeviceList.get(pos)[1];
			
			new Thread() {
				@Override
				public void run() {
					try {
						Class pc = Class.forName(playerClass);
						Method m = pc.getMethod("doFeaturesView", new Class[]{Activity.class});
						m.invoke(null, Player.this);
					} catch (Exception e) {
						Log.e("jinzora","error drawing features view for class " + playerClass,e);
					}
				}
			}.start();
		}
	}
	*/
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		try {
			List<String>tracks = Jinzora.sPbConnection.playbackBinding.getPlaylist();
			if (tracks != null) {
				playlistAdapter.clear();
				for (int i=0;i<tracks.size();i++) {
					playlistAdapter.add(tracks.get(i));
				}
			}
		} catch (Exception e) {
			Log.e("jinzora" , "Could not build playlist", e);
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	Jinzora.createMenu(menu);
    	
    	menu.add(0,PlayerMenuItems.ADDWHERE,3,"Queue Mode")
    	.setIcon(android.R.drawable.ic_menu_add)
    	.setAlphabeticShortcut('a');
    	
    	return true;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	
    	if (item.getItemId() == PlayerMenuItems.ADDWHERE) {

			ArrayAdapter<String> addTypeAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_dropdown_item,
			        addTypes );
			
			
			
			DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener () {
				@Override
				public void onClick(DialogInterface arg0, int pos) {
					if (pos == selectedAddType) return;
					try {
						Jinzora.sPbConnection.playbackBinding.setAddType(pos);
						selectedAddType = pos;
					} catch (Exception e) {
						Log.e("jinzora","Error setting add-type",e);
					}
				}
			};			
			
			AlertDialog dialog = 
			new AlertDialog.Builder(this)
				.setAdapter(addTypeAdapter, onClickListener)
				.setTitle(R.string.add_to)
				.create();
			
			dialog.show();
    		
    		
    	}
    	
    	Jinzora.menuItemSelected(featureId,item,this);
    	return super.onMenuItemSelected(featureId, item);
    }
    
    static class PlayerMenuItems {
    	public static int ADDWHERE = 101;
    	
    }
}
