package org.jinzora;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import android.app.Activity;
import android.content.Context;
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
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class Player extends Activity {
	private static Map<Integer,String>jukeboxes = null;
	private static int selectedPlaybackDevice = 0;
	private static int selectedAddType = 0;
	private static List<String[]> staticDeviceList;
	private static String[] addTypes = {"Replace current playlist","End of list","After current track"};

	
	static {
		staticDeviceList = new ArrayList<String[]>();
		staticDeviceList.add(new String[] { "Local Device","org.jinzora.playback.players.LocalDevice" });
		//staticDeviceList.add(new String[] { "Download List","org.jinzora.playback.players.DownloadPlaylist" });
		staticDeviceList.add(new String[] { "Pocket Jukebox","org.jinzora.playback.players.JukeboxReceiver" });
		//staticDeviceList.add(new String[] { "Pocket Jukebox 2","org.jinzora.playback.players.JukeboxReceiver" });
		//staticDeviceList.add(new String[] { "http://prpl.stanford.edu/music/api.php?jb_id=quickbox&request=jukebox&user=prpl&pass=ppleaters","org.jinzora.playback.players.ForeignJukeboxDevice" });
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (null == jukeboxes) {
			refreshJukeboxList();
		}
		
		this.setContentView(R.layout.player);
		
		
		try {
			setJukeboxSpinner();
			
			/* jukebox refresh */
			Button button = (Button)this.findViewById(R.id.refreshJukeboxes);
			button.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					refreshJukeboxList();
					setJukeboxSpinner();
				}
				
			});
			
			/* Add-types */
			Spinner spinner = (Spinner)this.findViewById(R.id.addtype_list);
			 
			
			ArrayAdapter<String> addTypeAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_dropdown_item,
			        addTypes );
			spinner.setAdapter(addTypeAdapter);
			spinner.setVisibility(View.VISIBLE);
			spinner.setSelection(selectedAddType);
			spinner.setOnItemSelectedListener(new OnItemSelectedListener () {

				@Override
				public void onItemSelected(AdapterView<?> parent, View v,
						int pos, long id) {
					
					try {
						Jinzora.playbackBinding.setAddType(pos);
						selectedAddType = pos;
					} catch (Exception e) {
						Log.e("jinzora","Error setting add-type",e);
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					
					
				}
				
			});
		} catch (Exception e) {
			Log.e("jinzora","Error creating jukebox list",e);
			}
		
		this.findViewById(R.id.prevbutton).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.playbackBinding.prev();
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
							Jinzora.playbackBinding.next();
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
							Jinzora.playbackBinding.pause();
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
							Jinzora.playbackBinding.pause();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
			
		});
	}
	
	
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
		/* List of players */
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
				
				try {
					if (pos < staticDeviceList.size()) {
						Jinzora.playbackBinding.setPlaybackDevice(staticDeviceList.get(pos)[1],staticDeviceList.get(pos)[0]);
					} else {
						// set jb_id to pos-1 somehow in Jinzora.
						Jinzora.playbackBinding.setPlaybackDevice("org.jinzora.playback.players.JukeboxDevice",""+(pos-staticDeviceList.size()));
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
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	return Jinzora.createMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	Jinzora.menuItemSelected(featureId,item);
    	return super.onMenuItemSelected(featureId, item);
    }
}
