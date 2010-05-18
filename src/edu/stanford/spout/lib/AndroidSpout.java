package edu.stanford.spout.lib;

import android.content.Context;
import android.content.Intent;

public class AndroidSpout {
	public static class Intents {
		public static String SPOUT_ACTION="android.intent.action.SPOUT";
	}
	
	public static Intent spoutableToIntent(Spoutable spout) {
		Intent intent = new Intent(Intents.SPOUT_ACTION);
		intent.putExtra("namespace", spout.getNamespace());
		intent.putExtra("type",spout.getType());
		intent.putExtra("tags",spout.getTags());
		intent.putExtra("json", spout.getJSONRepresentation().toString());
		intent.putExtra("text", spout.getTextRepresentation());
		intent.putExtra("html", spout.getHTMLRepresentation());
		
		return intent;
	}
	
	public static void spout(Context c, Spoutable spout) {
		Intent intent = spoutableToIntent(spout);
		c.sendBroadcast(intent);
	}
}
