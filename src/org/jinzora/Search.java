package org.jinzora;

import org.jinzora.playback.PlaybackService;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;



public class Search extends Activity {
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
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
	        		
	        			doSearch();
	        		}
	        		
	        		return false;
	        	}
	         });
	         
	         this.findViewById(R.id.search_button).setOnClickListener(new View.OnClickListener() {

	 			@Override
	 			public void onClick(View v) {
	 			
	 				doSearch();
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
		 								Intent quickplay = PlaybackService.getQuickplayIntent();
		 								quickplay.putExtra("query", query);
		 								startService(quickplay);
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
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	return Jinzora.createMenu(menu);
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	Jinzora.menuItemSelected(featureId,item, this);
    	return super.onMenuItemSelected(featureId, item);
    }
    
    private void doSearch() {
    	new Thread() {
				@Override
				public void run() {
					try {
						String query = ((EditText)findViewById(R.id.search_box)).getText().toString();
						if (query != null && query.length() > 0) {
							Intent intent = new Intent(Search.this, Jinzora.class);
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