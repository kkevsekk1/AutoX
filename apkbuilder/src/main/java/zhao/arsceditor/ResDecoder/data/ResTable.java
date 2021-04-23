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

import android.annotation.SuppressLint;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import zhao.arsceditor.AndrolibResources;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResTable {
	private boolean mAnalysisMode = false;

	private final Set<ResPackage> mFramePackages = new LinkedHashSet<ResPackage>();
	private final Set<ResPackage> mMainPackages = new LinkedHashSet<ResPackage>();
	private int mPackageId;

	private String mPackageOriginal;
	private String mPackageRenamed;
	@SuppressLint("UseSparseArrays")
	private final Map<Integer, ResPackage> mPackagesById = new HashMap<Integer, ResPackage>();
	private final Map<String, ResPackage> mPackagesByName = new HashMap<String, ResPackage>();
	private Map<String, String> mSdkInfo = new LinkedHashMap<String, String>();

	private boolean mSharedLibrary = false;
	private Map<String, String> mVersionInfo = new LinkedHashMap<String, String>();

	public ResTable() {
	}

	public ResTable(AndrolibResources andRes) {
	}

	public void addPackage(ResPackage pkg, boolean main) throws IOException {
		Integer id = pkg.getId();
		if (mPackagesById.containsKey(id)) {
			throw new IOException("Multiple packages: id=" + id.toString());
		}
		String name = pkg.getName();
		if (mPackagesByName.containsKey(name)) {
			throw new IOException("Multiple packages: name=" + name);
		}

		mPackagesById.put(id, pkg);
		mPackagesByName.put(name, pkg);
		if (main) {
			mMainPackages.add(pkg);
		} else {
			mFramePackages.add(pkg);
		}
	}

	public void addSdkInfo(String key, String value) {
		mSdkInfo.put(key, value);
	}

	public void addVersionInfo(String key, String value) {
		mVersionInfo.put(key, value);
	}

	public void clearSdkInfo() {
		mSdkInfo.clear();
	}

	public boolean getAnalysisMode() {
		return mAnalysisMode;
	}

	public ResPackage getPackage(int id) throws IOException {
		ResPackage pkg = mPackagesById.get(id);
		if (pkg != null) {
			return pkg;
		}
		/*
		 * if (mAndRes != null) { return mAndRes.loadFrameworkPkg(this, id,
		 * mAndRes.apkOptions.frameworkTag); }
		 */
		throw new IOException(String.format("package: id=%d", id));
	}

	public int getPackageId() {
		return mPackageId;
	}

	public String getPackageOriginal() {
		return mPackageOriginal;
	}

	public String getPackageRenamed() {
		return mPackageRenamed;
	}

	public ResResSpec getResSpec(int resID) throws IOException {
		// The pkgId is 0x00. That means a shared library is using its
		// own resource, so lie to the caller replacing with its own
		// packageId
		if (resID >> 24 == 0) {
			int pkgId = (mPackageId == 0 ? 2 : mPackageId);
			resID = (0xFF000000 & (pkgId << 24)) | resID;
		}
		return getResSpec(new ResID(resID));
	}

	public ResResSpec getResSpec(ResID resID) throws IOException {
		return getPackage(resID.package_).getResSpec(resID);
	}

	public Map<String, String> getSdkInfo() {
		return mSdkInfo;
	}

	public boolean getSharedLibrary() {
		return mSharedLibrary;
	}

	public Map<String, String> getVersionInfo() {
		return mVersionInfo;
	}

	public boolean hasPackage(int id) {
		return mPackagesById.containsKey(id);
	}

	public boolean hasPackage(String name) {
		return mPackagesByName.containsKey(name);
	}

	public Set<ResPackage> listFramePackages() {
		return mFramePackages;
	}

	public Set<ResPackage> listMainPackages() {
		return mMainPackages;
	}

	public void setAnalysisMode(boolean mode) {
		mAnalysisMode = mode;
	}

	public void setPackageId(int id) {
		mPackageId = id;
	}

	public void setPackageOriginal(String pkg) {
		mPackageOriginal = pkg;
	}

	public void setPackageRenamed(String pkg) {
		mPackageRenamed = pkg;
	}

	public void setSharedLibrary(boolean flag) {
		mSharedLibrary = flag;
	}
}
