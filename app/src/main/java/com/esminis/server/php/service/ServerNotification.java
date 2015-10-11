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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.esminis.server.php.MainActivity;
import com.esminis.server.php.R;
import com.esminis.server.php.model.manager.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ServerNotification {

	static private final int NOTIFICATION_ID = 1;

	private Notification notification = null;
	private boolean serviceIsRunning = false;

	@Inject
	protected Preferences preferences;

	private NotificationManager getManager(Context context) {
		return (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void hide(Context context) {
		getManager(context).cancel(NOTIFICATION_ID);
		notification = null;
		if (serviceIsRunning) {
			serviceIsRunning = false;
			context.stopService(new Intent(context, ServerNotificationService.class));
		}
	}

	public void show(Context context, CharSequence title, CharSequence titlePublic) {
		if (!preferences.getBoolean(context, Preferences.SHOW_NOTIFICATION_SERVER)) {
			hide(context);
			return;
		}
		if (!serviceIsRunning) {
			serviceIsRunning = true;
			context.startService(new Intent(context, ServerNotificationService.class));
		}
		if (notification == null) {
			Builder builder = setupNotificationBuilder(context, title)
				.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
				.setPublicVersion(setupNotificationBuilder(context, titlePublic).build());
			Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_notification_large);
			if (drawable != null && drawable instanceof BitmapDrawable) {
				builder.setLargeIcon(((BitmapDrawable) drawable).getBitmap());
			}
			Intent intent = new Intent(context, MainActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			builder.setContentIntent(
				PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
			);
			notification = builder.build();
		}
		getManager(context).notify(NOTIFICATION_ID, notification);
	}

	private Builder setupNotificationBuilder(Context context, CharSequence title) {
		Builder builder = new android.support.v7.app.NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_notification_small)
			.setContentTitle(context.getString(R.string.title)).setContentText(title)
			.setOnlyAlertOnce(true).setOngoing(true).setAutoCancel(false).setShowWhen(false);
		Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_notification_large);
		if (drawable != null && drawable instanceof BitmapDrawable) {
			builder.setLargeIcon(((BitmapDrawable) drawable).getBitmap());
		}
		return builder;
	}

}
