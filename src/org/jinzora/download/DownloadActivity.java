package org.jinzora.download;

import java.util.List;

import org.jinzora.android.R;
import org.jinzora.download.DownloaderInterface;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class DownloadActivity extends ListActivity {

	DownloadListAdapter mAdapter;
	BroadcastReceiver mDownloadListUpdateReceiver;
	
	final static int MENU_CANCEL_ALL = 1;
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Intent bindIntent = new Intent(this,DownloadService.class);
		bindService(bindIntent,mDlConnection,Service.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mDlConnection);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.download);
		mDlConnection = new DownloadServiceConnection();
		
		((Button)findViewById(R.id.launch_music_app)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Intent mediaPlayer 
							= new Intent(Intent.ACTION_MAIN)
								.addCategory(Intent.CATEGORY_LAUNCHER)
								.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
								.setClassName("com.android.music", "com.android.music.MusicBrowserActivity");
				
				startActivity(mediaPlayer);
			}
		});
		
		((ListView)findViewById(android.R.id.list))
		.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			String[] entryOptions = new String[] {"Cancel download"};
			String selectedEntry;
			
			@Override
			public boolean onItemLongClick(AdapterView<?> parent,
					View view, final int listPosition, long id) {
				
				// freeze it since adapter may change
				selectedEntry = mAdapter.getItem(listPosition);
				new AlertDialog.Builder(DownloadActivity.this)
				.setTitle(selectedEntry)
				.setItems(entryOptions, 
						new AlertDialog.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface arg0, int entryPos) {
								
								switch (entryPos) {
								case 0:
									// Cancel download
									try {
										mDlConnection
										.getBinding()
										.cancelDownload(listPosition, selectedEntry);
									} catch (Exception e) {
										Log.e("jinzora","Failed to cancel download",e);
									}
									break;
								}
								
							}
						}
				).create().show();
				
				return true;
			}
		});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		mDownloadListUpdateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				try {
					List<String>pendingDownloads = 
					   mDlConnection
						.getBinding()
						.getPendingDownloads();
					
					mAdapter = new DownloadListAdapter(DownloadActivity.this,pendingDownloads);
					setListAdapter(mAdapter);
				} catch (Exception e) {
					Log.e("jinzora","failed to get download list",e);
				}
			}
		};	
		
		IntentFilter intentFilter = new IntentFilter(DownloadService.UPDATE_DOWNLOAD_LIST);
		registerReceiver(mDownloadListUpdateReceiver, intentFilter);
		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		unregisterReceiver(mDownloadListUpdateReceiver);
		mDownloadListUpdateReceiver = null;
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0,MENU_CANCEL_ALL,1,R.string.cancel_all_downloads)
    	.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
    	.setAlphabeticShortcut('x');
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();
		switch (id) {
		case MENU_CANCEL_ALL:
			try {
			  mDlConnection
				.getBinding()
				.cancelAllDownloads();
			} catch (Exception e) {
				Log.e("jinzora","failed to cancel all downloads",e);
			}
			break;
		}
		return true;
	}
	
	private DownloadServiceConnection mDlConnection;
	private class DownloadServiceConnection implements ServiceConnection {
		private DownloaderInterface mDownloadServiceBinding;
		
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mDownloadServiceBinding = DownloaderInterface.Stub.asInterface(service);
			
			try {
				List<String>pendingDownloads = mDownloadServiceBinding.getPendingDownloads();
				mAdapter = new DownloadListAdapter(DownloadActivity.this,pendingDownloads);
				setListAdapter(mAdapter);
			} catch (Exception e) {
				Log.e("jinzora","failed to get download list",e);
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mDownloadServiceBinding = null;
		}

		public DownloaderInterface getBinding() {
			return mDownloadServiceBinding;
		}
	}
}

class DownloadListAdapter extends ArrayAdapter<String> {
	LayoutInflater sInflator;
	
	public DownloadListAdapter(Context c, List<String>list) {
		super(c,R.layout.download_entry,list);
		if (sInflator == null) {
			sInflator = LayoutInflater.from(c);
		}
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View row;
		if (convertView == null) {
			row = sInflator.inflate(R.layout.download_entry,null);
		} else {
			row = convertView;
		}
		((TextView)row.findViewById(R.id.dl_title)).setText(getItem(position));
		
		return row;
	}
	
	public String getEntryTitle(int pos) {
		return getItem(pos);
	}
}