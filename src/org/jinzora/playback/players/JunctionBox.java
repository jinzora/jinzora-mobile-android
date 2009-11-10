package org.jinzora.playback.players;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jinzora.playback.players.views.JunctionBoxView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import edu.stanford.prpl.junction.api.activity.ActivityDescription;
import edu.stanford.prpl.junction.api.activity.JunctionActor;
import edu.stanford.prpl.junction.api.messaging.MessageHandler;
import edu.stanford.prpl.junction.api.messaging.MessageHeader;
import edu.stanford.prpl.junction.impl.AndroidJunctionMaker;
import edu.stanford.prpl.junction.impl.JunctionMaker;



/** 
 * A jukebox implementation using Junction.
 * This class is identical to the LocalDevice class
 * but additionally accepts commands from a 
 * Junction activity.
 * 
 * TODO: make getInstance take in an Activity.
 * Use to present GUI.
 *  For remote, show JOIN dialog. For JB, show invitation (?)
 * @author bdodson
 *
 */
public class JunctionBox extends LocalDevice {
	private JinzoraActor mActor = null; 
	protected boolean playLocally = true;
	
	/**
	 * Constructor.
	 * When we initially create a jukebox,
	 * set up an activity with us as the
	 * "jukebox" and "remote" roles.
	 */
	private JunctionBox(URI invitationURI) {
		super();
		
		Log.d("jinzora","creating new junctionbox");
		if (mActor == null) {
			Log.d("jinzora","mActor is null");
			mActor = new JinzoraActor(this);
			
			ActivityDescription desc = new ActivityDescription();
			desc.setActivityID("org.jinzora.jukebox");
			desc.setFriendlyName("Jinzora Jukebox");
			
			JSONObject androidPlatform = null;
			try {
				new JSONObject("{platform:\"android\",package:\"org.jinzora.Jinzora\"}");
			} catch (Exception e) { e.printStackTrace(); }
			desc.addRolePlatform("jukebox", androidPlatform);
			desc.addRolePlatform("remote", androidPlatform);
			// jukebox and remote roles.
			
			
			// bind to junction
			if (invitationURI == null) {
				JunctionMaker.getInstance("prpl.stanford.edu").newJunction(desc, mActor);
			} else {
				JunctionMaker.getInstance("prpl.stanford.edu").newJunction(invitationURI, mActor);
			}
		} else {
			Log.w("jinzora","mActor was not null!");
		}
	}
	
	private static JunctionBox instance = null;
	public static JunctionBox getInstance(String arg) {
		if (arg.startsWith("junction://")) {
			try {
				instance = new JunctionBox(new URI(arg));
				return instance;
			} catch (Exception e) {
				Log.e("jinzora","Error converting junction URI " + arg, e);
			}
		}
			
		return getInstance();
	}
	public static JunctionBox getInstance() {
		if (instance == null) {
			instance = new JunctionBox(null);
		}
		return instance;
	}
	
	
	protected void superClear() throws RemoteException { super.clear(); }
	protected void superNext() throws RemoteException { super.next(); }
	protected void superPrev() throws RemoteException { super.prev(); }
	protected void superStop() throws RemoteException { super.stop(); }
	protected void superPause() throws RemoteException { super.pause(); }
	protected void superJumpto(int pos) throws RemoteException { super.jumpTo(pos); }
	
	/**
	 * Playback commands.
	 * If received from button press,
	 * send command over Junction.
	 * 
	 * If received from Jx, call to super.
	 */

