package org.jinzora.playback;

interface PlaybackInterface {
	void playlist( in String pl );
	void pause();
	void stop();
	void prev();
	void next();
	void clear();
	void jumpTo( in int pos );
	void onDestroy();
	void setBaseURL( in String url );
	void setAddType(in int type);
	void setPlaybackDevice( in String playerClass, in String arg );
	
	String playbackIPC ( in String input );
}