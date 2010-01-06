package org.jinzora;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jinzora.download.DownloadServiceConnection;
import org.jinzora.playback.PlaybackService;
import org.jinzora.playback.PlaybackServiceConnection;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class Browser extends ListActivity {
	private JzMediaAdapter allEntriesAdapter = null;
	private JzMediaAdapter visibleEntriesAdapter = null;
	
	protected static String browsing;
	protected static LayoutInflater sInflater = null;
	private String curQuery = "";
	
	Handler mAddEntriesHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			allEntriesAdapter.add(msg.getData());
			if (allEntriesAdapter != visibleEntriesAdapter) {
				if (matchesFilter(msg.getData().getString("name"))) {
					visibleEntriesAdapter.add(msg.getData());
				}
			}
		}
	};
	
	
	@Override
	protected void onStart() {
		super.onStart();
		
		
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		
	}
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Jinzora.initContext(this);
        
        allEntriesAdapter = new JzMediaAdapter(Browser.this);
        visibleEntriesAdapter = allEntriesAdapter;
        
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
        doBrowsing();
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
    		allEntriesAdapter.clear();
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
        		JzMediaAdapter adapter = new JzMediaAdapter(this, new ArrayList<Bundle>());
        		setContentView(R.layout.browse);
        		setListAdapter(adapter);
    			return;
    		}

    		setContentView(R.layout.browse);
    		setListAdapter(allEntriesAdapter);

    		final CharSequence[] entryOptions = {"Share", "Replace current playlist", "Queue to end of list", "Queue next", "Download to device" };
    		((ListView)findViewById(android.R.id.list))
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, final int listPosition, long id) {

						if (!visibleEntriesAdapter.isPlayable(listPosition)) return false;
						
						new AlertDialog.Builder(Browser.this)
							.setTitle(visibleEntriesAdapter.getEntryTitle(listPosition))
							.setItems(entryOptions, 
									new AlertDialog.OnClickListener() {
										@Override
										public void onClick(final DialogInterface dialog, final int entryPos) {
											final Bundle item = visibleEntriesAdapter.getEntry(listPosition);
											switch (entryPos) {
											case 0:
												// Share
												Intent share = new Intent("android.intent.action.SEND");
												share.setType("audio/x-mpegurl")
													.putExtra(Intent.EXTRA_TEXT, item.getString("playlink"));
												Browser.this
													.startActivity(Intent.createChooser(share, "Share playlist..."));
												break;
											case 1:
											case 2:
											case 3:
												// Play, Queue
												new Thread() {
													@Override
													public void run() {
														try {
															Jinzora.sPbConnection
															.playbackBinding
															.playlist(item.getString("playlink"), entryPos-1);
														} catch (Exception e) {
															Log.e("jinzora","Error in longpress event",e);
														} finally {
															dialog.dismiss();
														}
													}
												}.start();
												break;
											case 4:
												// Download to device
												try {
													Jinzora
													  .sDlConnection
													  .getBinding()
													  .downloadPlaylist(item.getString("playlink"));
												} catch (Exception e) {
													Log.d("jinzora","Error downloading playlist",e);
												}
												// Add menu entry
											}
										}
									}
							)
							.create().show();
						
						return true;
					}
					
				});

    		populateList(xpp, inStream);
    		
    	} catch (Exception e) {
    		Log.e("jinzora", "error", e);
    	}
    }

    
    public void populateList(final XmlPullParser xpp, final InputStream inStream) {
    	new Thread() {
    		@Override
			public void run() {
				try {
					int eventType = xpp.getEventType();
					while (eventType != XmlPullParser.END_DOCUMENT) {
						if(eventType == XmlPullParser.START_DOCUMENT) {
			
						} else if(eventType == XmlPullParser.END_DOCUMENT) {
			
						} else if(eventType == XmlPullParser.START_TAG && 
								(xpp.getName().equals("nodes") || xpp.getName().equals("browse") || xpp.getName().equals("tracks"))) {
			
							int depth = xpp.getDepth();
							xpp.next();
			
							Bundle item = null;
							while (!(depth == xpp.getDepth() && eventType == XmlPullParser.END_TAG)) {	            	  
								if (depth+1 == xpp.getDepth() && eventType == XmlPullParser.START_TAG) {
									item = new Bundle();
								} else if (depth+1 == xpp.getDepth() && eventType == XmlPullParser.END_TAG) {
									/*if (item.containsKey("album")) {
			            			  item.put("subfield1", item.get("album"));
			            			  if (item.containsKey("artist")) {
			            				  item.put("subfield2",item.get("artist"));
			            			  }
			            		  } else*/ if (item.containsKey("artist")){
			            			  item.putString("subfield1", item.getString("artist"));
			            		  }
			
			            		  Message m = mAddEntriesHandler.obtainMessage();
			            		  m.setData(item);
			            		  mAddEntriesHandler.sendMessage(m);
								}
			
								if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("name")) {
									eventType = xpp.next();
									item.putString("name", xpp.getText());	           			  
								}
			
								if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("artist")) {
									eventType = xpp.next();
									item.putString("artist", xpp.getText());
								}
			
								if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("album")) {
									eventType = xpp.next();
									item.putString("album", xpp.getText());
								}
			
								if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("playlink")) {
									eventType = xpp.next();
									item.putString("playlink", xpp.getText());
								}
			
								if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("browse")) {
									eventType = xpp.next();
									item.putString("browse",xpp.getText());
								}
			
								eventType = xpp.next();
							}
						} else if(eventType == XmlPullParser.END_TAG) {
			
						} else if(eventType == XmlPullParser.TEXT) {
			
						}
						eventType = xpp.next();
					}
				} catch (Exception e) {
					Log.e("jinzora","Error processing XML",e);
				} finally {
					try {
						inStream.close();
					} catch (Exception e) {}
				}
    		}
    	}.start();
    }
    
    
    
    @Override
    public void onListItemClick(ListView l, View v, final int position, long id) {
    	try {
    		String mBrowse = null;
    		if (null == (mBrowse = visibleEntriesAdapter.getItem(position).getString("browse"))) {
    			if (visibleEntriesAdapter.getItem(position).containsKey("playlink")) {
					new Thread() {
						public void run() {
							try {
								Jinzora.sPbConnection.playbackBinding
									.playlist(visibleEntriesAdapter.getItem(position).getString("playlink"),
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
    
    private boolean matchesFilter(String entry) {
    	return curQuery.length() == 0 || entry.toUpperCase().contains(curQuery);
    }
    
    @Override
    public synchronized boolean onKeyUp(int keyCode, android.view.KeyEvent event) {
    	JzMediaAdapter workingEntries;
    	char c;
    	if ('\0' != (c = event.getMatch("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 ".toCharArray()))) {
    		curQuery = curQuery + c;
    		workingEntries = visibleEntriesAdapter;
    	} else if (keyCode == KeyEvent.KEYCODE_DEL) {
    		if (curQuery.length() > 0) {
    			curQuery = curQuery.substring(0, curQuery.length()-1);
    			if (curQuery.length() == 0) {
    				visibleEntriesAdapter = allEntriesAdapter;
        	        setListAdapter(allEntriesAdapter);
        	    	return super.onKeyUp(keyCode,event);
        		}
    			workingEntries = allEntriesAdapter;
    		} else {
    			return super.onKeyUp(keyCode,event);
    		}
    	} else {
    		return super.onKeyUp(keyCode,event);
    	}
    	
    	//TODO: support caching in the case of deletions?
    	// (as long as it doesn't use too much memory)
    	int count = workingEntries.getCount();
    	ArrayList<Bundle> newList = new ArrayList<Bundle>();
    	for (int i = 0; i < count; i++) {
    		if (matchesFilter(workingEntries.getItem(i).getString("name"))) {
    			newList.add(workingEntries.getItem(i));
    		}
    	}
    	
    	Log.d("jinzora","trying to set new list of size " + newList.size());
    	visibleEntriesAdapter = new JzMediaAdapter(this, newList);
        setListAdapter(visibleEntriesAdapter);
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
    
    class JzMediaAdapter extends ArrayAdapter<Bundle> {
    	Browser context;
    	public JzMediaAdapter(Browser context) {
    		super(context, R.layout.media_element);
    		this.context=context;
    	}
    	
    	public JzMediaAdapter(Browser context, List<Bundle>data) {
    		super(context,R.layout.media_element,data);
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View row;
    		if (convertView == null) {
    			if (Browser.sInflater == null) {
    				Browser.sInflater = LayoutInflater.from(context);
    			}
    			row=Browser.sInflater.inflate(R.layout.media_element, null);
    		} else {
    			row = convertView;
    		}

    		final Bundle item = (Bundle)this.getItem(position);
    		TextView label = (TextView)row.findViewById(R.id.media_el_name);
    		label.setText(item.getString("name"));

    		if (item.containsKey("subfield1")) {
    			label = (TextView)row.findViewById(R.id.media_el_subfield1);
    			label.setText(item.getString("subfield1"));
    		} else {
    			label = (TextView)row.findViewById(R.id.media_el_subfield1);
    			label.setText("");
    		}
    		if (item.containsKey("subfield2")) {
    			label = (TextView)row.findViewById(R.id.media_el_subfield2);
    			label.setText(item.getString("subfield2"));
    		} else {
    			label = (TextView)row.findViewById(R.id.media_el_subfield2);
    			label.setText("");
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
    								Jinzora.sPbConnection.playbackBinding.playlist(item.getString("playlink"), Jinzora.getAddType());
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

    	public Bundle getEntry(int pos) {
    		if (this.getCount() <= pos ) return null;
    		return (Bundle)visibleEntriesAdapter.getItem(pos);
    	}
    	
    	public String getEntryTitle(int pos) {
    		if (this.getCount() <= pos ) return null;
    		Bundle item = (Bundle)visibleEntriesAdapter.getItem(pos);
    		return item.getString("name");
    	}
    	
    	public boolean isPlayable(int pos) {
    		if (this.getCount() <= pos ) return false;
    		Bundle item = (Bundle)visibleEntriesAdapter.getItem(pos);
    		return item.containsKey("playlink");
    	}
    }
}