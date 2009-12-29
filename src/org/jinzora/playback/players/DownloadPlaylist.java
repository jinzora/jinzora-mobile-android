package org.jinzora.playback.players;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jinzora.Jinzora;
import org.jinzora.R;
import org.jinzora.playback.PlaybackService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class DownloadPlaylist extends PlaybackDevice {
	private Service service;
	private MediaPlayer mp = null;
	private int pos;
	private List<String> trackNames;
	
	private NotificationManager nm;
	private static final int NOTIFY_ID = R.layout.player;
	private static String UNKNOWN_TRACK = "Unknown Track";
	
	
	public DownloadPlaylist() {
		this.service = PlaybackService.getInstance();
		mp = new MediaPlayer();
		playlist = new ArrayList<String>();
		trackNames = new ArrayList<String>();
		
		pos = 0;
		nm = (NotificationManager) this.service.getSystemService(Service.NOTIFICATION_SERVICE);
	}

	private static DownloadPlaylist instance = null;
	public static DownloadPlaylist getInstance(String arg) {
		if (instance == null) {
			instance = new DownloadPlaylist();
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
		
	}

	@Override
	public synchronized void pause() throws RemoteException {
		
	}

	@Override
	public synchronized void prev() throws RemoteException {
		
	}

	@Override
	public synchronized void jumpTo(int pos) throws RemoteException {
		
	}

	public void playlist(String urlstr, int addType) {
		try {
			URL url = new URL(urlstr);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			InputStream inStream = conn.getInputStream();
			conn.connect();
			
			ArrayList<Downloadable>downloadList = new ArrayList<Downloadable>();
			trackNames = new ArrayList<String>();

			BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
			String line = null; 
			String lastLine = null;
			
			line = br.readLine();
			while (line != null) {
				if (line.charAt(0) != '#') {
					try {
						URL track = new URL(line);
						String trackname;
						
					    if (lastLine.charAt(0) == '#') {
					    	int pos;
					    	if (-1 != (pos = lastLine.indexOf(','))) {
					    		trackname = lastLine.substring(pos+1,lastLine.length());
					    	} else {
					    		trackname = UNKNOWN_TRACK;
					    	}
					    } else {
					    	trackname = UNKNOWN_TRACK;
					    }
					    
					    downloadList.add(new Downloadable(track,trackname));
					    
					} catch (Exception e) {
						// probably a comment line
					}
				}
				
				lastLine = line;
				line = br.readLine();
			}
			
			download(downloadList);
		} catch (Exception e) {
			Log.e("jinzora","Error downloading media",e);
		}
	}
	
	
	@Override
	public void stop() throws RemoteException {
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
	
	
	
	public void download(final List<Downloadable>list) {
		new Thread() {
			@Override
			public void run() {
				for (Downloadable dl : list) {
					try {
						Log.d("jinzora","About to download " + dl.name);
						
					    File root = new File(Environment.getExternalStorageDirectory(), "Music");
					    Log.d("jinzora","Writing to " + root.getAbsolutePath());
					    if (root.canWrite()){
					    	
					    	Log.d("jinzora","path is writable; opening file");
					    	
					        File file = new File(root, dl.name + ".mp3");
					        FileOutputStream out = new FileOutputStream(file);
					        
					        InputStream in = dl.url.openStream();
					        byte[] buf = new byte[4 * 1024];
					        int bytesRead;
					        while ((bytesRead = in.read(buf)) != -1) {
					          out.write(buf, 0, bytesRead);
					        }
					        in.close();
					        out.close();
					        
					        Log.d("jinzora","downloaded " + dl.name);
					    }
					} catch (IOException e) {
					    Log.e("jinzora", "Could not write file " + e.getMessage());
					}
					
					
				}				
			}
		}.start();
	}

	@Override
	public String playbackIPC(String params) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getPlaylist() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPlaylistPos() throws RemoteException {
		// TODO Auto-generated method stub
		return -1;
	}
	
	@Override
	public String getArtistName() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getTrackName() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
}


class Downloadable {
	public URL url;
	public String name;
	
	public Downloadable(URL u, String n) {
		url=u;
		name=n;
	}
}