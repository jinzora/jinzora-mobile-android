package mobisocial.socialkit.musubi.multiplayer.rpc;

import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.Musubi;
import android.net.Uri;
import android.util.Log;

public abstract class Server {
    public static final String TAG = "musubi-rpcserver";
    public static final boolean DBG = true;

    static final String TYPE_CMD = "rpc-cmd";
    static final String FIELD_ACTION = "rpc-a";

    private final DbFeed mFeed;
    private long mLastObjId = -1;

    public Server(DbFeed feed) {
        if (DBG) Log.d(TAG, "Creating new rpc server");
        mFeed = feed;
        mFeed.registerStateObserver(mFeedObserver);
    }

    public abstract void handleRequest(Uri instanceId, Request request);

    private FeedObserver mFeedObserver = new FeedObserver() {
        @Override
        public void onUpdate(DbObj latestObj) {
            long objId = latestObj.getLocalId();
            if (mLastObjId >= objId) {
                if (DBG) Log.d(TAG, "not redelivering message");
                return;
            }
            mLastObjId = objId;
            if (DBG) Log.d(TAG, "feed updated, checking latest obj");
            // TODO: work through concurrency issues.
            // TODO: work through online/offline modes.
            //      add a broadcast receiver, or put in a service?
            if (TYPE_CMD.equals(latestObj.getType())) {
                if (DBG) Log.d(TAG, "latest obj is a command");
                handleRequest(mFeed.getUri(), new Client.DataRequest(
                        latestObj.getJson().optString(FIELD_ACTION), latestObj.getJson()));
            }
        }
    };
}