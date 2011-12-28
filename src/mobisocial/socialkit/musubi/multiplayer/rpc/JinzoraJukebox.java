package mobisocial.socialkit.musubi.multiplayer.rpc;

import org.jinzora.Jinzora;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.multiplayer.rpc.JinzoraClient.PlaybackAction;

public class JinzoraJukebox extends Server {
    public static final String TAG = "jinzora-musubi-server";
    private static final boolean DBG = true;

    private JinzoraJukebox(DbFeed feed) {
        super(feed);
    }

    public static JinzoraJukebox getInstance(DbFeed feed) {
        return new JinzoraJukebox(feed);
    }

    @Override
    public void handleRequest(Uri instanceId, Request request) {
        String action = request.getAction();
        JSONObject data = request.getData();
        if (JinzoraClient.CMD_PLAYLIST.equals(action)) {
            try {
                if (DBG) Log.d(TAG, "Handling request " + data);
                Jinzora.doPlaylist(data.getString(JinzoraClient.FIELD_PLAYLIST),
                        data.getInt(JinzoraClient.FIELD_ADDTYPE));
            } catch (JSONException e) {
                Log.e(TAG, "Malformatted request.", e);
            }
        } else if (JinzoraClient.CMD_PLAYBACK.equals(action)) {
            try {
                String cmdStr = data.getString(JinzoraClient.FIELD_ACTION);
                PlaybackAction cmd = PlaybackAction.valueOf(cmdStr);
                switch (cmd) {
                    case PREV:
                        Jinzora.sPbConnection.getPlaybackBinding().prev();
                        break;
                    case NEXT:
                        Jinzora.sPbConnection.getPlaybackBinding().next();
                        break;
                    case PLAY:
                        Jinzora.sPbConnection.getPlaybackBinding().play();
                        break;
                    case STOP:
                        Jinzora.sPbConnection.getPlaybackBinding().stop();
                        break;
                    case PAUSE:
                        Jinzora.sPbConnection.getPlaybackBinding().pause();
                        break;
                    case JUMPTO:
                        int pos = data.getInt(JinzoraClient.FIELD_POS);
                        Jinzora.sPbConnection.getPlaybackBinding().jumpTo(pos);
                        break;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error issuing command " + action, e);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing command", e);
            }
        }
    }
}
