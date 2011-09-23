package org.jinzora.fragments;

import org.jinzora.Jinzora;
import org.jinzora.android.R;
import org.jinzora.playback.PlaybackService;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;



public class SearchFragment extends Fragment {
/*
	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return Jinzora.doKeyUp(this, keyCode, event);
    }
*/

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
        	final View mainView = inflater.inflate(R.layout.search, container, false);
        	mainView.findViewById(R.id.search_box).setOnKeyListener(new View.OnKeyListener() {
	        	 @Override
	        	public boolean onKey(View v, int keyCode, KeyEvent event) {
	        		if (event.getAction() == KeyEvent.ACTION_UP &&
	        			event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
	        			doSearch(mainView);
	        		}
	        		return false;
	        	}
	         });
	         
        	mainView.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {
	 			@Override
	 			public void onClick(View v) {
	 				doSearch(mainView);
	 			}
	 		});
	         
        	mainView.findViewById(R.id.quicksearch_button).setOnClickListener(new View.OnClickListener() {
		 			@Override
		 			public void onClick(final View view) {
		 				new Thread() {
		 					@Override
		 					public void run() {
		 						try {
		 							String query = ((EditText)mainView
		 							        .findViewById(R.id.search_box)).getText().toString();
		 							if (query != null && query.length() > 0) {
		 								Intent quickplay = PlaybackService.getQuickplayIntent();
		 								quickplay.putExtra("query", query);
		 								getActivity().startService(quickplay);
		 							}
		 						} catch (Exception e) {
		 							Log.e("jinzora","Error during search",e);
		 						}
		 					}
		 				}.start();
		 			}
		 		});
        	return mainView;
        } catch (Exception e) {
        	Log.e("jinzora", "Error building search fragment ui", e);
        	return null;
        }
	}

    private void doSearch(final View v) {
    	new Thread() {
				@Override
				public void run() {
					try {
						String query = ((EditText)v.findViewById(R.id.search_box)).getText().toString();
						if (query != null && query.length() > 0) {
							Intent intent = new Intent(getActivity(), Jinzora.class);
							intent.setAction(Intent.ACTION_SEARCH);
							intent.putExtra(SearchManager.QUERY, query);
							//intent.putExtra(Search.this.getPackageName()+".browse",Jinzora.getBaseURL()+"&request=search&query="+URLEncoder.encode(query, "UTF-8"));
				    		startActivity(intent);
						}
					} catch (Exception e) {
						Log.e("jinzora","Error during search",e);
					}
				}
			}.start();
    }
}