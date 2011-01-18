package org.jinzora.upnp;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.binding.LocalServiceBindingException;
import org.teleal.cling.binding.annotations.*;
import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.ValidationError;
import org.teleal.cling.model.ValidationException;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.DeviceDetails;
import org.teleal.cling.model.meta.DeviceIdentity;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.LocalService;
import org.teleal.cling.model.types.DeviceType;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.support.connectionmanager.ConnectionManagerService;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * Connects to the UPnP backend. Note that this service does not
 * worry about WiFi connectivity, since this is handled by the
 * AndroidWifiSwitchingRouter.
 */
public class UpnpService extends AndroidUpnpServiceImpl {
	private String TAG = "jinzora";
	private boolean mStarted;
	private WifiLock mWifiLock;
	private static Context lastContext;

	private static UpnpConfiguration sConfig;
	public static UpnpConfiguration getConfig() {
		return sConfig;
	}
	
	@SuppressWarnings("unchecked")
	LocalDevice createDevice() throws ValidationException,
			LocalServiceBindingException, IOException {

		DeviceType type = new UDADeviceType("MediaServer", 1);
		UpnpConfiguration config = new UpnpConfiguration(this);
		sConfig = config;
		DeviceIdentity identity = config.getDeviceIdentity();
		DeviceDetails details = config.getDeviceDetails();

		URI uri = null;
		try {
			uri = new URI("assets/icon.png");
		} catch (Exception e) {}
		Icon icon = new Icon("image/png", 48, 48, 8, uri, getResources().getAssets().open("icon.png"));

		List<LocalService> localServices = new ArrayList<LocalService>();
		
			LocalService<CompatContentDirectory> jinzoraMediaService = new AnnotationLocalServiceBinder()
					.read(CompatContentDirectory.class);
			jinzoraMediaService.setManager(new DefaultServiceManager<CompatContentDirectory>(
					jinzoraMediaService, CompatContentDirectory.class));
			localServices.add(jinzoraMediaService);
			
		LocalService<ConnectionManagerService> connectionManagerService =
	        new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
		connectionManagerService.setManager(
		        new DefaultServiceManager<ConnectionManagerService>(
		        		connectionManagerService,
		                ConnectionManagerService.class
		        )
		);
		localServices.add(connectionManagerService);
	
		LocalService<MSMediaReceiverRegistrarService> receiverService =
			new AnnotationLocalServiceBinder().read(MSMediaReceiverRegistrarService.class);
		receiverService.setManager(new DefaultServiceManager<MSMediaReceiverRegistrarService>(
				receiverService, MSMediaReceiverRegistrarService.class));
		localServices.add(receiverService);
		
		return new LocalDevice(identity, type, details, icon, localServices.toArray(new LocalService[] {}));
	}

	/*
	 * Several services can be bound to the same device: 
	 * return new LocalDevice(identity, type, details,
	 *  icon, new LocalService[] {switchPowerService, myOtherService} );
	 */
	
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		lastContext = this;
		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		mWifiLock = wifiManager.createWifiLock(TAG);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!mStarted) {
			mWifiLock.acquire();
			try {
				upnpService.getRegistry().addDevice(createDevice());
				
				mStarted = true;
				Toast.makeText(this, "Started DLNA service.", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				mWifiLock.release();
				
				Log.e(TAG, "Error starting UPnP service", e);
				if (e instanceof ValidationException) {
					ValidationException v = (ValidationException)e;
					for (ValidationError err : v.getErrors()) {
						Log.d(TAG, "   " + err.getMessage());
					}
				}
				
				Toast.makeText(this, "Failed to start DLNA service.", Toast.LENGTH_SHORT).show();
				stopSelf();
			}
		}
		
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (mStarted) {
			mStarted = false;
			mWifiLock.release();
			
			Toast.makeText(this, "Stopped DLNA service.", Toast.LENGTH_SHORT).show();
		}
	}

	public static void start(Context ctx) {
		Intent intent = new Intent(ctx, UpnpService.class);
		ctx.startService(intent);
	}
	
	public static void stop(Context ctx) {
		Intent intent = new Intent(ctx, UpnpService.class);
		ctx.stopService(intent);
	}
	
	public static Context getContext() {
		return lastContext;
	}
}
