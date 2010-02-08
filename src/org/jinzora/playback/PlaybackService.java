package org.jinzora.playback;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.jinzora.Jinzora;
import org.jinzora.R;
import org.jinzora.playback.players.LocalDevice;
import org.jinzora.playback.players.PlaybackDevice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PlaybackService extends Service {
	PlaybackDevice player = null;
	private static PlaybackService instance = null;
	private static String baseurl = null;
	
	protected NotificationManager nm;
	protected static final int NOTIFY_ID = R.layout.player;

	private JinzoraAppWidgetProvider mAppWidgetProvider = JinzoraAppWidgetProvider.getInstance();
	
	
	public static final String SERVICECMD = "org.jinzora.musicservicecommand";
	public static final String CMDNAME = "command";
    public static final String CMDTOGGLEPAUSE = "togglepause";
    public static final String CMDSTOP = "stop";
    public static final String CMDPAUSE = "pause";
    public static final String CMDPREVIOUS = "previous";
    public static final String CMDNEXT = "next";
	
	
	public static final String PLAYSTATE_CHANGED = "org.jinzora.playstatechanged";
	public static final String META_CHANGED = "org.jinzora.metachanged";
	public static final String PLAYBACK_COMPLETE = "org.jinzora.playbackcomplete";
	public static final String PLAYLIST_UPDATED = "org.jinzora.playlistupdated";
	
	public static final String TOGGLEPAUSE_ACTION = "org.jinzora.musicservicecommand.togglepause";
	public static final String NEXT_ACTION = "org.jinzora.musicservicecommand.next";
	public static final String PREVIOUS_ACTION = "org.jinzora.musicservicecommand.previous";
	public static final String PAUSE_ACTION = "org.jinzora.musicservicecommand.pause";
	
	
	public static PlaybackService getInstance() {
		if (instance == null) {
			Log.w("jinzora","PlaybackService.instance should not be null");
			instance = new PlaybackService();
		}
		return instance;
	}
	
	
	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (player == null) {
        		Log.w("jinzora","Receieved a command but have a null player");
        		return;
        	}
        	try {
	            String action = intent.getAction();
	            String cmd = intent.getStringExtra("command");
	            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
	                player.next();
	            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
	                player.prev();
	            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
	                player.pause();
	            	/*if (player.isPlaying()) {
	                    player.pause();
	                } else {
	                    player.play();
	                }*/
	            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
	                player.pause();
	            } else if (CMDSTOP.equals(cmd)) {
	                player.stop();
	            } else if (JinzoraAppWidgetProvider.CMDAPPWIDGETUPDATE.equals(cmd)) {
	                // Someone asked us to refresh a set of specific widgets, probably
	                // because they were just added.
	                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
	                mAppWidgetProvider.performUpdate(PlaybackService.this, appWidgetIds);
	            }
        	} catch (Exception e) {
        		Log.e("jinzora","Error handling broadcast",e);
        	}
        }
    };
	
	
	@Override
	public void onCreate() {
		super.onCreate();
		nm = (NotificationManager) this.getSystemService(Service.NOTIFICATION_SERVICE);
		
		IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        
        registerReceiver(mIntentReceiver, commandFilter);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(mIntentReceiver);
	}
	
	public boolean isPlaying() {
		return (player != null && player.isPlaying());
	}
	
	public String getArtistName() {
		try {
			if (player != null) return player.getArtistName();
		} catch (Exception e) {
			Log.e("jinzora","Error getting remote info",e);
		}
		
		return null;
	}
	
	public String getTrackName() {
		try {
			if (player != null) return player.getTrackName();
		} catch (Exception e) {
			Log.e("jinzora","Error getting remote info",e);
		}
		
		return null;
	}
	
	
	public void notifyPlaying() {
		String artist = null, track = null;
		
		try {
			artist = player.getArtistName();
			track = player.getTrackName();
		} catch (Exception e ) {
			Log.e("jinzora","could not get artist/track info",e);
		}
		try {
			String notice;
			if (artist != null && track != null) {
				notice = artist + " - " + track;
			} else {
				notice = track;
			}
			/* Notification icon */
			Notification notification = new Notification(
					android.R.drawable.ic_media_play, notice, System.currentTimeMillis());
			PendingIntent pending = PendingIntent.getActivity(this, 0,
	                								new Intent(this, Jinzora.class), 0);
			notification.setLatestEventInfo(this, "Jinzora Mobile", notice, pending);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			nm.notify(NOTIFY_ID, notification);
			//startForeground(NOTIFY_ID,notification); // API level 5
		} catch (Exception e) {
			Log.w("jinzora","notification error",e);
		}
		
		try {
			/* Broadcast */
			Intent playlistIntent = new Intent(PLAYSTATE_CHANGED);
			playlistIntent.putExtra("position", player.getPlaylistPos());
			playlistIntent.putExtra("artist",artist);
			playlistIntent.putExtra("track",track);
			sendBroadcast(playlistIntent);
			
			mAppWidgetProvider.notifyChange(this, PLAYSTATE_CHANGED);
		} catch (Exception e) {
			Log.w("jinzora","broadcast error",e);
		}
	}
	
	public void notifyPaused() {
		try {
			nm.cancel(NOTIFY_ID);
			/*
			Notification notification = new Notification(
					android.R.drawable.ic_media_pause, "Paused", System.currentTimeMillis());
			PendingIntent pending = PendingIntent.getActivity(this.service, 0,
	                								new Intent(this.service, Jinzora.class), 0);
			notification.setLatestEventInfo(this.service, "Jinzora Mobile", "Paused", pending);
			
			nm.notify(NOTIFY_ID, notification);
			*/
			
			Intent playlistIntent = new Intent(PLAYSTATE_CHANGED);
			playlistIntent.putExtra("position", player.getPlaylistPos());
			playlistIntent.putExtra("artist", player.getArtistName());
			playlistIntent.putExtra("track",player.getTrackName());
			sendBroadcast(playlistIntent);
			mAppWidgetProvider.notifyChange(this, PLAYSTATE_CHANGED);
		} catch (Exception e) {
			Log.d("jinzora","notification error",e);
		}
	}
	
	public void killNotifications() {
		nm.cancel(NOTIFY_ID);
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
		public void playlist(String pl, int addType) throws RemoteException {
			player.playlist(pl, addType);
			
			/* Assumes synchronous player.playlist(). Might have to change this. */
			try {
				/* Broadcast */
				Intent playlistIntent = new Intent(PLAYLIST_UPDATED);
				sendBroadcast(playlistIntent);
				
			} catch (Exception e) {
				Log.w("jinzora","broadcast error",e);
			}
			
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
		public IBinder asBinder() {
			// TODO Auto-generated method stub

			return null;
		}
		
		@Override
		public void onDestroy() {
			nm.cancel(NOTIFY_ID);
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

		@Override
		public int getPlaylistPos() throws RemoteException {
			return player.getPlaylistPos();
		}

		@Override
		public void queueNext(int pos) throws RemoteException {
			player.queueNext(pos);
		}
		
		@Override
		public String getArtistName() throws RemoteException {
			return player.getArtistName();
		}
		
		@Override
		public String getTrackName() throws RemoteException {
			// TODO Auto-generated method stub
			return player.getTrackName();
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