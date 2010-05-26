package edu.stanford.spout.types;

import org.json.JSONObject;

import edu.stanford.spout.lib.Spoutable;

/**
 * A class that can represent any Spout in a generic way.
 * @author bjdodson
 * @author David Kettler
 *
 */
public class SpoutContainer extends Spoutable {
	private JSONObject json;
	private String ns;
	private String type;
	private String[] tags;
	private String text;
	private String html;
	
	public SpoutContainer(String namespace, String type, String[] tags, String text, String html, JSONObject json) {
		this.ns=namespace;
		this.type=type;
		this.tags=tags;
		this.text=text;
		this.html=html;
		this.json=json;
	}
	
	@Override
	public String getTextRepresentation() {
		if (text != null) return text;
		return json.toString();
	}
	
	@Override
	public String getHTMLRepresentation() {
		return html;
	}

	@Override
	public JSONObject getJSONRepresentation() {
		return json;
	}

	@Override
	public String getNamespace() {
		return ns;
	}

	@Override
	public String getType() {
		return type;
	}
	
	@Override
	public String[] getTags() {
		return tags;
	}
}
