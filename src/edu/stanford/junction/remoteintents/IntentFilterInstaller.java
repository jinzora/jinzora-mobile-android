package edu.stanford.junction.remoteintents;

import org.jinzora.playback.PlaybackService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


/**
 * A broadcast receiver that detects when
 * the user has joined a remote Jinzora
 * jukebox activity.
 * 
 * The receiver listens for an intent with
 * action: "junction.notify.JOINED_ACTIVITY"
 * 
 * @author bjdodson
 *
 */
public class IntentFilterInstaller extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent inbound) {
			
		if (!(inbound.hasExtra("activityID") && inbound.getStringExtra("activityID").equals("org.jinzora.jukebox"))) {
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
		intent.putExtras(inbound.getExtras());
		intent.putExtra("intentFilter", filter);
		intent.putExtra("method", "junction"); // vs REST
		
		//Log.d("junction","sending intent " + intent);
		context.sendBroadcast(intent);
		
		/* Sync request */
		Intent sync = new Intent(PlaybackService.Intents.ACTION_PLAYLIST_SYNC_REQUEST);
		sync.addCategory(PlaybackService.Intents.CATEGORY_REMOTABLE);
		context.sendBroadcast(sync);
	}
}
