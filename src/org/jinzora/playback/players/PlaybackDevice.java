package org.jinzora.playback.players;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jinzora.Jinzora;
import org.jinzora.playback.PlaybackInterface;

import android.os.RemoteException;
import android.util.Log;

public abstract class PlaybackDevice implements PlaybackInterface {
	protected List<String> playlist = null;
	protected int currentAddType;
	
	protected static final int ADD_REPLACE 	= 0;
	protected static final int ADD_END 		= 1;
	protected static final int ADD_CURRENT 	= 2;
	
	
	@Override
	public void setBaseURL(String url) throws RemoteException {
		// Devices don't need this method.
	}
	
	@Override
	public void setPlaybackDevice(String playerClass, String arg) throws RemoteException {
		// Devices don't need this method.
	}
	
	public void setAddType(int type) {
		currentAddType = type;
	}
	
	public int getAddType() {
		return currentAddType;
	}
	
	public String getAddTypeString() {
		switch (currentAddType) {
		case 0:
			return "REPLACE";
		case 1:
			return "END";
		case 2:
			return "CURRENT";
		}
		return null;
	}
}
