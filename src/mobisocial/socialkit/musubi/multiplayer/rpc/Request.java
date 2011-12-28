package mobisocial.socialkit.musubi.multiplayer.rpc;

import org.json.JSONObject;

public interface Request {
        /**
         * A string used by the server to determine an action to take.
         */
        public String getAction();

        /**
         * Data associated with this request.
         */
        public JSONObject getData();
    }