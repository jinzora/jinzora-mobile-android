package org.jinzora.playback.players;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jinzora.playback.PlaybackService;

import android.util.Log;

public class ForeignJukeboxDevice extends JukeboxDevice {
	private String base_url = null;
	private static ForeignJukeboxDevice instance = null;
	
	public static ForeignJukeboxDevice getInstance(String url) {
		//if (instance == null) {
			instance = new ForeignJukeboxDevice(url);
		//}
		return instance;
	}
	
	private ForeignJukeboxDevice(String url) {
		super(null);
		
		this.base_url = url+"&request=jukebox"; // todo: handle this param better
	}
	
	protected String getBaseURL() {
		if (base_url != null) {
			return base_url;
		} else {
			return null;
		}
	}
	
	
	@Override
	public void updatePlaylist(String urlstr, int currentAddType) {
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
			URL cmd = new URL(getBaseURL()+"&addwhere="+addtype+"&command=play&external_playlist="+URLEncoder.encode(urlstr));
			Log.d("jinzora","hitting " + cmd.toExternalForm());
			HttpURLConnection conn = (HttpURLConnection)cmd.openConnection();
			/*InputStream inStream =*/ conn.getInputStream();
			conn.connect();
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}
}
