package org.jinzora.playback.players.views;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.jinzora.Jinzora;
import org.jinzora.R;
import org.json.JSONObject;

import edu.stanford.prpl.junction.impl.AndroidJunctionMaker;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class JunctionBoxView extends Activity {
	
	private boolean killme=false;
	
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (killme) finish();
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.junctionbox);
		
		
		((Button)findViewById(R.id.jx_invite))
			.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					String params = "{request:\"invitation\"}";
					try {
						String vite = Jinzora.sPbConnection.playbackBinding.playbackIPC(params);
						URI invitation = new URI(vite);
						killme=true;
						AndroidJunctionMaker.getInstance().inviteActor(JunctionBoxView.this, invitation);
					} catch (Exception e) {
						Log.e("jinzora","error sending service request",e);
					}
				}
			});
		
		((Button)findViewById(R.id.jx_join))
		.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				killme=true;
				AndroidJunctionMaker.getInstance().findActivityByScan(JunctionBoxView.this);
			}
		});
		
		((CheckBox)findViewById(R.id.jx_localplay))
			.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean val) {
					JSONObject params = new JSONObject();
					try {
						params.put("localplay",val ? "true" : "false");
						Jinzora.sPbConnection.playbackBinding.playbackIPC(params.toString());
					} catch (Exception e) {
						Log.e("jinzora","error sending IPC request", e);
					}
				}
				
			});
	}
}
