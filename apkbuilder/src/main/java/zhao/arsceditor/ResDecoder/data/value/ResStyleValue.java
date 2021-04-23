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

import java.io.IOException;

import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResStyleValue extends ResBagValue implements GetResValues {
	private final Duo<ResReferenceValue, ResScalarValue>[] mItems;

	@SuppressWarnings("unchecked")
	ResStyleValue(ResReferenceValue parent, Duo<Integer, ResScalarValue>[] items, ResValueFactory factory) {
		super(parent);

		mItems = new Duo[items.length];
		for (int i = 0; i < items.length; i++) {
			mItems[i] = new Duo<ResReferenceValue, ResScalarValue>(factory.newReference(items[i].m1, null),
					items[i].m2);
		}
	}

	@Override
	public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
		for (int i = 0; i < mItems.length; i++) {
			Duo<ResReferenceValue, ResScalarValue> item = mItems[i];
			back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), res.getResSpec().getName(),
					item.m2.encodeResValue());
		}
	}
}
