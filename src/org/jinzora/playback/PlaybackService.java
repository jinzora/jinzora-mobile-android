package org.jinzora.playback;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.jinzora.playback.players.LocalDevice;
import org.jinzora.playback.players.PlaybackDevice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PlaybackService extends Service {
	PlaybackDevice player = null;
	private static PlaybackService instance = null;
	private static String baseurl = null;
	
	public static PlaybackService getInstance() {
		if (instance == null) {
			Log.w("jinzora","PlaybackService.instance should not be null");
			instance = new PlaybackService();
		}
		return instance;
	}
	
	/**
	 * This service and the associated PlaybackServices
	 * are in a different process from the main Jinzora activity,
	 * and cannot share static variables / preferences.
	 * Use this method to retrieve the media server url
	 * for classes running in this process.
	 */
	public static String getBaseURL() {
		return baseurl;
	}
	
	public static void setBaseURL(String url) {
		baseurl = url;
	}
	
	public void setPlaybackDevice(String playerClass, String arg) {
		try {
			Class<PlaybackDevice> pc = (Class<PlaybackDevice>) Class.forName(playerClass);
			Method m = pc.getMethod("getInstance", new Class[]{String.class});
			if (player != null) {
				try {
					player.onDestroy();
				} catch (Exception e) {
					Log.e("jinzora","error destroying player", e);
				}
			}
			player = (PlaybackDevice)m.invoke(null, arg);
			
		} catch (Exception e) {
			Log.e("jinzora","error instantiating player",e);
		}
	}
	
	private final PlaybackInterface.Stub mBinder = new PlaybackInterface.Stub() {
		
		@Override
		public boolean isPlaying() {
			return player.isPlaying();
		}
		
		@Override
		public void onCallBegin() {
			player.onCallBegin();
		}
		
		@Override
		public void onCallEnd() {
			player.onCallEnd();
		}
		
		@Override
		public void playlist(String pl) throws RemoteException {
			player.playlist(pl);
			
		}
		
		@Override
		public void clear() throws RemoteException {
			player.clear();
			
		}
		
		@Override
		public void next() throws RemoteException {
			player.next();
			
		}
		
		@Override
		public void pause() throws RemoteException {
			player.pause();
			
		}
		
		@Override
		public void prev() throws RemoteException {
			player.prev();
			
		}
		
		@Override
		public void jumpTo(int pos) throws RemoteException {
			player.jumpTo(pos);
			
		}
		
		@Override
		public void stop() throws RemoteException {
			player.stop();
			
		}
		
		@Override
		public void setAddType(int type) throws RemoteException {
			player.setAddType(type);
			
		}
		
		@Override
		public IBinder asBinder() {
			// TODO Auto-generated method stub

			return null;
		}
		
		@Override
		public void onDestroy() {
			
			if (player != null) {
				try {
					player.onDestroy();
				} catch (RemoteException e) {
					
				}
			}
		}

		@Override
		public void setBaseURL(String url) throws RemoteException {
			PlaybackService.setBaseURL(url);
			
		}
		
		@Override
		public void setPlaybackDevice(String playerClass, String arg) throws RemoteException {
			PlaybackService.getInstance().setPlaybackDevice(playerClass, arg);
			
		}

		@Override
		public String playbackIPC(String params) throws RemoteException {
			return player.playbackIPC(params);
		}

		@Override
		public List<String> getPlaylist() throws RemoteException {
			return player.getPlaylist();
		}
	};
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		instance = this;
		if (player == null) {
			player = LocalDevice.getInstance(null);
			
			
			Log.d("jinzora","initializing phone state listener");
			final PhoneStateListener mPhoneListener = new PhoneStateListener()
			{
			        public void onCallStateChanged(int state, String incomingNumber)
			        {
			                try {
			                        switch (state)
			                        {
			                        case TelephonyManager.CALL_STATE_RINGING:
			                        case TelephonyManager.CALL_STATE_OFFHOOK:
			                        	// pause playback
			                        	player.onCallBegin();
			                                break;

			                        case TelephonyManager.CALL_STATE_IDLE:
			                        	// resume playback
			                        	player.onCallEnd();
			                        	
			                                break;
			                        default:
			                               // Log.d("jinzora", "Unknown phone state=" + state);
			                        }
			                } catch (Exception e) { Log.e("jinzora","Error changing phone state",e); }
			        }
			};
			
			TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
			
			
			
			
			
		}
		//player = JukeboxDevice.getInstance("0");
		return mBinder;
	}
}
