package org.jinzora.playback;

import org.jinzora.Jinzora;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class QuickplayBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		intent.setComponent(new ComponentName(Jinzora.PACKAGE, PlaybackService.class.getName()));
		context.startService(intent);
	}

}
