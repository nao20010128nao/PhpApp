/**
 * Copyright 2015 Tautvydas Andrikys
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
package com.esminis.server.php.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.esminis.server.php.Application;
import com.esminis.server.php.service.server.Php;

import javax.inject.Inject;

public class ServerNotificationService extends Service {

	@Inject
	protected ServerNotification serverNotification;

	@Inject
	protected Php php;

	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action != null && action.equals(Php.INTENT_ACTION)) {
				Bundle extras = intent.getExtras();
				if (extras == null || (!extras.containsKey("errorLine") && !extras.getBoolean("running"))) {
					serverNotification.hide(getApplicationContext());
				}
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		((Application)getApplication()).getObjectGraph().inject(this);
		registerReceiver(receiver, new IntentFilter(Php.INTENT_ACTION));
		php.requestStatus();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
		serverNotification.hide(getApplicationContext());
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
		serverNotification.hide(getApplicationContext());
		super.onDestroy();
	}
}