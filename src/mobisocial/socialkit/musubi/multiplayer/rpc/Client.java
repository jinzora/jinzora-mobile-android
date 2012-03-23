package mobisocial.socialkit.musubi.multiplayer.rpc;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Client {
    public static final String TAG = "musubi-rpc-client";
    private static final boolean DBG = true;

    private final DbFeed mFeed;

    public Client(DbFeed feed) {
        mFeed = feed;
    }

    /**
     * Sends a request to a listening server.
     */
    public final void sendRequest(Request request) {
        if (DBG) Log.d(TAG, "sending request " + request.getAction());
        JSONObject req = request.getData();
        try {
            req.put(Server.FIELD_ACTION, request.getAction());
        } catch (JSONException e) {}
        Obj obj = new MemObj(Server.TYPE_CMD, req);
        mFeed.postObj(obj);
    }

    public static class BasicRequest implements Request {
        private final String mAction;

        public BasicRequest(String action) {
            mAction = action;
        }

        @Override
        public String getAction() {
            return mAction;
        }

        @Override
        public JSONObject getData() {
            return null;
        }
        
    }
    
    public static class DataRequest implements Request {
        private final String mAction;
        private final JSONObject mData;

        public DataRequest(String action, JSONObject data) {
            mAction = action;
            mData = data;
        }

        @Override
        public String getAction() {
            return mAction;
        }

        @Override
        public JSONObject getData() {
            return mData;
        }
        
    }
}