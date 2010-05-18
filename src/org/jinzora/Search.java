package org.jinzora;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.jinzora.playback.PlaybackService;
import org.jinzora.playback.PlaybackServiceConnection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;



public class Search extends Activity {
	private PlaybackServiceConnection sPbConnection = new PlaybackServiceConnection();
	
	@Override
	protected void onStart() {
		super.onStart();
		
		bindService(new Intent(this,PlaybackService.class), sPbConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		unbindService(sPbConnection);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Jinzora.initContext(this);
        try {
        	setContentView(R.layout.search);
        	
        	
        	
        	this.findViewById(R.id.search_box).setOnKeyListener(new View.OnKeyListener() {
	        	 @Override
	        	public boolean onKey(View v, int keyCode, KeyEvent event) {
	        		if (event.getAction() == KeyEvent.ACTION_UP &&
	        			event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
	        		
	        			
	        			new Thread() {
		 					@Override
		 					public void run() {
		 						try {
		 							String query = ((EditText)findViewById(R.id.search_box)).getText().toString();
		 							if (query != null && query.length() > 0) {
		 								Intent intent = new Intent(Search.this, Jinzora.class);
		 								intent.putExtra(Search.this.getPackageName()+".browse",Jinzora.getBaseURL()+"&request=search&query="+URLEncoder.encode(query, "UTF-8"));
		 					    		startActivity(intent);
		 							}
		 						} catch (Exception e) {
		 							Log.e("jinzora","Error during search",e);
		 						}
		 					}
		 				}.start();
	        			
	        			
	        		}
	        		
	        		return false;
	        	}
	         });
	         
	         this.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {

	 			@Override
	 			public void onClick(View v) {
	 			
	 				new Thread() {
	 					@Override
	 					public void run() {
	 						try {
	 							String query = ((EditText)findViewById(R.id.search_box)).getText().toString();
	 							if (query != null && query.length() > 0) {
	 								Intent intent = new Intent(Search.this, Jinzora.class);
	 								intent.putExtra(Search.this.getPackageName()+".browse",Jinzora.getBaseURL()+"&request=search&query="+URLEncoder.encode(query, "UTF-8"));
	 					    		startActivity(intent);
	 							}
	 						} catch (Exception e) {
	 							Log.e("jinzora","Error during search",e);
	 						}
	 					}
	 				}.start();
	 			}
	 		});
	         
	        this.findViewById(R.id.quicksearch_button).setOnClickListener(new View.OnClickListener() {

		 			@Override
		 			public void onClick(View v) {
		 			
		 				new Thread() {
		 					@Override
		 					public void run() {
		 						try {
		 							String query = ((EditText)findViewById(R.id.search_box)).getText().toString();
		 							if (query != null && query.length() > 0) {
		 								quickplay(query);
		 							}
		 						} catch (Exception e) {
		 							Log.e("jinzora","Error during search",e);
		 						}
		 					}
		 				}.start();
		 			}
		 		});
        	
        } catch (Exception e) {
        	
        }
	}
	
    /**
     * Performs a search and sends the
     * result directly to the player.
     */
	protected void quickplay(String query) {
		try {
			String urlStr = Jinzora.getBaseURL()+"&request=search&query="+URLEncoder.encode(query);
	    	URL url = new URL(urlStr);
	        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
	        //conn.setConnectTimeout(5000);
			InputStream inStream = conn.getInputStream();
			conn.connect();
	
			 XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
	         factory.setNamespaceAware(true);
	         XmlPullParser xpp = factory.newPullParser();
	         xpp.setInput(inStream, conn.getContentEncoding());
	
	         int eventType = xpp.getEventType();
	         while (eventType != XmlPullParser.END_DOCUMENT) {
	        	 if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("playlink")) {
	        		 eventType = xpp.next();
	        		 
	        		 // found a match; play it.
	        		 Jinzora.doPlaylist( xpp.getText(), Jinzora.getAddType() );
	        		 return;
	        	 }
	        	 eventType = xpp.next();
	         }
	    	
	    	
		} catch (Exception e) {
			Log.e("jinzora","Error during quicksearch",e);
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	return Jinzora.createMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	Jinzora.menuItemSelected(featureId,item, this);
    	return super.onMenuItemSelected(featureId, item);
    }
}