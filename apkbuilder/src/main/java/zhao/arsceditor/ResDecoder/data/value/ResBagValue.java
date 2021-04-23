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
public class ResBagValue extends ResValue implements GetResValues {
	protected final ResReferenceValue mParent;

	public ResBagValue(ResReferenceValue parent) {
		this.mParent = parent;
	}

	public ResReferenceValue getParent() {
		return mParent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
		String type = res.getResSpec().getType().getName();
		if ("style".equals(type)) {
			new ResStyleValue(mParent, new Duo[0], null).getResValues(back, res);
			return;
		}
		if ("array".equals(type)) {
			new ResArrayValue(mParent, new Duo[0]).getResValues(back, res);
			return;
		}
	}
}
