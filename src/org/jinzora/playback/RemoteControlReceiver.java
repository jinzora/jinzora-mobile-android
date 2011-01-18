package org.jinzora.playback;

import org.jinzora.playback.PlaybackService.Intents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver {
	public static final String TAG = "jinzora";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
        	KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        	if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
        		switch (keyEvent.getKeyCode()) {
        		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
        		case KeyEvent.KEYCODE_HEADSETHOOK:
        			PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_PLAYPAUSE);
        			break;
        		case KeyEvent.KEYCODE_MEDIA_NEXT:
        			PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_NEXT);
        			break;
        		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
        			PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_PREV);
        			break;
        		case KeyEvent.KEYCODE_MEDIA_STOP:
        			PlaybackService.broadcastCommand(context, Intents.ACTION_CMD_STOP);
        			break;
        		default:
        			Log.w(TAG, "Uknown key event: " + keyEvent.getKeyCode());
        		}
        	}
        }
    }
}