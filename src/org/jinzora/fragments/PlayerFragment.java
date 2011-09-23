package org.jinzora.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jinzora.Jinzora;
import org.jinzora.android.R;
import org.jinzora.playback.PlaybackService;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlayerFragment extends ListFragment {
	private static Map<Integer,String>jukeboxes = null;
	private static int selectedPlaybackDevice = 0;
	private static int selectedAddType = 0;
	private static List<String[]> staticDeviceList;
	
	PlaylistAdapter mPlaylistAdapter;
	BroadcastReceiver mPositionReceiver;
	BroadcastReceiver mListUpdatedReceiver;
	
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
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);	
		mPlaylistAdapter = new PlaylistAdapter(getActivity());
		setListAdapter(mPlaylistAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View v = inflater.inflate(R.layout.player, container, false);

		final CharSequence[] entryOptions = {"Play Now", "Play Next" };
		((ListView)v.findViewById(android.R.id.list))
			.setOnItemLongClickListener(
					new AdapterView.OnItemLongClickListener() {

						@Override
						public boolean onItemLongClick(AdapterView<?> parent,
								View view, final int listPosition, long id) {

							AlertDialog dialog =
								new AlertDialog.Builder(getActivity())
								.setTitle(mPlaylistAdapter.getItemName(listPosition))
								.setItems(entryOptions, new AlertDialog.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int entryPos) {
										switch (entryPos) {
										case 0:
											try {
												Jinzora.sPbConnection.getPlaybackBinding().jumpTo(listPosition);
											} catch (RemoteException e) {
												Log.e("jinzora", "Failed to play track",e);
											}
											break;
										case 1:
											try {
												Jinzora.sPbConnection.getPlaybackBinding().queueNext(listPosition);
											} catch (RemoteException e) {
												Log.e("jinzora", "Failed to queue track",e);
											}
											break;
										}
									}
								})
								.create();
							dialog.show();
							return true;
						}
					});

		// Buttons
		v.findViewById(R.id.prevbutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.sPbConnection.getPlaybackBinding().prev();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
		});
		
		/* Could handle these buttons with broadcasted Intents as well. */
		
		v.findViewById(R.id.nextbutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.sPbConnection.getPlaybackBinding().next();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
		});
		
		v.findViewById(R.id.playbutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							// also supports playpause();
							Jinzora.sPbConnection.getPlaybackBinding().play();
						} catch (RemoteException e) {
							Log.e("jinzora","Error during playback action",e);
						}
					}
				}.start();
			}
		});
		
		v.findViewById(R.id.pausebutton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread() {
					@Override
					public void run() {
						try {
							Jinzora.sPbConnection.getPlaybackBinding().pause();
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
		return v;
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
	public void onListItemClick(ListView l, View v, int position, long id) {
	    try {
            Jinzora.sPbConnection.getPlaybackBinding().jumpTo(position);
        } catch (Exception e) {
            Log.e("jinzora","Failed jumping in playlist",e);
        }
	}
	
	@Override
	public void onResume() {
		super.onResume();

		updateUi();
		mPositionReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int pos = intent.getExtras().getInt("position");
				mPlaylistAdapter.setPlaylistPos(pos);
			}
		};	

		IntentFilter intentFilter = new IntentFilter(PlaybackService.PLAYSTATE_CHANGED);
		getActivity().registerReceiver(mPositionReceiver, intentFilter);
		
		
		mListUpdatedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				try {
					List<String>tracks = Jinzora.sPbConnection.getPlaybackBinding().getPlaylistNames();
					if (tracks != null) {
						int pos = Jinzora.sPbConnection.getPlaybackBinding().getPlaylistPos();
						mPlaylistAdapter.setEntries(tracks,pos);
					}
				} catch (Exception e) {
					Log.e("jinzora" , "Could not build playlist", e);
				}
			}
		};
		
		IntentFilter listIntentFilter = new IntentFilter(PlaybackService.PLAYLIST_UPDATED);
		getActivity().registerReceiver(mListUpdatedReceiver, listIntentFilter);
	}
	
	private void updateUi() {
		if (Jinzora.sPbConnection.hasPlaybackBinding()) {
			try {
				List<String>tracks = Jinzora.sPbConnection.getPlaybackBinding().getPlaylistNames();
				if (tracks != null) {
					int pos = Jinzora.sPbConnection.getPlaybackBinding().getPlaylistPos();
					mPlaylistAdapter.setEntries(tracks,pos);
				}
			} catch (Exception e) {
				Log.e("jinzora" , "Could not build playlist", e);
			}
		} else {
			// plan B
			new Thread() {
				public void run() {
					while (!Jinzora.sPbConnection.hasPlaybackBinding()) {
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {}
					}
					if (Jinzora.sPbConnection.hasPlaybackBinding()) {
					    getActivity().runOnUiThread(new Runnable() {
							public void run() {
								try {
									List<String>tracks = Jinzora.sPbConnection.getPlaybackBinding().getPlaylistNames();
									if (tracks != null) {
										int pos = Jinzora.sPbConnection.getPlaybackBinding().getPlaylistPos();
										mPlaylistAdapter.setEntries(tracks,pos);
									}
								} catch (Exception e) {
									Log.e("jinzora" , "Could not build playlist", e);
								}
							};
						});
					}
				};
			}.start();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		getActivity().unregisterReceiver(mPositionReceiver);
		getActivity().unregisterReceiver(mListUpdatedReceiver);
		mPositionReceiver = null;
		mListUpdatedReceiver = null;
	}
}


class PlaylistAdapter extends ArrayAdapter<String> {
	protected int mPos = -1;
	Context mListActivity;
	LayoutInflater inflater;
	public PlaylistAdapter(Context parent) {
		super(parent, android.R.layout.simple_list_item_1);
		mListActivity = parent;
		inflater=LayoutInflater.from(parent);
	}
	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row;
		if (convertView != null) {
			row = convertView;
		} else {
			row=inflater.inflate(R.layout.playlist_item, null);
		}
		String entry = this.getItem(position);

		// copied from m3u convention.
		// TODO: build better metadata when playlist is read
		int p = entry.indexOf(" - ");
		if (p >= 0) {
			((TextView)row.findViewById(R.id.entry_line2)).setText(entry.substring(0,p));
			((TextView)row.findViewById(R.id.entry_line1)).setText(entry.substring(p+3));
		} else {
			((TextView)row.findViewById(R.id.entry_line1)).setText(entry);
		}
		
		//ImageView iv = (ImageView)row.findViewById(R.id.play_indicator);
		if (position == mPos) {
			//iv.setVisibility(View.VISIBLE);
			//iv.setImageResource(android.R.drawable.ic_media_play);
			row.setBackgroundResource(R.color.now_playing);
		} else {
			//iv.setVisibility(View.GONE);
			row.setBackgroundColor(Color.TRANSPARENT);
		}
		return row;
	}
	
	public void setEntries(List<String>tracks, int pos) {
		mPos=pos;
		this.clear();
		for (int i=0;i<tracks.size();i++) {
			this.add(tracks.get(i));
		}
	}
	
	public void setPlaylistPos(int pos) {
		mPos=pos;
		
		notifyDataSetChanged();
	}
	
	public String getItemName(int pos) {
		String item = getItem(pos);
		int p = item.indexOf(" - ");
		if (p > 0) {
			return item.substring(p+3);
		}
		return item;
	}
}
