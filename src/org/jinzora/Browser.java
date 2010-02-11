package org.jinzora;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.SectionIndexer;
import android.widget.TextView;

public class Browser extends ListActivity {
	private JzMediaAdapter allEntriesAdapter = null;
	private JzMediaAdapter visibleEntriesAdapter = null;
	
	protected String browsing;
	protected LayoutInflater mInflater = null;
	private String curQuery = "";
	private boolean mContentLoaded=false;
	
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
	
	Handler mEntriesCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			allEntriesAdapter.finalize();
		}
	};
	
	
	@Override
	protected void onStart() {
		super.onStart();
		
		
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		String newBrowsing=null;
		if (null != getIntent().getStringExtra(getPackageName()+".browse")) {
			// todo: get rid of this static.
			newBrowsing = getIntent().getStringExtra(getPackageName()+".browse");
		} else {
	   		newBrowsing = getHomeURL();
	   		if (null  == newBrowsing) {
	   			startActivity(new Intent(this, Preferences.class));
	   			return;
	   		}
       }
		
		if (browsing == null || !browsing.equals(newBrowsing) || !mContentLoaded) {
			browsing = newBrowsing;
			doBrowsing();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Jinzora.initContext(this);
        
        allEntriesAdapter = new JzMediaAdapter(Browser.this);
        visibleEntriesAdapter = allEntriesAdapter;
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
    		
    		ProgressDialog dialog = ProgressDialog.show(Browser.this, "", 
                    Browser.this.getResources().getText(R.string.loading), 
                    true, true, new DialogInterface.OnCancelListener() {
						
						@Override
						public void onCancel(DialogInterface arg0) {
							Browser.this.finish();
						}
					});

    		
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
    			dialog.hide();
    			Log.w("jinzora","could not connect to server",e);
    			
    			JzMediaAdapter adapter = new JzMediaAdapter(this, new ArrayList<Bundle>());
        		setContentView(R.layout.browse);
        		setListAdapter(adapter);
        		
        		((TextView)findViewById(R.id.browse_notice)).setText(R.string.connection_failed);
        		findViewById(R.id.browse_notice).setVisibility(View.VISIBLE);
    			return;
    		}
    		
    		// Not completely done loading, but have something to display
			dialog.hide();
			
			mContentLoaded=true;
			setContentView(R.layout.browse);
			((ListView)findViewById(android.R.id.list)).setVisibility(View.VISIBLE);
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
			
			            		  /*
			            		  if (isActivityPaused) {
			            			  // A fairly gross hack to fix the bug
			            			  // when a user presses 'back'
			            			  // while the page is still loading.
			            			  // Probably a better way to handle it.
			            			  inStream.close();
			            			  Log.d("jinzora","killed it");
			            			  mContentLoaded=false; // force refresh on resume
			            			  return;
			            		  }
			            		  */
			            		  
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
					} catch (Exception e) {
						Log.w("jinzora","Error closing stream",e);
					}
				}
				
				mEntriesCompleteHandler.sendEmptyMessage(0);
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
    
    private static List<Character> mSections = new ArrayList<Character>();
    int[] sectionHeaders = new int[26];
    int[] sectionPositions = new int[26];
    
    class JzMediaAdapter extends ArrayAdapter<Bundle> implements SectionIndexer {
    	private boolean isAlphabetical = true;
    	private boolean isFinishedLoading = false;
    	
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
    			if (Browser.this.mInflater == null) {
    				Browser.this.mInflater = LayoutInflater.from(context);
    			}
    			row=Browser.this.mInflater.inflate(R.layout.media_element, null);
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
    		return (Bundle)this.getItem(pos);
    	}
    	
    	public String getEntryTitle(int pos) {
    		if (this.getCount() <= pos ) return null;
    		Bundle item = (Bundle)this.getItem(pos);
    		return item.getString("name");
    	}
    	
    	public boolean isPlayable(int pos) {
    		if (this.getCount() <= pos ) return false;
    		Bundle item = (Bundle)this.getItem(pos);
    		return item.containsKey("playlink");
    	}

    	
    	
    	// SectionIndexer
    	
    	@Override 
    	public void clear() {
    		super.clear();
    		
    		mSections.clear();
    		sectionHeaders = new int[26];
    	};
    	
    	@Override
    	public void add(Bundle object) {
    		super.add(object);
    		
    		int len = this.getCount();
    		String entry2 = getEntryTitle(len-1).toUpperCase();
    		if (len == 1) {
    			char c2 = entry2.charAt(0);
    			if (c2 <= 'A')
    				mSections.add('A');
    			else if (c2 >= 'Z')
    				mSections.add('Z');
    			else
    				mSections.add(c2);
    			return;
    		}
    		
    		String entry1 = getEntryTitle(len-2).toUpperCase();
    		if (entry1.length() == 0 || entry2.length() == 0) {
    			// what happened?
    			isAlphabetical = false;
    			return;
    		}
    		
    		if (entry1.compareTo(entry2) > 0) {
    			isAlphabetical = false;
    		} else if (isAlphabetical) {
    				char c = entry2.charAt(0);
    				if (c < 'A') c = 'A';
					else if (c > 'Z') c = 'Z';
    				
    				if (mSections.get(mSections.size()-1) != c) {
    					mSections.add(c);
    					sectionHeaders[mSections.size()-1]=len-1;
    					sectionPositions[c-'A']=mSections.size()-1;
    				}
    		}
    	}
    	
		@Override
		public int getPositionForSection(int sec) {
			return sectionHeaders[sec];
		}

		@Override
		public int getSectionForPosition(int pos) {
			String entry = getEntryTitle(pos);
			if (entry != null && entry.length() > 0) {
				char c = Character.toUpperCase(entry.charAt(0));
				if (c <= 'A') {
					return 0;
				}
				if (c >= 'Z') {
					return sectionHeaders.length-1;
				}
				
				return sectionPositions[c-'A'];
			}
			return 0;
		}

		@Override
		public Object[] getSections() {
			if (isAlphabetical && isFinishedLoading) {
				return mSections.toArray();
			}
			return null;
		}
		
		// Called when all data has been loaded
		public void finalize() {
			isFinishedLoading=true;
		}
    }
}