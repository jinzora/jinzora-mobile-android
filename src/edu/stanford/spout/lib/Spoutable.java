package edu.stanford.spout.lib;

import java.util.UUID;

import org.json.JSONObject;



/**
 * An object that can be sent as a message
 * in weMail.
 * 
 * Example implementations: GPSLocation, StatusUpdate, 
 * NamespacedStatusUpdate, Image, HTMLMessage 
 * 
 * @author bjdodson
 *
 */
public abstract class Spoutable {
	/**
	 * Example: {"artist":"Pearl Jam","track":"Jeremy","album":"Ten","link":"..."...}
	 * @return
	 */
	public abstract JSONObject getJSONRepresentation();

	/**
	 * Example: "<div><b>Pearl Jam - Jeremy</b><br/>Album: Ten</div>"
	 * @return
	 */
	public String getHTMLRepresentation() {
		return null;
	}

	/**
	 * Example: "Now Playing: Pearl Jam - Jeremy"
	 * @return
	 */
	public abstract String getTextRepresentation();


	/**
	 * Example: "http://types.jinzora.org"
	 * @return
	 */
	public abstract String getNamespace();
	
	/**
	 * Example: "nowplaying"
	 * @return
	 */
	public abstract String getType();
	
	public String[] getTags() { return null; }
	
	
	@Override
	public String toString() {
		return getTextRepresentation();
	}
	
	private String _ID;
	public final String getID() {
		if (_ID == null)
			_ID = UUID.randomUUID().toString();
		
		return _ID;
	}
}