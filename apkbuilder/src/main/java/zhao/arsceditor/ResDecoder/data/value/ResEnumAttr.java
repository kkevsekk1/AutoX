/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package zhao.arsceditor.ResDecoder.data.value;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResEnumAttr extends ResAttr {
	private final Duo<ResReferenceValue, ResIntValue>[] mItems;

	@SuppressLint("UseSparseArrays")
	private final Map<Integer, String> mItemsCache = new HashMap<Integer, String>();

	ResEnumAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n,
                Duo<ResReferenceValue, ResIntValue>[] items) {
		super(parent, type, min, max, l10n);
		mItems = items;
	}

	@Override
	public String convertToResXmlFormat(ResScalarValue value) throws IOException {
		if (value instanceof ResIntValue) {
			String ret = String.valueOf(value);
			if (ret != null) {
				return ret;
			}
		}
		return super.convertToResXmlFormat(value);
	}

	@Override
	protected void serializeBody(ARSCCallBack back, ResResource res) throws IOException, IOException {
		for (Duo<ResReferenceValue, ResIntValue> duo : mItems) {
			int intVal = duo.m2.getValue();
			back.back(res.getConfig().toString(), "enum", res.getResSpec().getName(), String.valueOf(intVal));
		}
	}
}
