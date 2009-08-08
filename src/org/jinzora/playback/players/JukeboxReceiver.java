package org.jinzora.playback.players;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.UUID;

import org.jinzora.playback.PlaybackService;
import org.json.JSONArray;
import org.json.JSONObject;

import android.media.MediaPlayer;
import android.util.Log;

public class JukeboxReceiver extends JukeboxDevice {
	private String JB_ID = null;
	private ReceiverThread thread = null;

	
	private JukeboxReceiver(String mjb_id) {
		super(mjb_id);
		/*
		String code = UUID.randomUUID().toString();
		code = code.substring(code.length()-4);
		JB_ID = "Pocket Rockette (" + code + ")";
		*/
		JB_ID=mjb_id;
		
		(thread = new ReceiverThread(JB_ID)).start();
	}
	
	@Override
	public void onDestroy() {
		thread.murder();
		super.onDestroy();
	}
	
	private static JukeboxReceiver instance = null;
	public static JukeboxReceiver getInstance(String arg) {
		//if (instance == null) {
			instance = new JukeboxReceiver(arg/*JB_ID*/);
		//}
		return instance;
	}
}

class ReceiverThread extends Thread {
	private static int POLL_TIME = 2000;
	private boolean dying = false;
	private MediaPlayer mp = null;
	private int pos;
	boolean isPaused;
	ArrayList<String>playlist = null;
	private String updateURL = null;
	
	public ReceiverThread(String id) {
		mp = new MediaPlayer();
		
		playlist = new ArrayList<String>();
		pos = 0;
		isPaused = false;
		
		updateURL = PlaybackService.getBaseURL();
		int slash = updateURL.lastIndexOf("/");
		updateURL = updateURL.substring(0,slash) + "/jukebox/index.php?id="+URLEncoder.encode(id)+"&update=true";
	}
	
	public void murder() {
		dying = true;
	}
	
	@Override
	public void run() {
		Integer lastTime = -1;
		
		while (!dying) {
			try {
				URL url = new URL(updateURL);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				InputStream inStream = conn.getInputStream();
				conn.connect();
				
				String line;
				BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
				StringBuilder sb = new StringBuilder();
				while (null != (line = br.readLine())) {
					sb.append(line).append("\n");
				}
				
				JSONObject json = new JSONObject(sb.toString());
				if (json.get("time") instanceof Integer) {
					if ((Integer)json.get("time") > lastTime) {
						lastTime = (Integer)json.get("time");
						String cmd = (String)json.get("command");
						
						
						if (cmd.equals("playlist")) {
							if (json.get("playlist") != null && json.get("playlist") instanceof JSONArray) {
								playlist.clear();
								JSONArray pl = (JSONArray)json.get("playlist");
								for (int i = 0; i < pl.length(); i++) {
									playlist.add(pl.getString(i).trim());
								}
								if (json.get("addtype") != null && ((String)json.get("addtype")).equals("replace")) {
									jumpTo(0);
								}
							}
						}
						
						else if (cmd.equals("pause")) {
							pause();
						}
						
						else if (cmd.equals("next")) {
							jumpTo(pos+1);
						}
						
						else if (cmd.equals("prev")) {
							jumpTo(pos-1);
						}
						
					}
				}
				
			} catch (Exception e) {
				Log.e("jinzora","Error updating jukeboxReceiver",e);
			}
			
			try {
				Thread.sleep(POLL_TIME);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	protected synchronized void jumpTo(int p) {
		try {
			pos=p;
			if (pos < 0) {
				pos = 0;
			}
			
			if (pos >= playlist.size()) {
				pos = 0;
				return;
			}
			
			mp.reset();
			mp.setDataSource(playlist.get(pos));
			mp.prepare();
			mp.start();
			
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				
				public void onCompletion(MediaPlayer arg0) {
					jumpTo(pos+1);
				}
			});
	
			
			URL url = new URL(updateURL + "&update_pos="+pos);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.connect();
			
		} catch (Exception e) {
			Log.e("jinzora","Error changing media",e);
		}
	}
	
	protected synchronized void pause() {
		if (isPaused) {
			mp.start();
		} else {
			mp.pause();
		}
		isPaused = !isPaused;
	}
}