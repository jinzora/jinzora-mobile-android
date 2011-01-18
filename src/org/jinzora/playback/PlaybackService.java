package org.jinzora.playback;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.jinzora.Jinzora;
import org.jinzora.R;
import org.jinzora.playback.players.LocalDevice;
import org.jinzora.playback.players.PlaybackDevice;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import edu.stanford.spout.lib.AndroidSpout;
import edu.stanford.spout.lib.NowPlayingSpout;
import edu.stanford.spout.lib.Spoutable;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PlaybackService extends Service {

	private static Method sRegisterMediaButtonEventReceiver;
	private static Method sUnregisterMediaButtonEventReceiver;
	private ComponentName mRemoteControlResponder;

	private static final String TAG = "jinzora";
	PlaybackDevice player = null;
	private static PlaybackService instance = null;
	private static String baseurl = null;

	protected NotificationManager nm;
	protected static final int NOTIFY_ID = R.layout.player;
	protected static final int JB_NOTIFY_ID = R.layout.playlist_item; // stupid
	private AudioManager mAudioManager = null;

	private JinzoraAppWidgetProvider mAppWidgetProvider = JinzoraAppWidgetProvider
			.getInstance();

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

	// public static final String TOGGLEPAUSE_ACTION =
	// "org.jinzora.musicservicecommand.togglepause";
	// public static final String NEXT_ACTION =
	// "org.jinzora.musicservicecommand.next";
	// public static final String PREVIOUS_ACTION =
	// "org.jinzora.musicservicecommand.previous";
	// public static final String PAUSE_ACTION =
	// "org.jinzora.musicservicecommand.pause";

	private static void initializeRemoteControlRegistrationMethods() {
		try {
			if (sRegisterMediaButtonEventReceiver == null) {
				sRegisterMediaButtonEventReceiver = AudioManager.class
						.getMethod("registerMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
			}
			if (sUnregisterMediaButtonEventReceiver == null) {
				sUnregisterMediaButtonEventReceiver = AudioManager.class
						.getMethod("unregisterMediaButtonEventReceiver",
								new Class[] { ComponentName.class });
			}
		} catch (NoSuchMethodException nsme) { /* Ignored */ }
	}

	static {
		initializeRemoteControlRegistrationMethods();
	}
	
	public void registerRemoteControl() {
		Log.d(TAG, "Registering remote control listener");
        try {
            if (sRegisterMediaButtonEventReceiver == null) {
                return;
            }
            sRegisterMediaButtonEventReceiver.invoke(mAudioManager,
            		mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            Log.e(TAG, "unexpected " + ie);
        }
    }
    
    public void unregisterRemoteControl() {
    	Log.d(TAG, "Unregistering remote control listener");
        try {
            if (sUnregisterMediaButtonEventReceiver == null) {
                return;
            }
            sUnregisterMediaButtonEventReceiver.invoke(mAudioManager,
                    mRemoteControlResponder);
        } catch (InvocationTargetException ite) {
            /* unpack original exception when possible */
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                /* unexpected checked exception; wrap and re-throw */
                throw new RuntimeException(ite);
            }
        } catch (IllegalAccessException ie) {
            System.err.println("unexpected " + ie);  
        }
    }

	/**
	 * TODO: Get rid of quickplay() in Jinzora and Search classes make this a
	 * sane API (not spoutable)
	 * 
	 * @param query
	 */
	private void quickplay(final Intent intent) {

		new Thread() {
			public void run() {
				try {
					// TODO: support rich queries server-side ("power search")
					StringBuffer query = new StringBuffer();
					if (intent.hasExtra("artist")) {
						query.append(" " + intent.getStringExtra("artist"));
					}
					if (intent.hasExtra("album")) {
						query.append(" @album "
								+ intent.getStringExtra("album"));
					}
					if (query.length() == 0) {
						if (intent.hasExtra("query")) {
							query.append(intent.getStringExtra("query"));
						}
					}

					String urlStr = Jinzora.getBaseURL()
							+ "&request=search&query="
							+ URLEncoder.encode(query.toString().trim(),
									"UTF-8");
					URL url = new URL(urlStr);
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					// conn.setConnectTimeout(5000);
					InputStream inStream = conn.getInputStream();
					conn.connect();

					XmlPullParserFactory factory = XmlPullParserFactory
							.newInstance();
					factory.setNamespaceAware(true);
					XmlPullParser xpp = factory.newPullParser();
					xpp.setInput(inStream, conn.getContentEncoding());

					int eventType = xpp.getEventType();
					while (eventType != XmlPullParser.END_DOCUMENT) {
						if (eventType == XmlPullParser.START_TAG
								&& xpp.getName().equals("playlink")) {
							eventType = xpp.next();

							// found a match; play it.
							Jinzora.doPlaylist(xpp.getText(), Jinzora
									.getAddType());
							return;
						}
						eventType = xpp.next();
					}
				} catch (Exception e) {
					Log.e(TAG, "Error during quicksearch", e);
				}
			};
		}.start();
	}

	public static class Intents {
		public static final String ACTION_QUICKPLAY = "org.jinzora.action.QUICKPLAY";
		public static final String ACTION_CONNECT_JUKEBOX = "org.jinzora.action.CONNECT_JUKEBOX";
		public static final String ACTION_DISCONNECT_JUKEBOX = "org.jinzora.action.DISCONNECT_JUKEBOX";

		public static final String ACTION_PLAYLIST = "org.jinzora.jukebox.PLAYLIST";
		public static final String ACTION_PLAYLIST_SYNC_REQUEST = "org.jinzora.jukebox.PLAYLIST_SYNC_REQUEST";
		public static final String ACTION_PLAYLIST_SYNC_RESPONSE = "org.jinzora.jukebox.PLAYLIST_SYNC_RESPONSE";
		public static final String ACTION_CMD_PLAY = "org.jinzora.jukebox.cmd.PLAY";
		public static final String ACTION_CMD_PAUSE = "org.jinzora.jukebox.cmd.PAUSE";
		public static final String ACTION_CMD_NEXT = "org.jinzora.jukebox.cmd.NEXT";
		public static final String ACTION_CMD_PREV = "org.jinzora.jukebox.cmd.PREV";
		public static final String ACTION_CMD_STOP = "org.jinzora.jukebox.cmd.STOP";
		public static final String ACTION_CMD_CLEAR = "org.jinzora.jukebox.cmd.CLEAR";
		public static final String ACTION_CMD_JUMPTO = "org.jinzora.jukebox.cmd.JUMPTO";
		public static final String ACTION_CMD_PLAYPAUSE = "org.jinzora.jukebox.cmd.PLAYPAUSE";

		public static final String CATEGORY_REMOTABLE = "junction.remoteintent.REMOTABLE";
	}

	public static PlaybackService getInstance() {
		if (instance == null) {
			Log.w(TAG, "PlaybackService.instance should not be null");
			instance = new PlaybackService();
		}
		return instance;
	}

	private BroadcastReceiver mCommandReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (player == null) {
				Log.w(TAG, "Receieved a command but have a null player");
				return;
			}

			// If we received it, abort it.
			try {
				abortBroadcast();
			} catch (Exception e) {
			}

			try {
				String action = intent.getAction();
				String cmd = intent.getStringExtra("command");

				if (Intents.ACTION_PLAYLIST.equals(action)) {
					String playlist = intent.getStringExtra("playlist");
					int addtype = intent.getIntExtra("addtype", 0);

					try {
						mBinder.updatePlaylist(playlist, addtype);
						return;
					} catch (RemoteException e) {
						Log.e(TAG, "Could not update playlist", e);
					}

					return;
				}

				if (Intents.ACTION_PLAYLIST_SYNC_REQUEST.equals(action)) {
					final int SYNC_BUFFER_WAIT = 5250;
					List<String> names = player.getPlaylistNames();
					List<String> urls = player.getPlaylistURLs();
					if (urls.size() == 0) {
						return;
					}
					int plPos = player.getPlaylistPos();
					int seekPos = player.getSeekPos();

					Intent sync = new Intent(
							Intents.ACTION_PLAYLIST_SYNC_RESPONSE);
					sync.addCategory(Intents.CATEGORY_REMOTABLE);
					sync.putExtra("names", names.toArray(new String[] {}));
					sync.putExtra("urls", urls.toArray(new String[] {}));
					sync.putExtra("pl_pos", plPos);
					sync.putExtra("seek_pos", seekPos + SYNC_BUFFER_WAIT);

					sendBroadcast(sync);

					new Thread() {
						public void run() {
							try {
								Thread.sleep(SYNC_BUFFER_WAIT);
								player.pause();
							} catch (Exception e) {

							}
						};
					}.start();

					return;
				}

				if (Intents.ACTION_CMD_JUMPTO.equals(action)) {
					int pos = intent.getIntExtra("pos", 0);
					if (pos > 0)
						player.jumpTo(pos);
				} else if (CMDNEXT.equals(cmd)
						|| Intents.ACTION_CMD_NEXT.equals(action)) {
					player.next();
				} else if (CMDPREVIOUS.equals(cmd)
						|| Intents.ACTION_CMD_PREV.equals(action)) {
					player.prev();
				} else if (CMDTOGGLEPAUSE.equals(cmd)
						|| Intents.ACTION_CMD_PLAYPAUSE.equals(action)) {
					player.playpause();
				} else if (CMDPAUSE.equals(cmd)
						|| Intents.ACTION_CMD_PAUSE.equals(action)) {
					player.pause();
				} else if (Intents.ACTION_CMD_PLAY.equals(action)) {
					player.play();
				} else if (CMDSTOP.equals(cmd)) {
					player.stop();
				} else if (JinzoraAppWidgetProvider.CMDAPPWIDGETUPDATE
						.equals(cmd)) {
					// Someone asked us to refresh a set of specific widgets,
					// probably
					// because they were just added.
					int[] appWidgetIds = intent
							.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
					mAppWidgetProvider.performUpdate(PlaybackService.this,
							appWidgetIds);
				}

			} catch (Exception e) {
				Log.e(TAG, "Error handling broadcast", e);
			}
		}
	};

	/**
	 * Returns an intent that issues a quickplay request. Can be broadcasted or
	 * used via startService. Accepts string extras: query, artist, album
	 * 
	 * @return
	 */
	public static Intent getQuickplayIntent() {
		Intent i = new Intent();
		i.setAction(Intents.ACTION_QUICKPLAY);

		// service only:
		i.setComponent(new ComponentName(Jinzora.PACKAGE, PlaybackService.class
				.getName()));

		// for broadcasting:
		// i.setPackage(Jinzora.PACKAGE);

		return i;
	}

	public void onStart(Intent intent, int startId) {
		if (intent != null
				&& Intents.ACTION_QUICKPLAY
						.equalsIgnoreCase(intent.getAction())) {
			quickplay(intent);
		}

		if (Intents.ACTION_CONNECT_JUKEBOX.equalsIgnoreCase(intent.getAction())) {
			// TODO: this is where the remote intent stuff is.
			// we'd like to remove it.
			// TODO: props. Mad props.
			// TODO: instead of just passing "transfer=true|false", support
			// queuing/replacing/etc
			installRemoteIntentFilter(intent);

			// Notification so they can remove this jukebox
			String notice = "Connected to remote jukebox. Click to disconnect.";
			Intent cancelIntent = new Intent(this, Jinzora.class);
			cancelIntent.setAction(Intents.ACTION_DISCONNECT_JUKEBOX);
			Notification notification = new Notification(
					android.R.drawable.ic_btn_speak_now, notice, System
							.currentTimeMillis());
			PendingIntent pending = PendingIntent.getActivity(this,
					JB_NOTIFY_ID, cancelIntent, 0);

			notification.setLatestEventInfo(this, "Jinzora Mobile", notice,
					pending);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			nm.notify(JB_NOTIFY_ID, notification);
		}

		if (Intents.ACTION_DISCONNECT_JUKEBOX.equalsIgnoreCase(intent
				.getAction())) {
			nm.cancel(JB_NOTIFY_ID);
			// hacky end to a hacky beginning (remote intents)
			Intent holla = new Intent("profile.tag.LOAD");
			holla.putExtra("tag", "later");
			sendBroadcast(holla);
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "PlaybackService::onCreate called");
		nm = (NotificationManager) this
				.getSystemService(Service.NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mRemoteControlResponder = new ComponentName(this, RemoteControlReceiver.class);

		/** Playback commands (internal use) **/
		IntentFilter commandFilter = new IntentFilter();
		commandFilter.addAction(SERVICECMD);
		commandFilter.addAction(Intents.ACTION_CMD_PLAYPAUSE);
		commandFilter.addAction(Intents.ACTION_CMD_PAUSE);
		commandFilter.addAction(Intents.ACTION_CMD_PLAY);
		commandFilter.addAction(Intents.ACTION_CMD_PREV);
		commandFilter.addAction(Intents.ACTION_CMD_NEXT);
		commandFilter.addAction(Intents.ACTION_CMD_STOP);
		commandFilter.addAction(Intents.ACTION_CMD_CLEAR);
		commandFilter.addAction(Intents.ACTION_CMD_JUMPTO);
		commandFilter.addAction(Intents.ACTION_PLAYLIST);
		commandFilter.addAction(Intents.ACTION_PLAYLIST_SYNC_REQUEST);

		commandFilter.addCategory(Intents.CATEGORY_REMOTABLE);

		registerReceiver(mCommandReceiver, commandFilter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		unregisterRemoteControl();
		unregisterReceiver(mCommandReceiver);
	}

	public boolean isPlaying() {
		return (player != null && player.isPlaying());
	}

	public String getArtistName() {
		try {
			if (player != null)
				return player.getArtistName();
		} catch (Exception e) {
			Log.e(TAG, "Error getting remote info", e);
		}

		return null;
	}

	public String getTrackName() {
		try {
			if (player != null)
				return player.getTrackName();
		} catch (Exception e) {
			Log.e(TAG, "Error getting remote info", e);
		}

		return null;
	}

	public void notifyPlaying(boolean firstPlay) {
		String artist = null, track = null;

		try {
			artist = player.getArtistName();
			track = player.getTrackName();
		} catch (Exception e) {
			Log.e(TAG, "could not get artist/track info", e);
			return;
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
					android.R.drawable.ic_media_play, notice, System
							.currentTimeMillis());
			PendingIntent pending = PendingIntent.getActivity(this, 0,
					new Intent(this, Jinzora.class), 0);
			notification.setLatestEventInfo(this, "Jinzora Mobile", notice,
					pending);
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			nm.notify(NOTIFY_ID, notification);
			// startForeground(NOTIFY_ID,notification); // API level 5
		} catch (Exception e) {
			Log.w(TAG, "notification error", e);
		}

		try {
			/* Broadcast */
			Intent playlistIntent = new Intent(PLAYSTATE_CHANGED);
			playlistIntent.putExtra("position", player.getPlaylistPos());
			playlistIntent.putExtra("artist", artist);
			playlistIntent.putExtra("track", track);
			sendBroadcast(playlistIntent);

			/* Spout */
			if (firstPlay) {
				Spoutable spoutable = new NowPlayingSpout(artist, track);
				AndroidSpout.spout(this, spoutable);
			}

			mAppWidgetProvider.notifyChange(this, PLAYSTATE_CHANGED);
		} catch (Exception e) {
			Log.w(TAG, "broadcast error", e);
		}
	}

	public void notifyPaused() {
		try {
			nm.cancel(NOTIFY_ID);

			Intent playlistIntent = new Intent(PLAYSTATE_CHANGED);
			playlistIntent.putExtra("position", player.getPlaylistPos());
			playlistIntent.putExtra("artist", player.getArtistName());
			playlistIntent.putExtra("track", player.getTrackName());
			sendBroadcast(playlistIntent);

			mAppWidgetProvider.notifyChange(this, PLAYSTATE_CHANGED);
		} catch (Exception e) {
			Log.d(TAG, "notification error", e);
		}
	}

	public void killNotifications() {
		notifyPaused(); // same thing (for now)
	}

	/**
	 * This service and the associated PlaybackServices are in a different
	 * process from the main Jinzora activity, and cannot share static variables
	 * / preferences. Use this method to retrieve the media server url for
	 * classes running in this process.
	 */
	public static String getBaseURL() {
		return baseurl;
	}

	public static void setBaseURL(String url) {
		baseurl = url;
	}

	public void setPlaybackDevice(String playerClass, String arg) {
		try {
			Class<PlaybackDevice> pc = (Class<PlaybackDevice>) Class
					.forName(playerClass);
			Method m = pc
					.getMethod("getInstance", new Class[] { String.class });
			if (player != null) {
				try {
					player.onDestroy();
				} catch (Exception e) {
					Log.e(TAG, "error destroying player", e);
				}
			}
			player = (PlaybackDevice) m.invoke(null, arg);

		} catch (Exception e) {
			Log.e(TAG, "error instantiating player", e);
		}
	}

	/**
	 * Note: We switched from a service-binding approach for IPC to a
	 * broadcast-based approach, to support remote playback. This class is
	 * mostly unnecessary.
	 * 
	 * BJD 5/20/2010
	 */
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
		public void registerRemoteControl() {
			PlaybackService.this.registerRemoteControl();
		}
		
		@Override
		public void unregisterRemoteControl() {
			PlaybackService.this.unregisterRemoteControl();
		}

		@Override
		public void updatePlaylist(String pl, int addType)
				throws RemoteException {
			player.updatePlaylist(pl, addType);

			/* Assumes synchronous player.playlist(). Might have to change this. */
			try {
				/* Broadcast */
				Intent playlistIntent = new Intent(PLAYLIST_UPDATED);
				sendBroadcast(playlistIntent);

			} catch (Exception e) {
				Log.w(TAG, "broadcast error", e);
			}

		}

		@Override
		public void clear() throws RemoteException {
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_CLEAR);
		}

		@Override
		public void next() throws RemoteException {
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_NEXT);
		}

		@Override
		public void pause() throws RemoteException {
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_PAUSE);
		}

		@Override
		public void prev() throws RemoteException {
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_PREV);
		}

		@Override
		public void play() throws RemoteException {
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_PLAY);
		}

		@Override
		public void playpause() throws RemoteException {
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_PLAYPAUSE);
		}

		@Override
		public void jumpTo(int pos) throws RemoteException {
			Bundle extras = new Bundle();
			extras.putInt("pos", pos);
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_JUMPTO,
					extras);
		}

		@Override
		public void stop() throws RemoteException {
			broadcastCommand(PlaybackService.this, Intents.ACTION_CMD_STOP);
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
		public void setPlaybackDevice(String playerClass, String arg)
				throws RemoteException {
			PlaybackService.getInstance().setPlaybackDevice(playerClass, arg);

		}

		@Override
		public String playbackIPC(String params) throws RemoteException {
			return player.playbackIPC(params);
		}

		@Override
		public List<String> getPlaylistNames() throws RemoteException {
			return player.getPlaylistNames();
		}

		@Override
		public List<String> getPlaylistURLs() throws RemoteException {
			return player.getPlaylistURLs();
		}

		@Override
		public int getPlaylistPos() throws RemoteException {
			return player.getPlaylistPos();
		}

		@Override
		public int getSeekPos() throws RemoteException {
			return player.getSeekPos();
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

	public static void broadcastCommand(Context c, String cmd) {
		Intent i = new Intent(cmd);
		i.addCategory(Intents.CATEGORY_REMOTABLE);
		Log.d("junction", "broadcasting " + i);
		c.sendOrderedBroadcast(i, null);
	}

	public static void broadcastCommand(Context c, String cmd, Bundle extras) {
		Intent i = new Intent(cmd);
		i.addCategory(Intents.CATEGORY_REMOTABLE);
		i.putExtras(extras);
		c.sendOrderedBroadcast(i, null);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		instance = this;
		
		if (player == null) {
			player = LocalDevice.getInstance(null);

			final PhoneStateListener mPhoneListener = new PhoneStateListener() {
				public void onCallStateChanged(int state, String incomingNumber) {
					try {
						switch (state) {
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
							// Log.d(TAG, "Unknown phone state=" + state);
						}
					} catch (Exception e) {
						Log.e(TAG, "Error changing phone state", e);
					}
				}
			};

			TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

		}
		// player = JukeboxDevice.getInstance("0");
		return mBinder;
	}

	private/* hacky */void installRemoteIntentFilter(Intent inbound) {
		if (!(inbound.hasExtra("invitationURI"))) {
			Log.d(TAG, "could not set up remote jukebox; no activity given");
			return;
		}

		// Ugh. Should have made one org.jinzora.jukebox.COMMAND action.
		IntentFilter filter = new IntentFilter();
		filter.addAction("org.jinzora.jukebox.PLAYLIST");
		filter.addAction("org.jinzora.jukebox.PLAYLIST_SYNC_RESPONSE");
		filter.addAction("org.jinzora.jukebox.cmd.PLAY");
		filter.addAction("org.jinzora.jukebox.cmd.PAUSE");
		filter.addAction("org.jinzora.jukebox.cmd.NEXT");
		filter.addAction("org.jinzora.jukebox.cmd.PREV");
		filter.addAction("org.jinzora.jukebox.cmd.STOP");
		filter.addAction("org.jinzora.jukebox.cmd.CLEAR");
		filter.addAction("org.jinzora.jukebox.cmd.JUMPTO");
		filter.addAction("org.jinzora.jukebox.cmd.PLAYPAUSE");
		filter.addCategory("junction.remoteintent.REMOTABLE");

		Intent intent = new Intent("junction.remoteintent.INSTALL_FILTER");
		intent.putExtra("activityURI", inbound.getStringExtra("invitationURI"));
		intent.putExtra("activityID", "org.jinzora.jukebox");
		intent.putExtra("intentFilter", filter);
		intent.putExtra("method", "junction"); // vs REST

		// Log.d("junction","sending intent " + intent);
		sendBroadcast(intent);

		/* Sync request */
		boolean tx = inbound.getBooleanExtra("transfer", false);
		if (tx) {
			Intent sync = new Intent(
					PlaybackService.Intents.ACTION_PLAYLIST_SYNC_REQUEST);
			sync.addCategory(PlaybackService.Intents.CATEGORY_REMOTABLE);
			sendBroadcast(sync);
		} else {
			try {
				player.clear();
			} catch (RemoteException e) {
				Log.e(TAG,
						"failed to clear playlist while connecting to jukebox");
			}
		}
	}
}