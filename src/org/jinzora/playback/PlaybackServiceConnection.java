package org.jinzora.playback;

import org.jinzora.Jinzora;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class PlaybackServiceConnection implements ServiceConnection {
	public PlaybackInterface playbackBinding;
	private String baseurl;

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
		} else {
			//Log.w("jinzora","onServiceConnected called but playback object is not null");
		}
	}
	
	@Override
	public void onServiceDisconnected(ComponentName className) {
		Log.w("jinzora", "service disconnected.");
		playbackBinding = null;
	}
}
