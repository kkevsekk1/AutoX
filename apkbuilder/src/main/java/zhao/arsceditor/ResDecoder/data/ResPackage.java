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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.value.ResFileValue;
import zhao.arsceditor.ResDecoder.data.value.ResValueFactory;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResPackage {
	private final static Logger LOGGER = Logger.getLogger(ResPackage.class.getName());
	private final Map<ResConfigFlags, ResType> mConfigs = new LinkedHashMap<ResConfigFlags, ResType>();
	private final int mId;
	private final String mName;
	private final Map<ResID, ResResSpec> mResSpecs = new LinkedHashMap<ResID, ResResSpec>();
	private final ResTable mResTable;
	private final Set<ResID> mSynthesizedRes = new HashSet<ResID>();

	private final Map<String, ResTypeSpec> mTypes = new LinkedHashMap<String, ResTypeSpec>();

	private ResValueFactory mValueFactory;

	public ResPackage(ResTable resTable, int id, String name) {
		this.mResTable = resTable;
		this.mId = id;
		this.mName = name;
	}

	public void addConfig(ResType config) throws IOException {
		if (mConfigs.put(config.getFlags(), config) != null) {
			// throw new IOException("Multiple configs: " + config);
		}
	}

	public void addResource(ResResource res) {
	}

	public void addResSpec(ResResSpec spec) throws IOException {
		if (mResSpecs.put(spec.getId(), spec) != null) {
			// throw new IOException("Multiple resource specs: " + spec);
		}
	}

	public void addSynthesizedRes(int resId) {
		mSynthesizedRes.add(new ResID(resId));
	}

	public void addType(ResTypeSpec type) throws IOException {
		if (mTypes.containsKey(type.getName())) {
			LOGGER.warning("Multiple types detected! " + type + " ignored!");
		} else {
			mTypes.put(type.getName(), type);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ResPackage other = (ResPackage) obj;
		if (this.mResTable != other.mResTable && (this.mResTable == null || !this.mResTable.equals(other.mResTable))) {
			return false;
		}
		if (this.mId != other.mId) {
			return false;
		}
		return true;
	}

	public ResType getConfig(ResConfigFlags flags) throws IOException {
		ResType config = mConfigs.get(flags);
		if (config == null) {
			// throw new IOException("config: " + flags);
		}
		return config;
	}

	public List<ResType> getConfigs() {
		return new ArrayList<ResType>(mConfigs.values());
	}

	public int getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public ResType getOrCreateConfig(ResConfigFlags flags) throws IOException {
		ResType config = mConfigs.get(flags);
		if (config == null) {
			config = new ResType(flags);
			mConfigs.put(flags, config);
		}
		return config;
	}

	public ResResSpec getResSpec(ResID resID) throws IOException {
		ResResSpec spec = mResSpecs.get(resID);
		if (spec == null) {
			// throw new IOException("resource spec: " + resID.toString());
		}
		return spec;
	}

	public int getResSpecCount() {
		return mResSpecs.size();
	}

	public ResTable getResTable() {
		return mResTable;
	}

	public ResTypeSpec getType(String typeName) throws IOException {
		ResTypeSpec type = mTypes.get(typeName);
		if (type == null) {
			// throw new IOException("type: " + typeName);
		}
		return type;
	}

	public ResValueFactory getValueFactory() {
		if (mValueFactory == null) {
			mValueFactory = new ResValueFactory(this);
		}
		return mValueFactory;
	}

	public boolean hasConfig(ResConfigFlags flags) {
		return mConfigs.containsKey(flags);
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31 * hash + (this.mResTable != null ? this.mResTable.hashCode() : 0);
		hash = 31 * hash + this.mId;
		return hash;
	}

	public boolean hasResSpec(ResID resID) {
		return mResSpecs.containsKey(resID);
	}

	public boolean hasType(String typeName) {
		return mTypes.containsKey(typeName);
	}

	boolean isSynthesized(ResID resId) {
		return mSynthesizedRes.contains(resId);
	}

	public Set<ResResource> listFiles() {
		Set<ResResource> ret = new HashSet<ResResource>();
		for (ResResSpec spec : mResSpecs.values()) {
			for (ResResource res : spec.listResources()) {
				if (res.getValue() instanceof ResFileValue) {
					ret.add(res);
				}
			}
		}
		return ret;
	}

	public List<ResResSpec> listResSpecs() {
		return new ArrayList<ResResSpec>(mResSpecs.values());
	}

	public List<ResTypeSpec> listTypes() {
		return new ArrayList<ResTypeSpec>(mTypes.values());
	}

	public Collection<ResValuesFile> listValuesFiles() {
		Map<Duo<ResTypeSpec, ResType>, ResValuesFile> ret = new HashMap<Duo<ResTypeSpec, ResType>, ResValuesFile>();
		for (ResResSpec spec : mResSpecs.values()) {
			for (ResResource res : spec.listResources()) {
				if (res.getValue() instanceof GetResValues) {
					ResTypeSpec type = res.getResSpec().getType();
					ResType config = res.getConfig();
					Duo<ResTypeSpec, ResType> key = new Duo<ResTypeSpec, ResType>(type, config);
					ResValuesFile values = ret.get(key);
					if (values == null) {
						values = new ResValuesFile(this, type, config);
						ret.put(key, values);
					}
					values.addResource(res);
				}
			}
		}
		return ret.values();
	}

	public void removeResource(ResResource res) {
	}

	public void removeResSpec(ResResSpec spec) throws IOException {
		mResSpecs.remove(spec.getId());
	}

	@Override
	public String toString() {
		return mName;
	}
}
