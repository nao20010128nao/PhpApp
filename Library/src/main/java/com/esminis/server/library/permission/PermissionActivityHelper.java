/**
 * Copyright 2016 Tautvydas Andrikys
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
package com.esminis.server.library.permission;

import android.app.Activity;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class PermissionActivityHelper {

	@Inject
	protected PermissionRequester permissionRequester;

	private WeakReference<Activity> activity = new WeakReference<>(null);

	private String permission = null;
	private Subscriber<? super Boolean> permissionSubscriber = null;
	private Boolean onResumeGranted = null;

	@Inject
	public PermissionActivityHelper() {}

	public void onDestroy() {
		permissionSubscriber = null;
		permissionRequester.cleanup();
	}

	public void onResume(Activity activity) {
		this.activity = new WeakReference<>(activity);
		if (onResumeGranted != null) {
			final boolean granted = onResumeGranted;
			onResumeGranted = null;
			finished(granted);
		}
	}

	public void onPause() {
		activity = new WeakReference<>(null);
	}

	public void onRequestPermissionsResult(int requestCode, int[] grantResults) {
		permissionRequester.onRequestPermissionsResult(requestCode, grantResults);
	}

	public void request(final String permission, final @NonNull PermissionListener listener) {
		if (permissionRequester.hasPermission(activity.get(), permission)) {
			listener.onGranted();
		} else {
			Observable.create(new Observable.OnSubscribe<Boolean>() {
				@Override
				public void call(Subscriber<? super Boolean> subscriber) {
					PermissionActivityHelper.this.permission = permission;
					permissionSubscriber = subscriber;
					requestInternal();
				}
			}).subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread())
				.subscribe(
					new Action1<Boolean>() {
						@Override
						public void call(Boolean granted) {
							if (granted) {
								listener.onGranted();
							} else {
								listener.onDenied();
							}
						}
					}
				);
		}
	}

	private void finished(boolean granted) {
		if (activity.get() == null) {
			onResumeGranted = granted;
		} else {
			permissionSubscriber.onNext(granted);
			permissionSubscriber.onCompleted();
			permissionSubscriber = null;
		}
	}


	private void requestInternal() {
		permissionRequester.request(activity.get(), permission).subscribe(new Subscriber<Void>() {
			@Override
			public void onCompleted() {}

			@Override
			public void onError(Throwable e) {
				if (
					e instanceof PermissionRequestFailed &&
					((PermissionRequestFailed) e).code == PermissionRequestFailed.ACTIVITY_NOT_AVAILABLE
				) {
					if (activity.get() != null) {
						PermissionActivityHelper.this.requestInternal();
						return;
					}
				}
				finished(false);
			}

			@Override
			public void onNext(Void dummy) {
				finished(true);
			}
		});
	}

}
