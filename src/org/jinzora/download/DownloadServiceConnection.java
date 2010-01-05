package org.jinzora.download;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class DownloadServiceConnection implements ServiceConnection {

	private DownloaderInterface mDownloadServiceBinding;
	
	@Override
	public void onServiceConnected(ComponentName componentName, IBinder service) {
		mDownloadServiceBinding = DownloaderInterface.Stub.asInterface(service);
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		mDownloadServiceBinding = null;
	}

	public DownloaderInterface getBinding() {
		return mDownloadServiceBinding;
	}
}
