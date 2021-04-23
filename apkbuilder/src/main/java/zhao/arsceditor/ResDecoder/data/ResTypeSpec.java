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

package zhao.arsceditor.ResDecoder.data;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public final class ResTypeSpec {
	private final int mEntryCount;
	private final byte mId;

	private final String mName;
	private final Map<String, ResResSpec> mResSpecs = new LinkedHashMap<String, ResResSpec>();

	public ResTypeSpec(String name, ResTable resTable, ResPackage package_, byte id, int entryCount) {
		this.mName = name;
		this.mId = id;
		this.mEntryCount = entryCount;
	}

	public void addResSpec(ResResSpec spec) throws IOException {
		if (mResSpecs.put(spec.getName(), spec) != null) {
			// throw new IOException(String.format("Multiple res specs: %s/%s",
			// getName(), spec.getName()));
		}
	}

	public int getEntryCount() {
		return mEntryCount;
	}

	public byte getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public ResResSpec getResSpec(String name) throws IOException {
		ResResSpec spec = mResSpecs.get(name);
		if (spec == null) {
			// throw new UndefinedResObject(String.format("resource spec:
			// %s/%s", getName(), name));
		}
		return spec;
	}

	public boolean isString() {
		return mName.equalsIgnoreCase("string");
	}

	public Set<ResResSpec> listResSpecs() {
		return new LinkedHashSet<ResResSpec>(mResSpecs.values());
	}

	public void removeResSpec(ResResSpec spec) throws IOException {
		mResSpecs.remove(spec.getName());
	}

	@Override
	public String toString() {
		return mName;
	}
}
