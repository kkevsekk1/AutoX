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
public class ResIdValue extends ResValue implements GetResValues {
	@Override
	public void getResValues(ARSCCallBack back, ResResource res) throws IOException {
		back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), res.getResSpec().getName(),
				res.getValue().toString());
	}
}
