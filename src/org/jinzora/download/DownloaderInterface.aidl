package org.jinzora.download;

interface DownloaderInterface {
	//void downloadPlaylist( in String pl);
	List<String> getPendingDownloads();
	void cancelAllDownloads();
	void cancelDownload(in int indexGuess, in String label);
}