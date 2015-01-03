/**
 * Copyright 2014 Tautvydas Andrikys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.esminis.server.php.service.install;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import com.esminis.model.manager.Network;
import com.esminis.server.php.model.manager.Preferences;
import com.esminis.server.php.service.server.Php;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InstallServer extends AsyncTask<Context, Void, Boolean> {

	public interface OnInstallListener {

		public void OnInstallNewVersionRequest(InstallServer installer);

		public void OnInstallEnd(boolean success);
		
	}
	
	private OnInstallListener listener = null;

	private boolean canStartInstall = false;

	private boolean installStarted = false;

	@Inject
	protected Network network;

	@Inject
	protected Php php;

	@Inject
	protected Preferences preferences;

	public void installIfNeeded(OnInstallListener listener, Context context) {
		this.listener = listener;
		if (installStarted) {
			return;
		}
		File file = php.getPhp();
		if (file.isFile()) {
			if (!preferences.getIsSameBuild(context)) {
				if (listener != null) {
					listener.OnInstallNewVersionRequest(this);
				}
			} else {
				installEnd(true);
			}
		} else {
			startInstall(context);
		}
	}

	public void continueInstall(Context context, boolean confirm) {
		if (confirm) {
			startInstall(context);
		} else {
			installEnd(true);
		}
	}

	private void startInstall(Context context) {
		if (installStarted) {
			return;
		}
		installStarted = true;
		executeOnExecutor(THREAD_POOL_EXECUTOR, context);
	}

	private void installEnd(boolean success) {
		installStarted = false;
		if (listener != null) {
			listener.OnInstallEnd(success);
			listener = null;
		}
	}

	@Override
	protected Boolean doInBackground(Context... arguments) {
		Context context = arguments[0];
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
		if (!preferences.contains(context, Preferences.DOCUMENT_ROOT)) {
			File file = new File(Environment.getExternalStorageDirectory(), "www");
			File tempDirectory = new File(context.getExternalFilesDir(null), "tmp");
			if (!tempDirectory.isDirectory() && !tempDirectory.mkdir()) {
				tempDirectory = file;
			}
			if (!file.isDirectory()) {
				if (file.mkdir() && file.isDirectory()) {
					try {
						Install install = new Install();
						install.fromAssetDirectory(file, "www", context);
						HashMap<String, String> variables = new HashMap<>();
						variables.put("tempDirectory", tempDirectory.getAbsolutePath());
						install.preprocessFile(new File(file, "php.ini"), variables);
					} catch (IOException ignored) {}
				}
			}
			preferences.set(context, Preferences.DOCUMENT_ROOT, file.getAbsolutePath());
		}
		if (!preferences.contains(context, Preferences.PORT)) {
			preferences.set(context, Preferences.PORT, "8080");
		}
		if (!preferences.contains(context, Preferences.ADDRESS)) {
			preferences.set(context, Preferences.ADDRESS, network.get(0).name);
		}
		String[] list = preferences.getInstallPaths(context);
		File moduleDirectory = php.getPhp().getParentFile();
		if (!new Install().fromAssetFiles(moduleDirectory, list, context)) {
			return false;
		}

		HashMap<String, String> variables = new HashMap<>();
		variables.put("moduleDirectory", moduleDirectory.getAbsolutePath());
		new Install().preprocessFile(new File(php.getPhp().getParentFile(), "odbcinst.ini"), variables);

		preferences.set(context, Preferences.PHP_BUILD, preferences.getPhpBuild(context));
		return true;
	}

	@Override
	protected void onCancelled() {
		installEnd(false);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		installEnd(result);
	}
	
}