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
package com.esminis.server.library.service.server.install;

import android.content.Context;

import com.esminis.server.library.application.LibraryApplicationComponent;
import com.esminis.server.library.preferences.Preferences;
import com.esminis.server.library.service.server.ServerControl;

import java.io.File;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class InstallServer {

	private final ServerControl serverControl;
	private final Preferences preferences;

	private OnInstallServerListener listener = null;
	private Subscription install = null;
	private final Object lock = new Object();
	private final LibraryApplicationComponent component;

	public InstallServer(
		Preferences preferences, ServerControl serverControl, LibraryApplicationComponent component
	) {
		this.preferences = preferences;
		this.serverControl = serverControl;
		this.component = component;
	}

	public void install(Context context, OnInstallServerListener listener) {
		synchronized (lock) {
			this.listener = listener;
			if (install != null) {
				return;
			}
		}
		final File file = serverControl.getBinary();
		if (file.isFile() && preferences.getIsInstalled(context)) {
			if (!preferences.getIsSameBuild(context)) {
				if (this.listener != null) {
					this.listener.OnInstallNewVersionRequest(this);
				}
			} else {
				finish(null);
			}
		} else {
			start();
		}
	}

	public void installNewVersionConfirmed() {
		start();
	}

	public void installFinish() {
		finish(null);
	}

	private void start() {
		synchronized (lock) {
			if (install != null) {
				return;
			}
			install = Observable.create(component.getInstallTask())
				.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe(
					new Subscriber<Void>() {
						@Override
						public void onCompleted() {
							finish(null);
						}

						@Override
						public void onError(Throwable e) {
							finish(e);
						}

						@Override
						public void onNext(Void dummy) {}
					}
				);
		}
	}

	private void finish(Throwable error) {
		final OnInstallServerListener listener;
		synchronized (lock) {
			if (install != null) {
				install.unsubscribe();
				install = null;
			}
			listener = this.listener;
			this.listener = null;
		}
		if (listener != null) {
			listener.OnInstallEnd(error);
		}
	}
	
}
