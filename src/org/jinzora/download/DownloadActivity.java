package org.jinzora.download;

import java.util.List;

import org.jinzora.Jinzora;
import org.jinzora.R;
import org.jinzora.playback.PlaybackService;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

public class DownloadActivity extends ListActivity {

	DownloadListAdapter mAdapter;
	BroadcastReceiver mDownloadListUpdateReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.download);
		
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
		
		try {
			List<String>pendingDownloads = 
			   Jinzora
				.sDlConnection
				.getBinding()
				.getPendingDownloads();
			
			mAdapter = new DownloadListAdapter(DownloadActivity.this,pendingDownloads);
			setListAdapter(mAdapter);
		} catch (Exception e) {
			Log.e("jinzora","failed to get download list",e);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mDownloadListUpdateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				try {
					List<String>pendingDownloads = 
					   Jinzora
						.sDlConnection
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
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(mDownloadListUpdateReceiver);
		mDownloadListUpdateReceiver = null;
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
}