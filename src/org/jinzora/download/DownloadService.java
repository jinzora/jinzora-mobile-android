package org.jinzora.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class DownloadService extends Service {
	public static final String UPDATE_DOWNLOAD_LIST = "org.jinzora.dllistupdated";
	
	LinkedList<String>downloadLabels;
	LinkedList<URL>downloadURLs;
	
	//TODO
	//LinkedList<String>failedLabels;
	//LinkedList<String>failedURLs;
	
	private static DownloadService sInstance;
	private boolean amDownloading = false;
	
	private static String UNKNOWN_TRACK = "Unknown Track";
	private static String ROOT_DIR = "jinzora";
	private File dlDir;
	
	private final DownloaderInterface.Stub mBinder = new DownloaderInterface.Stub() {

		@Override
		public void downloadPlaylist(final String urlstr) throws RemoteException {
			Toast.makeText(DownloadService.this, "Downloading playlist", Toast.LENGTH_SHORT).show();
			new Thread() {
				public void run() {
							
						try {
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
										URL track = new URL(line);
										String trackname;
										
									    if (lastLine.charAt(0) == '#') {
									    	int pos;
									    	if (-1 != (pos = lastLine.indexOf(','))) {
									    		trackname = lastLine.substring(pos+1,lastLine.length());
									    	} else {
									    		trackname = UNKNOWN_TRACK;
									    	}
									    } else {
									    	trackname = UNKNOWN_TRACK;
									    }
									    Log.d("jinzora","adding a track to dl list");
									    synchronized(mBinder) {
										    downloadLabels.add(trackname);
										    downloadURLs.add(track);
										    
										    //mBinder.notifyAll();
									    }
									    
									    if (!amDownloading) {
										    new Thread() {
										    	public void run() {
										    		doDownload();		
										    	};
										    }.start();
									    }
									} catch (Exception e) {
										// probably a comment line
									}
								}
								
								lastLine = line;
								line = br.readLine();
							}
						} catch (Exception e) {
							Log.e("jinzora","Error downloading media",e);
						}
				}
			}.start();
		}

		@Override
		public List<String> getPendingDownloads() throws RemoteException {
			return downloadLabels;
		}
		
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		if (sInstance != null) return;
		
		sInstance = this;
		
		Log.d("jinzora","creating download service");
		downloadLabels = new LinkedList<String>();
		downloadURLs = new LinkedList<URL>();
		
		dlDir = new File(Environment.getExternalStorageDirectory(), ROOT_DIR);
		if (!dlDir.exists()) {
			dlDir.mkdir();
		}
	}
	
	private void doDownload() {
		if (amDownloading) {
			return;
		}
		
		amDownloading = true;
		if (!dlDir.canWrite()){
			Log.e("jinzora","could not download to " + ROOT_DIR);
			amDownloading=false;
			return;
		}
		
		while (downloadURLs.size() > 0) {
			URL dlURL = downloadURLs.get(0);
			
			String dlName = downloadLabels.get(0)+".mp3"; // TODO: fix extension; better file name choice.
			//String dlMimeType = "audio/mpeg"; // TODO: read from headers
			
			File tempFile = null;
			
		    try {
		    	tempFile = File.createTempFile(dlName, null);
		    	tempFile.deleteOnExit();
			} catch (IOException e) {
				Log.e("jinzora","could not create temporary file",e);
				
				synchronized(mBinder) {
					downloadURLs.removeFirst();
					downloadLabels.removeFirst();
					
					mBinder.notifyAll();
				}
			}
		    
			try {
				FileOutputStream out = new FileOutputStream(tempFile);
		        
		        InputStream in = dlURL.openStream();
		        byte[] buf = new byte[4 * 1024];
		        int bytesRead;
		        while ((bytesRead = in.read(buf)) != -1) {
		          out.write(buf, 0, bytesRead);
		        }
		        in.close();
		        out.close();
		       
				// TODO: Send proper playlist with album/artist/track/num info to better name file.
		        File dlFile = null;
		        if (dlName.contains(" - ")) {
					int p = dlName.indexOf(" - ");
					String artist = dlName.substring(0,p);
					String track = dlName.substring(p+3);
					dlFile = new File(dlDir, artist);
					if (!dlFile.exists()) {
						boolean res = dlFile.mkdir();
						if (!res) {
							throw new Exception("Could not create directory " + dlFile.getAbsolutePath());
						}
					}
					dlFile = new File(dlFile,track);
				} else {
					dlFile = new File(dlDir, dlName);
				}
		        
		        tempFile.renameTo(dlFile);
		        MediaScannerNotifier.scan(DownloadService.this, dlFile.getAbsolutePath(), null);
		        
		        try {
		        	// Otherwise, every other download fails (BJD 1/4/10)
		        	Thread.sleep(1500);
		        } catch (Exception e) {}
			} catch (Exception e) {
				Log.e("jinzora","failed to download file " + dlURL.toExternalForm(),e);
			}
		
			synchronized(mBinder) {
				downloadURLs.removeFirst();
				downloadLabels.removeFirst();
				
				mBinder.notifyAll();
			}
			
			Intent notify = new Intent(UPDATE_DOWNLOAD_LIST);
			sendBroadcast(notify);
			
		}

		amDownloading = false;
	}
}