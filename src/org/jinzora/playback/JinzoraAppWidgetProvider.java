/*
 * Code borrowed from Android's media application:
 * http://github.com/android/platform_packages_apps_music/blob/master/src/com/android/music/MediaAppWidgetProvider.java
 * 
 * 
 * 
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.jinzora.playback;
 
import org.jinzora.Jinzora;
import org.jinzora.android.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
 
/**
 * Simple widget to show currently playing album art along
 * with play/pause and next track buttons.  
 */
public class JinzoraAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "jinzora";
    
    public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";
    
    static final ComponentName THIS_APPWIDGET =
        new ComponentName("org.jinzora.android",
                "org.jinzora.playback.JinzoraAppWidgetProvider");
    
    private static JinzoraAppWidgetProvider sInstance;
    
    static synchronized JinzoraAppWidgetProvider getInstance() {
        if (sInstance == null) {
            sInstance = new JinzoraAppWidgetProvider();
        }
        return sInstance;
    }
 
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        // Send broadcast intent to any running MediaPlaybackService so it can
        // wrap around with an immediate update.
        Intent updateIntent = new Intent(PlaybackService.SERVICECMD);
        updateIntent.putExtra(PlaybackService.CMDNAME,
                JinzoraAppWidgetProvider.CMDAPPWIDGETUPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }
    
    /**
     * Initialize given widgets to default state, where we launch Music on default click
     * and hide actions if service not running.
     */
    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        final Resources res = context.getResources();
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.album_appwidget);
        
        views.setViewVisibility(R.id.title, View.GONE);
        views.setTextViewText(R.id.artist, res.getText(R.string.emptyplaylist));
 
        linkButtons(context, views, false /* not playing */);
        pushUpdate(context, appWidgetIds, views);
    }
    
    private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            gm.updateAppWidget(appWidgetIds, views);
        } else {
            gm.updateAppWidget(THIS_APPWIDGET, views);
        }
    }
    
    /**
     * Check against {@link AppWidgetManager} if there are any instances of this widget.
     */
    private boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        return (appWidgetIds.length > 0);
    }
 
    /**
     * Handle a change notification coming over from {@link MediaPlaybackService}
     */
    void notifyChange(PlaybackService service, String what) {
        if (hasInstances(service)) {
            if (PlaybackService.PLAYBACK_COMPLETE.equals(what) ||
                    PlaybackService.META_CHANGED.equals(what) ||
                    PlaybackService.PLAYSTATE_CHANGED.equals(what)) {
                performUpdate(service, null);
            }
        }
    }
    
    /**
     * Update all active widget instances by pushing changes 
     */
    void performUpdate(PlaybackService service, int[] appWidgetIds) {
        final RemoteViews views = new RemoteViews(service.getPackageName(), R.layout.album_appwidget);
        
        CharSequence titleName = service.getTrackName();
        CharSequence artistName = service.getArtistName();
        CharSequence errorState = null;
        
        if (errorState != null) {
            // Show error state to user
            views.setViewVisibility(R.id.title, View.GONE);
            views.setTextViewText(R.id.artist, errorState);
            
        } else {
            // No error, so show normal titles
            views.setViewVisibility(R.id.title, View.VISIBLE);
            views.setTextViewText(R.id.title, titleName);
            views.setTextViewText(R.id.artist, artistName);
        }
        
        // Set correct drawable for pause state
        final boolean playing = service.isPlaying();
        if (playing) {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_pause);
        } else {
            views.setImageViewResource(R.id.control_play, R.drawable.ic_appwidget_music_play);
        }
 
        // Link actions buttons to intents
        linkButtons(service, views, playing);
        pushUpdate(service, appWidgetIds, views);
    }
 
    /**
     * Link up various button actions using {@link PendingIntents}.
     * 
     * @param playerActive True if player is active in background, which means
     *            widget click will launch {@link MediaPlaybackActivity},
     *            otherwise we launch {@link MusicBrowserActivity}.
     */
    private void linkButtons(Context context, RemoteViews views, boolean playerActive) {
        // Connect up various buttons and touch events
        Intent intent;
        PendingIntent pendingIntent;
        
        final ComponentName serviceName = new ComponentName(context, PlaybackService.class);
        
        Intent playerIntent = new Intent(context, Jinzora.class);
        playerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        playerIntent.putExtra(Jinzora.EXTRA_SWITCH_TAB, "playback");
        pendingIntent = PendingIntent.getActivity(context,
                0 /* no requestCode */, playerIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);

        intent = new Intent(PlaybackService.Intents.ACTION_CMD_PLAYPAUSE);
        pendingIntent = PendingIntent.getBroadcast(context,
                0 /* no requestCode */, intent, 0 /* no flags */);
        views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

        intent = new Intent(PlaybackService.Intents.ACTION_CMD_NEXT);
        //intent.setComponent(serviceName);
        pendingIntent = PendingIntent.getBroadcast(context,
                0 /* no requestCode */, intent, 0);
        views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
    }
}