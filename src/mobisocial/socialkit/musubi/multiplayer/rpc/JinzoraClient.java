
package mobisocial.socialkit.musubi.multiplayer.rpc;

import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.musubi.DbFeed;

public class JinzoraClient extends Client {
    static final String CMD_PLAYLIST = "jz.pl";
    static final String CMD_PLAYBACK = "jz.pb";

    static final String FIELD_ACTION = "act";
    static final String FIELD_ADDTYPE= " add";
    static final String FIELD_PLAYLIST= "pl";
    static final String FIELD_POS= "pos";

    public enum AddType { BEGINNING, CURRENT, END, REPLACE };
    public enum PlaybackAction { PREV, NEXT, PAUSE, PLAY, STOP, JUMPTO, JUMPNEXT };

    public JinzoraClient(DbFeed feed) {
        super(feed);
    }

    public void sendPlaylistRequest(int addType, String playlist) {
        JSONObject req = new JSONObject();
        try {
            req.put(FIELD_ADDTYPE, addType);
            req.put(FIELD_PLAYLIST, playlist);
        } catch (JSONException e) {}
        Request request = new DataRequest(CMD_PLAYLIST, req);
        sendRequest(request);
    }

    public void sendPlaybackCommand(PlaybackAction command) {
        JSONObject req = new JSONObject();
        try {
            req.put(FIELD_ACTION, command.name());
        } catch (JSONException e) {}
        Request request = new DataRequest(CMD_PLAYBACK, req);
        sendRequest(request);
    }

    public void sendJumptoCommand(int pos) {
        JSONObject req = new JSONObject();
        try {
            req.put(FIELD_ACTION, PlaybackAction.JUMPTO.name());
            req.put(FIELD_POS, pos);
        } catch (JSONException e) {}
        Request request = new DataRequest(CMD_PLAYBACK, req);
        sendRequest(request);
    }

    public void sendJumpnextCommand(int pos) {
        JSONObject req = new JSONObject();
        try {
            req.put(FIELD_ACTION, PlaybackAction.JUMPNEXT.name());
            req.put(FIELD_POS, pos);
        } catch (JSONException e) {}
        Request request = new DataRequest(CMD_PLAYBACK, req);
        sendRequest(request);
    }
}