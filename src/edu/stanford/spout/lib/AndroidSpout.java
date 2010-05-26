package edu.stanford.spout.lib;

import org.json.JSONObject;

import edu.stanford.spout.types.SpoutContainer;
import edu.stanford.spout.lib.Spoutable;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * This class does the work of broadcasting a
 * Spoutable across Android, so it can be broadcasted
 * to other Spout users.
 * 
 * A Spoutable is converted into an Android Intent
 * and broadcasted. A broadcasted Spout may not be
 * received by an application. The result is that no
 * Spout will be sent, but the application will not
 * error.
 * 
 * @author bjdodson
 *
 */
public class AndroidSpout {
	public static final String LT = "spout";
	
	public static class Intents {
		public static String SPOUT_ACTION="android.intent.action.SPOUT";
		
		public static String EXTRA_NS = "namespace";
		public static String EXTRA_TYPE = "type";
		public static String EXTRA_TAGS = "tags";
		public static String EXTRA_JSON = "json";
		public static String EXTRA_TEXT = "text";
		public static String EXTRA_HTML = "html";
	}
	
	/**
	 * Converts an Intent into a Spoutable object.
	 * 
	 * @param intent
	 * @return
	 */
	public static Spoutable intentToSpoutable(Intent intent) {
		Bundle b = intent.getExtras();
		String ns = 		b.getString(Intents.EXTRA_NS);
		String type = 		b.getString(Intents.EXTRA_TYPE);
		String[] tags = 	b.getStringArray(Intents.EXTRA_TAGS);
		String jsonTxt = 	b.getString(Intents.EXTRA_JSON);
		String text = 		b.getString(Intents.EXTRA_TEXT);
		String html = 		b.getString(Intents.EXTRA_HTML);
		
		Spoutable spout=null;
		try {
			JSONObject json = new JSONObject(jsonTxt);
			spout = new SpoutContainer(ns, type, tags, text, html, json);
		} catch (Exception e) {
			Log.e(LT,"could not construct spoutable",e);
		}
		
		return spout;
	}
	
	/**
	 * Converts a Spoutable into an Intent
	 * 
	 * @param spout
	 * @return
	 */
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
	
	/**
	 * Broadcasts a Spoutable as an Intent.
	 * @param c
	 * @param spout
	 */
	public static void spout(Context c, Spoutable spout) {
		Intent intent = spoutableToIntent(spout);
		c.sendBroadcast(intent);
	}
}
