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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
   private boolean cacheToDisk = true;

   private static DrawableManager lastManager;
   private static String lastKey = null;

   private final File THUMB_DIR;

   public DrawableManager(Context context) {
       drawableMap = new HashMap<String, Drawable>();
       pendingViews = new HashMap<ImageView, String>();
       THUMB_DIR = getThumbDirectory(context);
   }

   public static DrawableManager forKey(Context context, String key) {
       /*
        * TODO: Currently, we only store the last-used DrawableManager.
        * This is primarily useful for screen orientation changes,
        * where the key (representing the overall view) remains the same.
        * More robust caching requires deeper thought about memory management.
        */
       if (lastKey != null && lastKey.equals(key)) {
           return lastManager;
       }
       lastKey = key;
       lastManager = new DrawableManager(context);
       return lastManager;
   }

   private File getThumbDirectory(Context context) {
       File base;
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
           base = context.getExternalCacheDir();
       } else {
           base = new File(Environment.getExternalStorageDirectory(),
                   context.getPackageName() + "/cache");
       }
       return new File(base, "thumbs");
   }

   public void setCacheToDisk(boolean cacheToDisk) {
       this.cacheToDisk = cacheToDisk;
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
       if (urlString == null) {
           if (DBG) Log.d(TAG, "Null image url.");
           synchronized (pendingViews) {
               pendingViews.remove(imageView);
           }
           return;
       }

       synchronized (pendingViews) {
           pendingViews.put(imageView, urlString);
       }

       if (drawableMap.containsKey(urlString)) {
           if (DBG) Log.d(TAG, "Image cached in memory.");
           imageView.setImageDrawable(drawableMap.get(urlString));
           return;
       }

       File localFile = getLocalFile(urlString);
       if (localFile != null && localFile.exists()) {
           if (DBG) Log.d(TAG, "Image from disk.");
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
       String state = Environment.getExternalStorageState();
       if (!externalStorageAvailable()) {
           if (DBG) Log.d(TAG, "External storage not available; fetching from network");
           DefaultHttpClient httpClient = new DefaultHttpClient();
           HttpGet request = new HttpGet(urlString);
           HttpResponse response = httpClient.execute(request);
           return response.getEntity().getContent();
       } 

       File thumbFile = getLocalFile(urlString);
       if (thumbFile.exists()) {
           return new FileInputStream(thumbFile);
       }

       if (!cacheToDisk || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
           if (DBG) Log.d(TAG, "Fetching from network and not writing to disk");
           DefaultHttpClient httpClient = new DefaultHttpClient();
           HttpGet request = new HttpGet(urlString);
           HttpResponse response = httpClient.execute(request);
           return response.getEntity().getContent();
       } else {
           if (DBG) Log.d(TAG, "Fetching from network and storing on disk");
           File thumbDir = thumbFile.getParentFile();
           if (!thumbDir.exists()) {
               thumbDir.mkdirs();
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
           return new FileInputStream(thumbFile);
       }
   }

    private File getLocalFile(String urlString) {
        if (!externalStorageAvailable()) {
            return null;
        }
        try {
            return new File(THUMB_DIR, HashUtils.SHA1(urlString));
        } catch (Exception e) {
            Log.e(TAG, "Error computing string hash.", e);
            return null;
        }
    }

    private boolean externalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    private static class HashUtils {
        private static String convertToHex(byte[] data) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < data.length; i++) {
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    if ((0 <= halfbyte) && (halfbyte <= 9))
                        buf.append((char) ('0' + halfbyte));
                    else
                        buf.append((char) ('a' + (halfbyte - 10)));
                    halfbyte = data[i] & 0x0F;
                } while (two_halfs++ < 1);
            }
            return buf.toString();
        }

        public static String SHA1(String text) throws NoSuchAlgorithmException,
                UnsupportedEncodingException {
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            byte[] sha1hash = new byte[40];
            md.update(text.getBytes("iso-8859-1"), 0, text.length());
            sha1hash = md.digest();
            return convertToHex(sha1hash);
        }
    }
}