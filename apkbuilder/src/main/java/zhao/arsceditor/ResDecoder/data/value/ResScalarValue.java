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
import zhao.arsceditor.ResDecoder.data.ResResource;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public abstract class ResScalarValue extends ResIntBasedValue implements GetResValues {
	protected final String mRawValue;
	protected final String mType;

	protected ResScalarValue(String type, int rawIntValue, String rawValue) {
		super(rawIntValue);
		mType = type;
		mRawValue = rawValue;
	}

	protected abstract String encodeAsResValue() throws IOException;

	public String encodeAsResXmlItemValue() throws IOException {
		return encodeResValue();
	}

	public String encodeResValue() throws IOException {
		if (mRawValue != null) {
			return mRawValue;
		}
		return encodeAsResValue();
	}

	@Override
	public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
		String type = res.getResSpec().getType().getName();

		String body = encodeAsResValue();
		back.back(res.getConfig().toString(), type, res.getResSpec().getName(), body);
	}

	public String getType() {
		return mType;
	}
}
