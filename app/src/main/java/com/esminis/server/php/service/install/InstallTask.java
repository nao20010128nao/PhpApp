package com.esminis.server.php.service.install;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.esminis.model.manager.Network;
import com.esminis.server.php.model.manager.Preferences;
import com.esminis.server.php.service.server.Php;

class InstallTask extends AsyncTask<Void, Void, Boolean> {

	private final Object lock = new Object();

	private boolean installSuccess = false;
	private boolean canStartInstall = false;
	private Php php;
	private Context context;
	private InstallServer installServer;
	private Preferences preferences;
	private Network network;

	private Messenger messengerSender = null;
	private Messenger messengerReceiver = new Messenger(new Handler() {

		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
				case InstallService.ACTION_INSTALL_COMPLETE:
				case InstallService.ACTION_INSTALL_ERROR:
					synchronized (lock) {
						installSuccess = message.what == InstallService.ACTION_INSTALL_COMPLETE;
						lock.notify();
					}
					break;
				default:
					super.handleMessage(message);
			}
		}

	});

	private ServiceConnection connection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			messengerSender = new Messenger(service);
			synchronized (lock) {
				lock.notify();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			messengerSender = null;
		}
	};

	public InstallTask(
		Php php, InstallServer installServer, Preferences preferences, Network network,
		Context context
	) {
		this.php = php;
		this.context = context;
		this.network = network;
		this.preferences = preferences;
		this.installServer = installServer;
	}

	@Override
	protected Boolean doInBackground(Void... arguments) {
		BroadcastReceiver receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction() == null || !intent.getAction().equals(Php.INTENT_ACTION)) {
					return;
				}
				Bundle extras = intent.getExtras();
				if (extras == null || extras.containsKey("errorLine") || extras.getBoolean("running")) {
					return;
				}
				canStartInstall = true;
			}
		};
		context.registerReceiver(receiver, new IntentFilter(Php.INTENT_ACTION));
		canStartInstall = false;
		php.requestStop();
		while (!canStartInstall) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException ignored) {}
		}
		context.unregisterReceiver(receiver);
		bindMessenger(context);
		try {
			Message message = Message.obtain(null, InstallService.ACTION_INSTALL);
			message.replyTo = messengerReceiver;
			synchronized (lock) {
				messengerSender.send(message);
				try {
					lock.wait();
				} catch (InterruptedException ignored) {}
				if (!installSuccess) {
					return false;
				}
				initializePreferences();
				return true;
			}
		} catch (RemoteException e) {
			return false;
		} finally {
			context.unbindService(connection);
		}
	}

	private void initializePreferences() {
		if (!preferences.contains(context, Preferences.PORT)) {
			preferences.set(context, Preferences.PORT, "8080");
		}
		if (!preferences.contains(context, Preferences.ADDRESS)) {
			preferences.set(context, Preferences.ADDRESS, network.get(0).name);
		}
		if (!preferences.contains(context, Preferences.DOCUMENT_ROOT)) {
			preferences.set(
				context, Preferences.DOCUMENT_ROOT,
				preferences.getDefaultDocumentRoot().getAbsolutePath()
			);
		}
		preferences.set(context, Preferences.PHP_BUILD, preferences.getPhpBuild(context));
	}

	@Override
	protected void onCancelled() {
		installServer.finish(false);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		installServer.finish(result);
	}

	private void bindMessenger(Context context) {
		synchronized (lock) {
			context.bindService(
				new Intent(context, InstallService.class), connection, Context.BIND_AUTO_CREATE
			);
			try {
				lock.wait();
			} catch (InterruptedException ignored) {}
		}
	}

}