package org.jinzora.playback.players;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jinzora.Jinzora;
import org.jinzora.R;
import org.jinzora.playback.PlaybackService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class JukeboxDevice extends PlaybackDevice {
	private Service service;
	private String jb_id = null;
	
	private NotificationManager nm;
	private static final int NOTIFY_ID = R.layout.browse;
	
	protected JukeboxDevice(String mjb_id) {
		this.service = PlaybackService.getInstance();
		playlist = new ArrayList<String>();
		jb_id=mjb_id;
		nm = (NotificationManager) this.service.getSystemService(Service.NOTIFICATION_SERVICE);
	}
	
	private static JukeboxDevice instance = null;
	public static JukeboxDevice getInstance(String jb_id) {
		//if (instance == null) {
			instance = new JukeboxDevice(jb_id);
		//}
		return instance;
	}
	
	protected String getBaseURL() {
		if (jb_id != null) {
			return PlaybackService.getBaseURL() + "&request=jukebox&jb_id="+URLEncoder.encode(jb_id);
		} else {
			return null;
		}
	}

	public void onDestroy() {
		nm.cancel(NOTIFY_ID);
		playlist = null;
	}
	
	@Override
	public void clear() throws RemoteException {
		try {
			URL cmd = new URL(getBaseURL() + "&command=clear");
			HttpURLConnection conn = (HttpURLConnection)cmd.openConnection();
			/*InputStream inStream =*/ conn.getInputStream();
			conn.connect();
			
			playlist.clear();
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public synchronized void next() throws RemoteException {
		try {
			URL cmd = new URL(getBaseURL() + "&command=next");
			HttpURLConnection conn = (HttpURLConnection)cmd.openConnection();
			/*InputStream inStream =*/ conn.getInputStream();
			conn.connect();
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public synchronized void pause() throws RemoteException {
		try {
			URL cmd = new URL(getBaseURL() + "&command=pause");
			HttpURLConnection conn = (HttpURLConnection)cmd.openConnection();
			/*InputStream inStream =*/ conn.getInputStream();
			conn.connect();
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public synchronized void prev() throws RemoteException {
		try {
			URL cmd = new URL(getBaseURL() + "&command=previous");
			HttpURLConnection conn = (HttpURLConnection)cmd.openConnection();
			/*InputStream inStream =*/ conn.getInputStream();
			conn.connect();
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public synchronized void jumpTo(int pos) throws RemoteException {
		try {
			// easy, but have to fix $_POST['jbjumpto'] to be $_REQUEST-based.
		} catch (Exception e) {
			Log.e("jinzora","Error changing media",e);
		}
	}

	@Override
	public void stop() throws RemoteException {
		try {
			URL cmd = new URL(getBaseURL() + "&command=stop");
			HttpURLConnection conn = (HttpURLConnection)cmd.openConnection();
			/*InputStream inStream =*/ conn.getInputStream();
			conn.connect();
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public void playlist(String urlstr, int currentAddType) {
		try {
			String addtype;
			switch (currentAddType) {
			case 1:
				addtype = "end";
				break;
			case 2:
				addtype = "current";
				break;
			default:
				addtype = "replace";
			}
			URL cmd = new URL(urlstr + "&jb_id="+URLEncoder.encode(jb_id)+"&addwhere="+addtype);
			HttpURLConnection conn = (HttpURLConnection)cmd.openConnection();
			/*InputStream inStream =*/ conn.getInputStream();
			conn.connect();
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}
	
	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String playbackIPC(String params) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getPlaylist() throws RemoteException {
		return null;
	}

	@Override
	public int getPlaylistPos() throws RemoteException {
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