	@Override
	public void clear() throws RemoteException {
		try {
			sendCommand("clear");
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public synchronized void next() throws RemoteException {
		try {
			sendCommand("next");
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public synchronized void pause() throws RemoteException {
		try {
			sendCommand("pause");
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}

	@Override
	public synchronized void prev() throws RemoteException {
		try {
			sendCommand("prev");
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}
	
	@Override
	public void stop() throws RemoteException {
		try {
			sendCommand("stop");
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}
	
	@Override
	public void jumpTo(int pos){
		try {
			JSONObject msg = new JSONObject ("{\"action\":\"jumpto\",\"pos\":\""+pos+"\"}");
			mActor.getJunction().sendMessageToRole("jukebox",msg);
		} catch (Exception e) {
			Log.e("jinzora","Error sending jukebox command",e);
		}
	}
	
	@Override
	public synchronized void playlist(String urlstr) {
		try {
			JSONArray tracklist = new JSONArray();
			
			URL url = new URL(urlstr);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			InputStream inStream = conn.getInputStream();
			conn.connect();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
			String line = null; 
			String lastLine = null;
			
			line = br.readLine();
			while (line != null) {
				if (line.charAt(0) != '#') {
					try {
						JSONObject theTrack = new JSONObject();
						
						URL track = new URL(line);
						theTrack.put("url", track.toExternalForm());
					    
					    if (lastLine.charAt(0) == '#') {
					    	int pos;
					    	if (-1 != (pos = lastLine.indexOf(','))) {
					    		theTrack.put("name",lastLine.substring(pos+1,lastLine.length()));
					    	} else {
					    		theTrack.put("name",UNKNOWN_TRACK);
					    	}
					    } else {
					    	theTrack.put("name",UNKNOWN_TRACK);
					    }
					    
					    tracklist.put(theTrack);
					} catch (Exception e) {
						// probably a comment line
					}
				}
				
				lastLine = line;
				line = br.readLine();
			}
			
			JSONObject msg = new JSONObject();
			
			msg.put("action","playlist");
			msg.put("playlist", tracklist);
			msg.put("addtype", getAddTypeString());
			
			mActor.getJunction().sendMessageToRole("jukebox", msg);
			
		} catch (Exception e) {
			Log.e("jinzora","Error playing media",e);
		}
	}
	
	private void sendCommand(String action) throws JSONException {
		JSONObject msg = new JSONObject ("{\"action\":\""+action+"\"}");
		Log.d("jinzora","actor " + mActor);
		Log.d("jinzora","junction " + ((mActor != null) ? mActor.getJunction() : " :("));
		mActor.getJunction().sendMessageToRole("jukebox",msg);
	}
	
	
	/**
	 * A method for building a playlist from a
	 * JSON request. Used by JunctionBox jukebox.
	 */
	protected synchronized void playlist(JSONObject request) {
		try {
			String addtype = request.getString("addtype");
			if ("REPLACE".equalsIgnoreCase(addtype)) {
				playlist = new ArrayList<String>();
				trackNames = new ArrayList<String>();
			}
			
			List<String> endList = new ArrayList<String>();
			List<String> endListNames = new ArrayList<String>();
			if ("CURRENT".equalsIgnoreCase(addtype)) {
				while (playlist.size() > pos+1) {
					endList.add(playlist.remove(pos+1));
					endListNames.add(trackNames.remove(pos+1));
				}
			}

			JSONArray inbound = request.getJSONArray("playlist");
			int i = 0;
			do {
				playlist.add(inbound.getJSONObject(i).getString("url"));
				trackNames.add(inbound.getJSONObject(i).getString("name"));
			} while (++i < inbound.length());
			
			playlist.addAll(endList);
			trackNames.addAll(endListNames);
			
			if ("REPLACE".equalsIgnoreCase(addtype) || mp == null) {
				superJumpto(0);
			} else if (!mp.isPlaying()) {
				superJumpto(pos);
			}
			
		} catch (Exception e) {
			Log.e("jinzora","Error playing media",e);
		}
	}
	
	
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		Log.d("jinzora","onDestroy called; killing actor");
		mActor.leave();
		mActor = null;
	}
	
	@Override
	protected void notifyPlaying() {
		super.notifyPlaying();
		
		JSONObject msg = new JSONObject();
		try {
			msg.put("status","playing");
			//msg.put("entry", trackNames.get(pos));
			msg.put("pos",pos);
		} catch (JSONException e) {
			Log.e("jinzora","Error sending notification",e);
		}
		mActor.getJunction().sendMessageToRole("playlist", msg);
	}
	
	@Override
	protected void notifyPaused() {
		super.notifyPaused();
		
		JSONObject msg = new JSONObject();
		try {
			msg.put("status", "paused");
		} catch (JSONException e) {
			Log.e("jinzora","Error sending notification",e);
		}
		mActor.getJunction().sendMessageToRole("playlist", msg);
	}
	
	public void setLocalPlayback(boolean val) {
		playLocally=val;
		try {
			if (!val) superStop();
		} catch (Exception e) {}
	}
	
	public static void doFeaturesView(Activity activity ) {
		Intent intent = new Intent(activity,JunctionBoxView.class);
		activity.startActivity(intent);
	}
	
	public void invite(Activity activity) {
		AndroidJunctionMaker.getInstance().inviteActor(activity, mActor.getJunction(), "remote");
	}
	
	public void join(Activity activity) {
		AndroidJunctionMaker.getInstance().findActivityByScan(activity);
	}
	
	@Override
	public String playbackIPC(String input) throws RemoteException {
		try {
			JSONObject params = new JSONObject(input);
			if ("invitation".equals(params.optString("request"))) {
				return mActor.getJunction().getInvitationURI("remote").toString();
			}
			
			if (params.has("localplay")) {
				setLocalPlayback(params.getString("localplay").equals("true"));
			}
			
			} catch (Exception e) {
				Log.e("jinzora","error in IPC",e);
			}
		return null;
	}
}

class JinzoraActor extends JunctionActor {
	JunctionBox mDevice = null;
	public JinzoraActor(JunctionBox device) {
		super(new String[]{"jukebox","remote"});
		mDevice=device;
	}
	
	// TODO for the love of god, please make this just onMessageReceived()
	@Override
	public MessageHandler getMessageHandler() {
		// TODO Auto-generated method stub
		return new MessageHandler() {
			@Override
			public void onMessageReceived(MessageHeader header,
					JSONObject message) {
				
				if (message.has("action")) {
					
					// TODO: update pointer information
					if (!mDevice.playLocally) return;
					
					String action = message.optString("action");
					try {
						if ("pause".equalsIgnoreCase(action)) {
							mDevice.superPause();
						}
					
						else if ("next".equalsIgnoreCase(action)) {
							mDevice.superNext();
						}
						
						else if ("prev".equalsIgnoreCase(action)) {
							mDevice.superPrev();
						}
						
						else if ("stop".equalsIgnoreCase(action)) {
							mDevice.superStop();
						}
						
						else if ("jumpto".equalsIgnoreCase(action)) {
							mDevice.superJumpto(Integer.parseInt(message.optString("pos")));
						}
						
						else if ("clear".equalsIgnoreCase(action)) {
							mDevice.superClear();
						}
						
						else if ("playlist".equalsIgnoreCase(action)) {
							mDevice.playlist(message);
						}
						
					} catch (Exception e) { Log.e("jinzora","error sending command " + action, e); }
				}
				
			}
		};
	}
}