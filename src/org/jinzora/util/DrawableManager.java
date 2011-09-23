package org.jinzora.util;

/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.    
*/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

public class DrawableManager {
   private static final String TAG = "drawableManager";
   private static final boolean DBG = false;
   private final Map<String, Drawable> drawableMap;
   private final Map<ImageView, String> pendingViews;

   private static DrawableManager lastManager;
   private static String lastKey = null;

   private static final String THUMB_DIR = "jinzora/.thumb";

   public DrawableManager() {
       drawableMap = new HashMap<String, Drawable>();
       pendingViews = new HashMap<ImageView, String>();
   }

   public static DrawableManager forKey(String key) {
       // TODO: Currently, we only store the last-used DrawableManager.
       // This is primarily useful for screen orientation changes,
       // where the 
       if (lastKey != null && lastKey.equals(key)) {
           return lastManager;
       }
       lastKey = key;
       lastManager = new DrawableManager();
       return lastManager;
   }

   public Drawable fetchDrawable(String urlString) {
       if (drawableMap.containsKey(urlString)) {
           if (DBG) Log.d(TAG, "Using cached image");
           return drawableMap.get(urlString);
       }

       if (DBG) Log.d(TAG, "Fetching image");
       try {
           InputStream is = fetch(urlString);
           Drawable drawable = Drawable.createFromStream(is, "src");

           if (drawable != null) {
               drawableMap.put(urlString, drawable);
           } else {
             Log.w(this.getClass().getSimpleName(), "could not get thumbnail for " + urlString);
           }

           return drawable;
       } catch (MalformedURLException e) {
           Log.e(this.getClass().getSimpleName(), "fetchDrawable failed", e);
           return null;
       } catch (IOException e) {
           Log.e(this.getClass().getSimpleName(), "fetchDrawable failed", e);
           return null;
       }
   }

   public void fetchDrawableOnThread(final String urlString, final ImageView imageView) {
       if (drawableMap.containsKey(urlString)) {
           imageView.setImageDrawable(drawableMap.get(urlString));
           return;
       }
       File localFile = getLocalFile(urlString);
       if (localFile.exists()) {
           imageView.setImageDrawable(fetchDrawable(urlString));
           return;
       }

       final Handler handler = new Handler() {
           @Override
           public void handleMessage(Message message) {
               synchronized (pendingViews) {
                   String latest = pendingViews.get(imageView);
                   if (urlString.equals(latest)) {
                       imageView.setImageDrawable((Drawable) message.obj);
                   }
               }
           }
       };

       synchronized (pendingViews) {
           pendingViews.put(imageView, urlString);
       }

       new Thread() {
           @Override
           public void run() {
               Drawable drawable = fetchDrawable(urlString);
               Message message = handler.obtainMessage(1, drawable);
               handler.sendMessage(message);
           }
       }.start();
   }

   private InputStream fetch(String urlString) throws MalformedURLException, IOException {
       File thumbFile = getLocalFile(urlString);
       if (!thumbFile.exists()) {
           File thumbDir = thumbFile.getParentFile();
           if (!thumbDir.exists()) {
               thumbDir.mkdir();
           }
           DefaultHttpClient httpClient = new DefaultHttpClient();
           HttpGet request = new HttpGet(urlString);
           HttpResponse response = httpClient.execute(request);
           InputStream is = response.getEntity().getContent();
           OutputStream out = new FileOutputStream(thumbFile);
           byte[] buf = new byte[1024];
           int len;
           while ((len = is.read(buf)) > 0) {
               out.write(buf, 0, len);
           }
           out.close();
       }

       return new FileInputStream(thumbFile);
   }

    private File getLocalFile(String urlString) {
        try {
            File thumbDir = new File(Environment.getExternalStorageDirectory(), THUMB_DIR);
            return new File(thumbDir, HashUtils.SHA1(urlString));
        } catch (Exception e) {
            Log.e(TAG, "Error computing string hash.", e);
            return null;
        }
    }
}