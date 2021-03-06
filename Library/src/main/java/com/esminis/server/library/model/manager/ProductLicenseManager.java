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
package com.esminis.server.library.model.manager;

import android.content.Context;

import com.esminis.server.library.model.ProductLicense;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProductLicenseManager {

	private final Context context;
	private ProductLicense[] licenses = null;

	public ProductLicenseManager(Context context) {
		this.context = context;
	}

	public ProductLicense[] getLicenses() {
		if (licenses == null) {
			final List<ProductLicense> licenses = new ArrayList<>();
			try {
				JSONObject json = new JSONObject(
					IOUtils.toString(context.getAssets().open("licenses/licenses.json"))
				);
				Iterator<String> keys = json.keys();
				while (keys.hasNext()) {
					final String file = keys.next();
					final JSONObject licenseData = json.getJSONObject(file);
					final JSONArray items = licenseData.getJSONArray("items");
					for (int i = 0; i < items.length(); i++) {
						licenses.add(
							new ProductLicense(items.getString(i), licenseData.getString("title"), file)
						);
					}
				}
			} catch (IOException | JSONException ignored) {}
			this.licenses = licenses.toArray(new ProductLicense[licenses.size()]);
		}
		return licenses;
	}

	public String getProductLicenseContent(final ProductLicense model) {
		try {
			return IOUtils.toString(context.getAssets().open("licenses/" + model.name + ".txt"));
		} catch (IOException ignored) {}
		return null;
	}

}
