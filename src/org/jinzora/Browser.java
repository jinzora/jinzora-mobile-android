package org.jinzora;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jinzora.playback.PlaybackService;
import org.jinzora.playback.PlaybackServiceConnection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class Browser extends ListActivity {
	private ArrayList<Map<String,String>> allEntries = new ArrayList<Map<String,String>>();
	private ArrayList<Map<String,String>> visibleEntries = allEntries;
	
	protected static String browsing;
	
	protected JzMediaAdapter mMediaAdapter;
	
	@Override
	protected void onStart() {
		super.onStart();
		if (null != getIntent().getStringExtra(getPackageName()+".browse")) {
			// todo: get rid of this static.
			browsing = getIntent().getStringExtra(getPackageName()+".browse");
		} else {
	   		browsing = getHomeURL();
	   		if (null  == browsing) {
	   			startActivity(new Intent(this, Preferences.class));
	   			return;
	   		}
       }
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if (browsing == null) {
			Log.w("jinzora","should not see null browse link");
			browsing = getHomeURL();
		}
		
		if (null != browsing) {
			doBrowsing();
		} else {
			Log.w("jinzora","could not set browsing url");
		}
	}
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    
    private String getHomeURL() {
    	String baseurl = Jinzora.getBaseURL();
   		if (baseurl == null) {
   			return null;
   		} else {
   			return baseurl + "&request=home";
   		}
    }

    private void doBrowsing() {
    	try {
    		allEntries.clear();
    		XmlPullParser xpp;
    		InputStream inStream;
    		try {
    			
    			URL url = new URL(browsing);
    			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    			conn.setConnectTimeout(30000);
    			inStream = conn.getInputStream();
    			conn.connect();

    			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
    			factory.setNamespaceAware(true);
    			xpp = factory.newPullParser();
    			xpp.setInput(inStream, conn.getContentEncoding());
    		} catch (Exception e) {

        		JzMediaAdapter adapter = new JzMediaAdapter(this, new ArrayList<HashMap<String,String>>());
        		setContentView(R.layout.browse);
        		setListAdapter(adapter);
    			
    			return;
    		}


    		int eventType = xpp.getEventType();
    		while (eventType != XmlPullParser.END_DOCUMENT) {
    			if(eventType == XmlPullParser.START_DOCUMENT) {

    			} else if(eventType == XmlPullParser.END_DOCUMENT) {

    			} else if(eventType == XmlPullParser.START_TAG && 
    					(xpp.getName().equals("nodes") || xpp.getName().equals("browse") || xpp.getName().equals("tracks"))) {

    				int depth = xpp.getDepth();
    				xpp.next();

    				Map<String,String> item = null;
    				while (!(depth == xpp.getDepth() && eventType == XmlPullParser.END_TAG)) {	            	  
    					if (depth+1 == xpp.getDepth() && eventType == XmlPullParser.START_TAG) {
    						item = new HashMap<String,String>();
    					} else if (depth+1 == xpp.getDepth() && eventType == XmlPullParser.END_TAG) {
    						/*if (item.containsKey("album")) {
	            			  item.put("subfield1", item.get("album"));
	            			  if (item.containsKey("artist")) {
	            				  item.put("subfield2",item.get("artist"));
	            			  }
	            		  } else*/ if (item.containsKey("artist")){
	            			  item.put("subfield1", item.get("artist"));
	            		  }

	            		  allEntries.add(item);
    					}

    					if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("name")) {
    						eventType = xpp.next();
    						item.put("name", xpp.getText());	           			  
    					}

    					if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("artist")) {
    						eventType = xpp.next();
    						item.put("artist", xpp.getText());
    					}

    					if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("album")) {
    						eventType = xpp.next();
    						item.put("album", xpp.getText());
    					}

    					if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("playlink")) {
    						eventType = xpp.next();
    						item.put("playlink", xpp.getText());
    					}

    					if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("browse")) {
    						eventType = xpp.next();
    						item.put("browse",xpp.getText());
    					}

    					eventType = xpp.next();
    				}
    			} else if(eventType == XmlPullParser.END_TAG) {

    			} else if(eventType == XmlPullParser.TEXT) {

    			}
    			eventType = xpp.next();
    		}

    		inStream.close();
    		mMediaAdapter = new JzMediaAdapter(this, allEntries);

    		setContentView(R.layout.browse);
    		setListAdapter(mMediaAdapter);

    		final CharSequence[] entryOptions = {"Share", "Replace current playlist", "Queue to end of list", "Queue next" };
    		((ListView)findViewById(android.R.id.list))
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, final int listPosition, long id) {

						if (!mMediaAdapter.isPlayable(listPosition)) return false;
						
						new AlertDialog.Builder(Browser.this)
							.setTitle(mMediaAdapter.getEntryTitle(listPosition))
							.setItems(entryOptions, 
									new AlertDialog.OnClickListener() {
										@Override
										public void onClick(final DialogInterface dialog, final int entryPos) {
											final Map<String,String>item = mMediaAdapter.getEntry(listPosition);
											switch (entryPos) {
											case 0:
												Intent share = new Intent("android.intent.action.SEND");
												share.setType("audio/x-mpegurl")
													.putExtra(Intent.EXTRA_TEXT, item.get("playlink"));
												Browser.this
													.startActivity(Intent.createChooser(share, "Share playlist..."));
												break;
											case 1:
											case 2:
											case 3:
												new Thread() {
													@Override
													public void run() {
														try {
															Jinzora.sPbConnection
															.playbackBinding
															.playlist(item.get("playlink"), entryPos-1);
														} catch (Exception e) {
															Log.e("jinzora","Error in longpress event",e);
														} finally {
															dialog.dismiss();
														}
													}
												}.start();
												break;
											}
										}
									}
							)
							.create().show();
						
						return true;
					}
					
				});

    	} catch (Exception e) {
    		Log.e("jinzora", "error", e);
    	}
    }

    
    @Override
    public void onListItemClick(ListView l, View v, final int position, long id) {
    	try {
    		String mBrowse = null;
    		if (null == (mBrowse = visibleEntries.get(position).get("browse"))) {
    			if (visibleEntries.get(position).containsKey("playlink")) {
					new Thread() {
						public void run() {
							try {
								Jinzora.sPbConnection.playbackBinding
									.playlist(visibleEntries.get(position).get("playlink"),
											  Jinzora.getAddType());
							} catch (Exception e) {
								Log.e("jinzora","Error playing media",e);
							}
						}
					}.start();
    			}
    			
    			return;
    		}

    		Intent intent = new Intent(this,Jinzora.class);
    		intent.putExtra(getPackageName()+".browse", mBrowse);
    		startActivity(intent);
    		
    	} catch (Exception e) {
    		Log.e("jinzora","Error during listItemClick",e);
    	}
    
    }
    
    private String query = "";
    @Override
    public synchronized boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
    	ArrayList<Map<String,String>> workingEntries;
    	char c;
    	if ('\0' != (c = event.getMatch("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 ".toCharArray()))) {
    		query = query + c;
    		workingEntries = visibleEntries;
    	} else if (keyCode == KeyEvent.KEYCODE_DEL) {
    		if (query.length() > 0) {
    			query = query.substring(0, query.length()-1);
    			if (query.length() == 0) {
        			visibleEntries = allEntries;
        			JzMediaAdapter adapter = new JzMediaAdapter(this, visibleEntries);
        	        setListAdapter(adapter);
        	    	return super.onKeyUp(keyCode,event);
        		}
    			workingEntries = allEntries;
    		} else {
    			return super.onKeyUp(keyCode,event);
    		}
    	} else {
    		return super.onKeyUp(keyCode,event);
    	}
    	
    	//TODO: support caching in the case of deletions?
    	// (as long as it doesn't use too much memory)
    	ArrayList<Map<String,String>> newList = new ArrayList<Map<String,String>>();
    	for (int i = 0; i < workingEntries.size(); i++) {
    		if (workingEntries.get(i).get("name").toUpperCase().contains(query)) {
    			newList.add(workingEntries.get(i));
    		}
    	}
    	visibleEntries= newList;
    	
    	JzMediaAdapter adapter = new JzMediaAdapter(this, visibleEntries);
        setListAdapter(adapter);
    	return super.onKeyUp(keyCode,event);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	return Jinzora.createMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	Jinzora.menuItemSelected(featureId,item,this);
    	return super.onMenuItemSelected(featureId, item);
    }
    
    public static void clearBrowsing() {
    	browsing=null;
    }
    

    class JzMediaAdapter extends SimpleAdapter {
    	Browser context;
    	public JzMediaAdapter(Browser context, List<? extends Map<String, ?>> data) {
    		super(context, data, R.layout.media_element, null, null);
    		this.context=context;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View row;
    		if (convertView == null) {
    			LayoutInflater inflater=LayoutInflater.from(context);
    			row=inflater.inflate(R.layout.media_element, null);
    		} else {
    			row = convertView;
    		}

    		final Map<String,String>item = (Map<String,String>)this.getItem(position);
    		TextView label = (TextView)row.findViewById(R.id.media_el_name);
    		label.setText(item.get("name"));

    		if (item.containsKey("subfield1")) {
    			label = (TextView)row.findViewById(R.id.media_el_subfield1);
    			label.setText(item.get("subfield1"));
    		}
    		if (item.containsKey("subfield2")) {
    			label = (TextView)row.findViewById(R.id.media_el_subfield2);
    			label.setText(item.get("subfield2"));
    		}

    		if (!item.containsKey("playlink")) {
    			row.findViewById(R.id.media_el_play).setVisibility(View.INVISIBLE);
    		} else {
    			Button button = (Button)row.findViewById(R.id.media_el_play);
    			button.setOnClickListener(new View.OnClickListener() {

    				@Override
    				public void onClick(View v) {

    					new Thread() {
    						public void run() {
    							try {
    								Jinzora.sPbConnection.playbackBinding.playlist(item.get("playlink"), Jinzora.getAddType());
    							} catch (Exception e) {
    								Log.e("jinzora","Error playing media",e);
    							}
    						}
    					}.start();
    				}

    			});
    		}
    		return row;  
    	}

    	public Map<String,String>getEntry(int pos) {
    		if (this.getCount() <= pos ) return null;
    		return (Map<String,String>)visibleEntries.get(pos);
    	}
    	
    	public String getEntryTitle(int pos) {
    		if (this.getCount() <= pos ) return null;
    		Map<String,String>item = (Map<String,String>)visibleEntries.get(pos);
    		return item.get("name");
    	}
    	
    	public boolean isPlayable(int pos) {
    		if (this.getCount() <= pos ) return false;
    		Map<String,String>item = (Map<String,String>)visibleEntries.get(pos);
    		return item.containsKey("playlink");
    	}
    }
}