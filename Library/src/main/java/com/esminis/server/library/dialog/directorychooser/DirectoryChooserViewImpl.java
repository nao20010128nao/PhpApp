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
package com.esminis.server.library.dialog.directorychooser;

import android.content.Context;

import com.esminis.server.library.dialog.pager.DialogPager;

public class DirectoryChooserViewImpl
	extends DialogPager<DirectoryChooserView, DirectoryChooserPresenter>
	implements DirectoryChooserView
{

	public DirectoryChooserViewImpl(Context context, DirectoryChooserPresenter presenter) {
		super(context, presenter);
		setAdapter(new DirectoryChooserAdapter(presenter, this));
	}

	@Override
	public void showDirectoryChooser() {
		setCurrentItem(DirectoryChooserAdapter.PAGE_DIRECTORY_CHOOSER);
	}

	@Override
	public void showDirectoryCreate() {
		setCurrentItem(DirectoryChooserAdapter.PAGE_DIRECTORY_CREATE);
	}

	@Override
	public void onBackPressed() {
		if (getCurrentItem() == 0) {
			super.onBackPressed();
		} else {
			showDirectoryChooser();
		}
	}

}
