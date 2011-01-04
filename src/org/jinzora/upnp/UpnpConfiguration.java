package org.jinzora.upnp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.ManufacturerDetails;
import org.teleal.cling.model.meta.ModelDetails;
import org.teleal.cling.model.types.DLNACaps;
import org.teleal.cling.model.types.DLNADoc;
import org.teleal.cling.model.types.UDN;

import android.content.Context;
import android.content.SharedPreferences;

public class UpnpConfiguration {
	private final SharedPreferences mPreferences;
	
	public UpnpConfiguration(Context c) {
		mPreferences = c.getSharedPreferences("main", 0);
	}
	
	public DeviceDetails getDeviceDetails() {
		return new DeviceDetails(getServiceName(),
				new ManufacturerDetails("Jinzora"), 
				new ModelDetails("Jinzora", "Provides access to a Jinzora media server.","v1"),
				new DLNADoc[]{
                    new DLNADoc("DMS", DLNADoc.Version.V1_5),
                    new DLNADoc("M-DMS", DLNADoc.Version.V1_5)
                },
                new DLNACaps(new String[] {
            		//"av-upload", "image-upload", "audio-upload"
                }));
	}
	
	public DeviceIdentity getDeviceIdentity() {
		return new DeviceIdentity(UDN
				.uniqueSystemIdentifier("Jinzora Media Server v1"));
	}
	
	public String getServiceName() {
		return mPreferences.getString("upnp.service.name", "Jinzora");
	}
	
	public String getJinzoraEndpoint() {
		String base = mPreferences.getString("site", null);
		if (base == null) return null;
		if (!base.endsWith("/")) {
			base += "/";
		}
		base += "api.php?";
		String username = mPreferences.getString("username", null);
		if (username == null) {
			return base + "1=1";
		}
		
		try {
			base += "user=" + URLEncoder.encode(username, "UTF-8");
			String password = mPreferences.getString("password", null);
			if (password != null) {
				base += "&pass=" + URLEncoder.encode(password, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) { /* Ignored */ }
		return base;
	}
	
	public boolean useWMPCompat() {
		return false;
	}
}
