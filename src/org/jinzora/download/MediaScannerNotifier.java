package org.jinzora.download;
import android.content.Context;
import android.media.MediaScannerConnection; 
import 
android.media.MediaScannerConnection.MediaScannerConnectionClient; 
import android.net.Uri;

class MediaScannerNotifier implements MediaScannerConnectionClient { 
    private Context mContext; 
    private MediaScannerConnection mConnection; 
    private String mPath; 
    private String mMimeType; 
    
    
    public static void scan(Context context, String path, String mimeType) {
    	new MediaScannerNotifier(context,path,mimeType);
    }
    
    private MediaScannerNotifier(Context context, String path, String mimeType) { 
        mContext = context; 
        mPath = path; 
        mMimeType = mimeType; 
        mConnection = new MediaScannerConnection(context, this); 
        mConnection.connect(); 
    } 
    public void onMediaScannerConnected() { 
        mConnection.scanFile(mPath, mMimeType); 
    } 
    public void onScanCompleted(String path, Uri uri) { 
        // OPTIONAL: scan is complete, this will cause the viewer to render it
    	/*
        try { 
            if (uri != null) { 
                Intent intent = new Intent(Intent.ACTION_VIEW); 
                intent.setData(uri); 
                mContext.startActivity(intent); 
            } 
        } finally { 
            mConnection.disconnect(); 
            mContext = null; 
        } 
        */
    } 
} 