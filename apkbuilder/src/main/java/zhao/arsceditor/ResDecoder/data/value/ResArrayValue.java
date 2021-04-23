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
import java.util.Arrays;

import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResArrayValue extends ResBagValue implements GetResValues {
	public static final int BAG_KEY_ARRAY_START = 0x02000000;

	private final String AllowedArrayTypes[] = { "string", "integer" };

	private final ResScalarValue[] mItems;

	ResArrayValue(ResReferenceValue parent, Duo<Integer, ResScalarValue>[] items) {
		super(parent);

		mItems = new ResScalarValue[items.length];
		for (int i = 0; i < items.length; i++) {
			mItems[i] = items[i].m2;
		}
	}

	public ResArrayValue(ResReferenceValue parent, ResScalarValue[] items) {
		super(parent);
		mItems = items;
	}

	@Override
	public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
		String type = getType();
		type = (type == null ? "" : type + "-") + "array";
		// add <item>'s
		for (int i = 0; i < mItems.length; i++) {
			back.back(res.getConfig().toString(), type, res.getResSpec().getName(), mItems[i].encodeAsResValue());
		}
	}

	public String getType() throws IOException {
		if (mItems.length == 0) {
			return null;
		}
		String type = mItems[0].getType();
		for (int i = 0; i < mItems.length; i++) {
			if (mItems[i].encodeAsResXmlItemValue().startsWith("@string")) {
				return "string";
			} else if (mItems[i].encodeAsResXmlItemValue().startsWith("@drawable")) {
				return null;
			} else if (mItems[i].encodeAsResXmlItemValue().startsWith("@integer")) {
				return "integer";
			} else if (!"string".equals(type) && !"integer".equals(type)) {
				return null;
			} else if (!type.equals(mItems[i].getType())) {
				return null;
			}
		}
		if (!Arrays.asList(AllowedArrayTypes).contains(type)) {
			return "string";
		}
		return type;
	}
}
