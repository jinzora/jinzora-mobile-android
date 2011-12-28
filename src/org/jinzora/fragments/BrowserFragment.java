package org.jinzora.fragments;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mobisocial.nfc.NdefFactory;

import org.jinzora.Jinzora;
import org.jinzora.android.R;
import org.jinzora.download.DownloadService;
import org.jinzora.playback.PlaybackService;
import org.jinzora.util.DrawableManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

public class BrowserFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<Bundle>> {
    private final String TAG = "jinzora";
	private JzMediaAdapter allEntriesAdapter = null;
	private JzMediaAdapter visibleEntriesAdapter = null;

	private URL mUrl;
	protected LayoutInflater mInflater = null;
	private String curQuery = "";
	private ListView mListView;
	private boolean mButtonNav = false;

	private static final String STATE_LIST_POSITION = "listpos";
	private int mLastPosition = -1;
	private Uri mShareLink;

	Handler mAddEntriesHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
		    synchronized (this) {
    			allEntriesAdapter.add(msg.getData());
    			if (allEntriesAdapter != visibleEntriesAdapter) {
    				if (matchesFilter(msg.getData().getString("name"))) {
    					visibleEntriesAdapter.add(msg.getData());
    				}
    			}
		    }
		}
	};

    @Override
    public void onResume() {
        super.onResume();

        if (mShareLink != null) {
            Jinzora.mNfc.share(mShareLink);
        }
    }
	
	@Override
    public void onPause() {
		super.onPause();
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        synchronized (this) {
            if (mUrl != null) {
                allEntriesAdapter = new JzMediaAdapter(BrowserFragment.this);
                visibleEntriesAdapter = allEntriesAdapter;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            Log.d(TAG, "Attached fragment.");
            for (String arg : getArguments().keySet()) {
                Log.d(TAG, arg);
            }
            mUrl = new URL(getArguments().getString("browsing"));
        } catch (MalformedURLException e) {
            Log.e(TAG, "Could not load browsing URL", e);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String playlink = getArguments().getString("playlink");
        String browsing = getArguments().getString("browsing");
        /*if (playlink != null) {
            Log.d(TAG, "Setting ndef for " + playlink);
            Jinzora.mNfc.share(NdefFactory.fromUri(Uri.parse(playlink)));
        } else*/ if (browsing != null) {
            Uri webAccess = Uri.parse("http://bjdodson.com/jinzora/demo.html#" + browsing);
            Log.d(TAG, "Setting ndef for " + webAccess);
            mShareLink = webAccess;
            Jinzora.mNfc.share(NdefFactory.fromUri(webAccess));
        } else {
            Log.d(TAG, "No link for sharing.");
            Jinzora.mNfc.share(NdefFactory.fromUri("http://jinzora.org"));
        }

        mListView = getListView();
        mListView.setVisibility(View.VISIBLE);
        synchronized (this) {
            setListAdapter(allEntriesAdapter);
        }

        mListView.setOnItemClickListener(mListClickListener);
        mListView.setOnItemLongClickListener(mListLongClickListener);
        mListView.setOnKeyListener(mListKeyListener);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState != null) {
            mLastPosition = savedInstanceState.getInt(STATE_LIST_POSITION);
        } else {
            mLastPosition = -1;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.browse, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isResumed()) {
            outState.putInt(STATE_LIST_POSITION, getListView().getFirstVisiblePosition());
        }
    }

    private boolean matchesFilter(String entry) {
    	return curQuery.length() == 0 || entry.toUpperCase().contains(curQuery);
    }

    private synchronized boolean onKeyUp(int keyCode, KeyEvent event) {
    	JzMediaAdapter workingEntries;
    	char c;
    	if (event.getAction() != KeyEvent.ACTION_UP) {
    	    return false;
    	}

    	if ('\0' != (c = event.getMatch("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 ".toCharArray()))) {
    		curQuery = curQuery + c;
    		workingEntries = visibleEntriesAdapter;
    	} else if (keyCode == KeyEvent.KEYCODE_DEL) {
    		if (curQuery.length() > 0) {
    			curQuery = curQuery.substring(0, curQuery.length()-1);
    			if (curQuery.length() == 0) {
    				visibleEntriesAdapter = allEntriesAdapter;
        	        setListAdapter(allEntriesAdapter);
        	    	return true;
        		}
    			workingEntries = allEntriesAdapter;
    		} else {
    			return true;
    		}
    	} else {
    		return false;
    	}

    	if (workingEntries == null) {
    	    return false;
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
        Log.d(TAG, "SET " + newList.size() + " + ITEMS");
        return true;
    }

    private Character[] mSectionsArray;
    int[] sectionHeaders = new int[26];
    int[] sectionPositions = new int[26];
    
    class JzMediaAdapter extends ArrayAdapter<Bundle> implements SectionIndexer {
        private final DrawableManager mDrawableManager =
                DrawableManager.forKey(getActivity(), mUrl.toExternalForm());
    	private boolean isAlphabetical = true;
    	private boolean hasArtwork = false;
    	
    	public JzMediaAdapter(BrowserFragment context) {
    		super(getActivity(), R.layout.media_element);
    	}
    	
    	public JzMediaAdapter(BrowserFragment fragment, List<Bundle>data) {
    		super(fragment.getActivity(), R.layout.media_element, data);
    		for (Bundle b : data) {
    		    if (b.containsKey("thumbnail")) {
    		        hasArtwork = true;
    		        break;
    		    }
    		}
    		buildSectionIndices();
    	}

    	@Override
    	public View getView(final int position, View convertView, ViewGroup parent) {
    		View row;
    		if (convertView == null) {
    			if (BrowserFragment.this.mInflater == null) {
    				BrowserFragment.this.mInflater = LayoutInflater.from(getActivity());
    			}
    			row = BrowserFragment.this.mInflater.inflate(R.layout.media_element, null);
    		} else {
    			row = convertView;
    			// Reusing a view; reset the ImageView since
    			// it will be set asynchronously.
    			((ImageView)row.findViewById(R.id.media_artwork))
    			    .setImageResource(R.drawable.albumart_mp_unknown);
    		}

    		final Bundle item = (Bundle)this.getItem(position);
    		TextView label = (TextView)row.findViewById(R.id.media_el_name);
    		label.setText(item.getString("name"));

    		if (item.containsKey("subfield1")) {
    			label = (TextView)row.findViewById(R.id.media_el_subfield1);
    			label.setVisibility(View.VISIBLE);
    			label.setText(item.getString("subfield1"));
    		} else {
    		    row.findViewById(R.id.media_el_subfield1).setVisibility(View.GONE);
    		}
    		if (item.containsKey("subfield2")) {
    			label = (TextView)row.findViewById(R.id.media_el_subfield2);
    			label.setVisibility(View.VISIBLE);
    			label.setText(item.getString("subfield2"));
    		} else {
    			row.findViewById(R.id.media_el_subfield2).setVisibility(View.GONE);
    		}

    		if (item.containsKey("playlink")) {
    		    Button button = (Button)row.findViewById(R.id.media_el_play);
                button.setTag(R.id.media_el_play, item.getString("playlink"));
                button.setTag(R.id.media_el_subfield1, position);
                button.setOnClickListener(mPlayButtonClicked);
                button.setOnKeyListener(mPlayButtonKeyListener);
                if (mButtonNav) {
                    button.setFocusable(true);
                }
    		} else {
    		   row.findViewById(R.id.media_el_play).setVisibility(View.GONE);
            }

    		if (hasArtwork) {
    		    ImageView thumb = (ImageView)row.findViewById(R.id.media_artwork);
    		    thumb.setVisibility(View.VISIBLE);
    		    mDrawableManager.fetchDrawableOnThread(item.getString("thumbnail"), thumb);
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
    		buildSectionIndices();
    	};

    	private void buildSectionIndices() {
    	    List<Character> sections = new ArrayList<Character>();
    	    int count = getCount();
    	    if (count == 0) return;
    	    String entry = null;
    	    String previous = null;
    	    for (int i = 0; i < count; i++) {
    	        previous = entry;
    	        entry = getEntryTitle(i).toUpperCase();
        		if (i == 0) {
        			char c2 = entry.charAt(0);
        			if (c2 <= 'A') {
        			    sections.add('A');
        			} else if (c2 >= 'Z') {
        			    sections.add('Z');
        			} else {
        			    sections.add(c2);
        			}
        			continue;
        		}
        		
        		if (previous.length() == 0 || entry.length() == 0) {
        			// what happened?
        			isAlphabetical = false;
        			continue;
        		}
        		
        		if (previous.compareTo(entry) > 0) {
        			isAlphabetical = false;
        		} else if (isAlphabetical) {
    				char c = entry.charAt(0);
    				if (c < 'A') c = 'A';
					else if (c > 'Z') c = 'Z';
    				
    				if (sections.get(sections.size()-1) != c) {
    				    sections.add(c);
    					sectionHeaders[sections.size()-1] = i;
    					sectionPositions[c - 'A'] = sections.size() - 1;
    				}
        		}
    	    }
    	    mSectionsArray = new Character[sections.size()];
    	    sections.toArray(mSectionsArray);
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
			return mSectionsArray;
		}
    }

    private AdapterView.OnItemClickListener mListClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, final int position, long id) {
            try {
                String browse = null;
                if (null == (browse = visibleEntriesAdapter.getItem(position).getString("browse"))) {
                    if (visibleEntriesAdapter.getItem(position).containsKey("playlink")) {
                        new Thread() {
                            public void run() {
                                try {
                                    doPlaylist(position);
                                } catch (Exception e) {
                                    Log.e("jinzora","Error playing media",e);
                                }
                            }
                        }.start();
                    }
                    return;
                }

                Bundle args = new Bundle();
                if (visibleEntriesAdapter.getItem(position).containsKey("playlink")) {
                    args.putString("playlink", visibleEntriesAdapter.getItem(position).getString("playlink"));
                }
                args.putString("browsing", browse);
                Intent newBrowser = new Intent(getActivity(), Jinzora.class);
                newBrowser.putExtras(args);
                startActivity(newBrowser);
            } catch (Exception e) {
                Log.e("jinzora","Error during listItemClick",e);
            }
        }
    };

    private AdapterView.OnItemLongClickListener mListLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent,
                View view, final int listPosition, long id) {

            final String title = visibleEntriesAdapter.getEntryTitle(listPosition);
            final CharSequence[] entryOptions = {"Share", "Replace current playlist", "Queue to end of list", "Queue next", "Download to device" };
            if (!visibleEntriesAdapter.isPlayable(listPosition)) return false;
            new AlertDialog.Builder(getActivity())
                .setTitle(title)
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
                                        .putExtra(Intent.EXTRA_TEXT, item.getString("playlink"))
                                        .putExtra(Intent.EXTRA_TITLE, title);
                                    BrowserFragment.this
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
                                                Jinzora.doPlaylist(item.getString("playlink"), entryPos-1);
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
                                        Intent dlIntent = new Intent(DownloadService.Intents.ACTION_DOWNLOAD_PLAYLIST);
                                        dlIntent.putExtra("playlist", item.getString("playlink"));
                                        dlIntent.setClass(getActivity(), DownloadService.class);
                                        getActivity().startService(dlIntent);
                                        /*Jinzora
                                          .sDlConnection
                                          .getBinding()
                                          .downloadPlaylist(item.getString("playlink"));*/
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
    };

    private View.OnKeyListener mListKeyListener = new View.OnKeyListener() {
        @Override
        public synchronized boolean onKey(View view, int keyCode, KeyEvent event) {
            if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode) {
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    return true;
                }
                final View w = mListView.getSelectedView();
                if (w == null) return false;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        View v = w.findViewById(R.id.media_el_play);
                        if (v.getVisibility() == View.VISIBLE) {
                            mListView.setItemsCanFocus(true);
                            mListView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
                            int c = mListView.getChildCount();
                            for (int i = 0; i < c; i++) {
                                View u = mListView.getChildAt(i).findViewById(R.id.media_el_play);
                                u.setFocusable(true);
                            }
                            v.requestFocus(View.FOCUS_RIGHT);
                            mButtonNav = true;
                        }
                    }
                });
                return true;   
            } else {
                return onKeyUp(keyCode, event);
            }
        }
    };

    private View.OnClickListener mPlayButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            new Thread() {
                public void run() {
                    try {
                        doPlaylist((Integer)v.getTag(R.id.media_el_subfield1));
                    } catch (Exception e) {
                        Log.e("jinzora","Error playing media",e);
                    }
                }
            }.start();
        }
    };

    private View.OnKeyListener mPlayButtonKeyListener = new View.OnKeyListener() {
        @Override
        public synchronized boolean onKey(final View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (event.getAction() != KeyEvent.ACTION_UP) {
                    return true;
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int c = mListView.getChildCount();
                        for (int i = 0; i < c; i++) {
                            View u = mListView.getChildAt(i);
                            u.findViewById(R.id.media_el_play).setFocusable(false);
                        }
                        mListView.setItemsCanFocus(false);
                        mListView.setItemChecked((Integer)v.getTag(R.id.media_el_subfield1), true);
                        mListView.requestFocus(View.FOCUS_LEFT);
                        mButtonNav = false;
                    }
                });
                return true;
            }
            return false;
        }
    };

    static class MediaListLoader extends AsyncTaskLoader<List<Bundle>> {
        private InputStream inStream = null;
        private String mEncoding;
        private final URL mmUrl;
        private final BrowserFragment mmFragment;
        private static List<Bundle> sLastResults;
        private static URL sLastUrl;
        List<Bundle> mmResults;

        public MediaListLoader(BrowserFragment fragment, URL url) {
            super(fragment.getActivity());
            mmUrl = url;
            mmFragment = fragment;
        }

        protected void onStartLoading() {
            if (mmResults == null) {
                if (sLastUrl != null && sLastUrl.equals(mmUrl)) {
                    mmResults = sLastResults;
                }
            }

            if (mmResults != null) {
                deliverResult(mmResults);
            }
            if (takeContentChanged() || mmResults == null) {
                forceLoad();
            }
        }

        @Override
        public List<Bundle> loadInBackground() {
            try {
                HttpURLConnection conn = (HttpURLConnection)mmUrl.openConnection();
                conn.setConnectTimeout(20000);
                inStream = conn.getInputStream();
                conn.connect();
                mEncoding = conn.getContentEncoding();
            } catch (Exception e) {
            }

            if (inStream == null) {
                Log.w("jinzora","could not connect to server");
                return null;
            }

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp;
                xpp = factory.newPullParser();
                xpp.setInput(inStream, mEncoding);
                
                mmResults = populateList(xpp, inStream);
            } catch  (Exception e) {
                Log.e("jinzora","could not populate list",e);
            }
            return mmResults;
        }

        private List<Bundle> populateList(XmlPullParser xpp, InputStream inStream) {
            final Activity activity = mmFragment.getActivity();
            List<Bundle> results = new ArrayList<Bundle>();
            try {
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_DOCUMENT) {
        
                    } else if(eventType == XmlPullParser.END_DOCUMENT) {
        
                    } else if(eventType == XmlPullParser.START_TAG && 
                            (xpp.getName().equals("login"))) {

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView)activity.findViewById(R.id.browse_notice)).setText(R.string.bad_login);
                                activity.findViewById(R.id.browse_notice).setVisibility(View.VISIBLE);
                            }
                        });
                        return results;
                    } else if(eventType == XmlPullParser.START_TAG && 
                            (xpp.getName().equals("nodes") || xpp.getName().equals("browse") || xpp.getName().equals("tracks"))) {

                        int depth = xpp.getDepth();
                        xpp.next();

                        Bundle item = null;
                        while (!(depth == xpp.getDepth() && eventType == XmlPullParser.END_TAG)) {                    
                            if (depth + 1 == xpp.getDepth() && eventType == XmlPullParser.START_TAG) {
                                item = new Bundle();
                            } else if (depth + 1 == xpp.getDepth()
                                    && eventType == XmlPullParser.END_TAG) {
                                /*
                                 * if (item.containsKey("album")) {
                                 * item.put("subfield1", item.get("album")); if
                                 * (item.containsKey("artist")) {
                                 * item.put("subfield2",item.get("artist")); } }
                                 * else
                                 */
                                if (item.containsKey("artist")) {
                                    item.putString("subfield1", item.getString("artist"));
                                }

                                results.add(item);
                                /*if (results.size() % UPDATE_INTERVAL == 0) {
                                    deliverResult(results);
                                }*/
                            }
        
                            if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("name")) {
                                eventType = xpp.next();
                                item.putString("name", xpp.getText());                        
                            }

                            // "thumbnail" is 75x75; "image" is full-size.
                            if (eventType == XmlPullParser.START_TAG && xpp.getName() != null && xpp.getName().equals("thumbnail")) {
                                eventType = xpp.next();
                                String thumb = xpp.getText().trim();
                                if (thumb.length() > 0) {
                                    item.putString("thumbnail", thumb);
                                }
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
                return results;
            } catch (Exception e) {
                Log.e("jinzora","Error processing XML",e);
                return null;
            } finally {
                try {
                    inStream.close();
                } catch (Exception e) {
                    Log.w("jinzora","Error closing stream",e);
                }
            }
        }
    }

    private void doPlaylist(int position) {
        /* Broadcast */
        String playlink = visibleEntriesAdapter.getItem(position).getString("playlink");
        String artist = visibleEntriesAdapter.getItem(position).getString("artist");
        String name = visibleEntriesAdapter.getItem(position).getString("name");
        Intent playlistIntent = new Intent(PlaybackService.META_CHANGED_GOOGLE);
        playlistIntent.putExtra("artist", artist);
        playlistIntent.putExtra("track", name);
        playlistIntent.putExtra("url", playlink);
        // TODO: display bugs.
        getActivity().sendBroadcast(playlistIntent);

        ((Jinzora)getActivity()).setTab("player");
        Jinzora.doPlaylist(playlink, Jinzora.getAddType());
        Jinzora.mNfc.share(NdefFactory.fromUri(Uri.parse(playlink)));
    }

    @Override
    public Loader<List<Bundle>> onCreateLoader(int arg0, Bundle arg1) {
        return new MediaListLoader(this, mUrl);
    }

    @Override
    public void onLoadFinished(Loader<List<Bundle>> loader, List<Bundle> result) {
        synchronized (this) {
            if (result == null) {
                ((TextView)getActivity().findViewById(R.id.browse_notice)).setText(R.string.connection_failed);
                getActivity().findViewById(R.id.browse_notice).setVisibility(View.VISIBLE);
            } else {
                getActivity().findViewById(R.id.browse_notice).setVisibility(View.GONE);
                MediaListLoader.sLastResults = result;
                MediaListLoader.sLastUrl = mUrl;
                allEntriesAdapter = new JzMediaAdapter(this, result);
                visibleEntriesAdapter = allEntriesAdapter;
                setListAdapter(allEntriesAdapter);
                if (mLastPosition != -1) {
                    getListView().setSelection(mLastPosition);
                }
                getListView().setFastScrollEnabled(true);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Bundle>> loader) {
        MediaListLoader.sLastResults = null;
        MediaListLoader.sLastUrl = null;
    }
}