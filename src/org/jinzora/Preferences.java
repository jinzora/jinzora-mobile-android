package org.jinzora;

import java.util.Map;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Preferences extends PreferenceActivity {
	protected class MenuItems { 
		final static int SAVE = 1;
		final static int LOAD = 2;	
	}
	
	private static final int DIALOG_CONFIRM_PROFILE_DELETE = 1;
	private static String deleteProfile = null;
	private static Dialog mDialog = null;
	
	 @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Jinzora.initContext(this);
		this.getPreferenceManager().setSharedPreferencesName("main");
		addPreferencesFromResource(R.layout.preferences);
	}
	 
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
    		Preference preference) {
    	
    	/* should be a better method to override.. but this works. */
    	Jinzora.resetBaseURL();
    	Browser.clearBrowsing();
    	
    	return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	//return Jinzora.createMenu(menu);
    	
    	menu.add(0,MenuItems.SAVE,1,R.string.save)
    	.setIcon(android.R.drawable.ic_menu_save)
    	.setAlphabeticShortcut('s');
    	
    	menu.add(0,MenuItems.LOAD,2,R.string.load)
    	.setIcon(android.R.drawable.ic_menu_set_as)
    	.setAlphabeticShortcut('l');
    	   	
    	return true;
    	
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	final SharedPreferences preferences = getSharedPreferences("profiles", 0);
    	final SharedPreferences settings = getSharedPreferences("main", 0);
    	
    	switch (item.getItemId()) {
    	case MenuItems.SAVE:
    		String site = settings.getString("site", null);
    		String user = settings.getString("username",null);
    		String pass = settings.getString("password",null);
    		
    		String key;
    		if (user != null && user.length() > 0){
    			key = user + "@" + site;
    		} else {
    			key = site;
    		}
    		
    		SharedPreferences.Editor editor = preferences.edit();
    		editor.putString(key, pass);
    		editor.commit();
    		
    		break;
    	case MenuItems.LOAD:
    		try {
				final Map<String,?>profiles = preferences.getAll();
				
				final Dialog dl = new Dialog(this);
				dl.setTitle("Select Profile");
				dl.setContentView(R.layout.profiles);
				
				dl.show();
				
				String[] profileNames = profiles.keySet().toArray(new String[]{});
				for (int i=0;i<profileNames.length;i++) {
					profileNames[i] = profileNames[i].replace("http://", "");
					profileNames[i] = profileNames[i].replace("https://", "");
				}
				
				ListView listview = (ListView)dl.findViewById(R.id.profile_list);
				ArrayAdapter<String> adapter = 
					new ArrayAdapter<String>(this,
								android.R.layout.simple_list_item_1,
								profileNames);
				listview.setAdapter(adapter);
				
				listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						
						String key = (String)profiles.keySet().toArray()[position];
						SharedPreferences.Editor editor = settings.edit();
						
						if (key.contains("@")) {
							editor.putString("site", key.substring(key.indexOf("@")+1));
							editor.putString("username", key.substring(0,key.indexOf("@")));
						} else {
							editor.putString("site", key);
							editor.putString("username","");
						}
						editor.putString("password", (String)profiles.get(key));
						
						editor.commit();
						dl.hide();
						
						Jinzora.resetBaseURL();
						Browser.clearBrowsing();
						startActivity(new Intent(Preferences.this,Jinzora.class)); // ?
					}

				});
				
				listview.setLongClickable(true);
				listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> parent,
							View view, int position, long id) {
						deleteProfile = (String)profiles.keySet().toArray()[position];
						mDialog=dl;
						showDialog(DIALOG_CONFIRM_PROFILE_DELETE);

						return true;
					}
					
				});
				
				//Browser.clearBrowsing();
    		} catch (Exception e) {
    			Log.d("jinzora","Error loading profiles",e);
    		}
    		break;
    	}
    	
    	return true;
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CONFIRM_PROFILE_DELETE:
            return new AlertDialog.Builder(Preferences.this)
                .setTitle(R.string.delete_profile)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	
                    	SharedPreferences.Editor editor = getSharedPreferences("profiles", 0).edit();
                    	editor.remove(deleteProfile);
                    	editor.commit();
                    	
                    	deleteProfile = null;
                    	
                    	mDialog.hide();
                    }
                })
                .setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	deleteProfile = null;
                    }
                })
                .create();
        }
        
        return null;
    }
}
