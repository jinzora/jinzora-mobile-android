package org.jinzora;

import android.content.ContentValues;
import android.content.SearchRecentSuggestionsProvider;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class SearchHistoryProvider extends SearchRecentSuggestionsProvider{
	public static String AUTHORITY="org.jinzora";
	public static int MODE=DATABASE_MODE_QUERIES;
	
	public SearchHistoryProvider() {
		//Log.d("jinzora","Building SearchHistoryProvider");
		setupSuggestions(AUTHORITY, MODE);
	}
	
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		//Log.d("jinzora","Called insert on SearchHistorProvider: " + values.toString());
		return super.insert(uri, values);
	}
}
