package org.jinzora.playback.players;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jinzora.Jinzora;
import org.jinzora.R;
import org.jinzora.playback.PlaybackService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class LocalDevice extends PlaybackDevice {
	private Service service;
	private MediaPlayer mp = null;
	private int pos;
	private List<String> trackNames;
	
	private NotificationManager nm;
	private static final int NOTIFY_ID = R.layout.player;
	private static String UNKNOWN_TRACK = "Unknown Track";
	
	
	public LocalDevice() {
		this.service = PlaybackService.getInstance();
		mp = new MediaPlayer();
		playlist = new ArrayList<String>();
		trackNames = new ArrayList<String>();
		
		pos = 0;
		nm = (NotificationManager) this.service.getSystemService(Service.NOTIFICATION_SERVICE);
	}

	private static LocalDevice instance = null;
	public static LocalDevice getInstance(String arg) {
		if (instance == null) {
			instance = new LocalDevice();
		}
		return instance;
	}
	
	public void onDestroy() {
		nm.cancel(NOTIFY_ID);
		/*mp.stop();
		mp.release();
		mp = null;
		playlist = null;*/
	}
	
	@Override
	public void clear() throws RemoteException {
		playlist.clear();
		mp.stop();
		nm.cancel(NOTIFY_ID);
	}

	@Override
	public synchronized void next() throws RemoteException {
		if (++pos >= playlist.size()) {
			pos = 0;
		} else {
			jumpTo(pos);
		}
	}

	@Override
	public synchronized void pause() throws RemoteException {
		if (!mp.isPlaying()) {
			mp.start();
			notifyPlaying();
			
		} else {
			mp.pause();
			notifyPaused();
		}
	}

	@Override
	public synchronized void prev() throws RemoteException {
		if (pos > 0) {
			pos--;
		}
		jumpTo(pos);
	}

	@Override
	public synchronized void jumpTo(int pos) throws RemoteException {
		try {
			this.pos=pos;
			mp.reset();
			mp.setDataSource(playlist.get(pos));
			mp.prepare();
			mp.start();
	
			mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
	
				public void onCompletion(MediaPlayer arg0) {
					try {
						next();
					} catch (RemoteException e) {

					}
				}
			});
			
			notifyPlaying();
			
		} catch (Exception e) {
			Log.e("jinzora","Error changing media",e);
		}
	}

	public synchronized void playlist(String urlstr) {
		try {
			URL url = new URL(urlstr);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			InputStream inStream = conn.getInputStream();
			conn.connect();
			
			if (currentAddType == ADD_REPLACE) {
				playlist = new ArrayList<String>();
				trackNames = new ArrayList<String>();
			}
			
			List<String> endList = new ArrayList<String>();
			List<String> endListNames = new ArrayList<String>();
			if (currentAddType == ADD_CURRENT) {
				while (playlist.size() > pos+1) {
					endList.add(playlist.remove(pos+1));
					endListNames.add(trackNames.remove(pos+1));
				}
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
			String line = null; 
			String lastLine = null;
			
			line = br.readLine();
			while (line != null) {
				if (line.charAt(0) != '#') {
					try {
						URL track = new URL(line);
					    playlist.add(track.toExternalForm());
					    if (lastLine.charAt(0) == '#') {
					    	int pos;
					    	if (-1 != (pos = lastLine.indexOf(','))) {
					    		trackNames.add(lastLine.substring(pos+1,lastLine.length()));
					    	} else {
					    		trackNames.add(UNKNOWN_TRACK);
					    	}
					    } else {
					    	trackNames.add(UNKNOWN_TRACK);
					    }
					} catch (Exception e) {
						// probably a comment line
					}
				}
				
				lastLine = line;
				line = br.readLine();
			}
			
			playlist.addAll(endList);
			trackNames.addAll(endListNames);
			
			if (currentAddType == ADD_REPLACE || mp == null) {
				jumpTo(0);
			} else if (!mp.isPlaying()) {
				jumpTo(pos);
			}
			
		} catch (Exception e) {
			Log.e("jinzora","Error playing media",e);
		}
	}
	
	
	@Override
	public void stop() throws RemoteException {
		mp.stop();
		nm.cancel(NOTIFY_ID);
	}

	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void notifyPlaying() {
		try {
			String notice = /*"Playing: " +*/ trackNames.get(pos);
			Notification notification = new Notification(
					android.R.drawable.ic_media_play, notice, System.currentTimeMillis());
			PendingIntent pending = PendingIntent.getActivity(this.service, 0,
	                								new Intent(this.service, Jinzora.class), 0);
			notification.setLatestEventInfo(this.service, "Jinzora Mobile", notice, pending);
			
			nm.notify(NOTIFY_ID, notification);
		} catch (Exception e) {
			Log.d("jinzora","notification error",e);
		}
	}
	
	private void notifyPaused() {
		try {
			Notification notification = new Notification(
					android.R.drawable.ic_media_pause, "Paused", System.currentTimeMillis());
			PendingIntent pending = PendingIntent.getActivity(this.service, 0,
	                								new Intent(this.service, Jinzora.class), 0);
			notification.setLatestEventInfo(this.service, "Jinzora Mobile", "Paused", pending);
			
			nm.notify(NOTIFY_ID, notification);
		} catch (Exception e) {
			Log.d("jinzora","notification error",e);
		}
	}
}
