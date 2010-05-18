package edu.stanford.spout.lib;

import org.json.JSONObject;

public class NowPlayingSpout extends Spoutable {
	private JSONObject mJSON = null;
	
	/**
	 * TODO: support more information:
	 * album, playlink, trackno
	 * 
	 * @param artist
	 * @param track
	 */
	public NowPlayingSpout(String artist, String track) {
		mJSON = new JSONObject();
		try {
			mJSON.put("artist",artist);
			mJSON.put("track",track);
		}catch(Exception e){}
	}
	
	@Override
	public JSONObject getJSONRepresentation() {
		return mJSON;
	}

	@Override
	public String getNamespace() {
		return "org.jinzora";
	}

	@Override
	public String getTextRepresentation() {
		return "Now Playing: " + mJSON.optString("artist") + " - " + mJSON.optString("track");
	}

	@Override
	public String getType() {
		return "nowplaying";
	}

}
