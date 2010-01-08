package org.jinzora.download;

interface DownloaderInterface {
	void downloadPlaylist( in String pl);
	List<String> getPendingDownloads();
	void cancelAllDownloads();
}