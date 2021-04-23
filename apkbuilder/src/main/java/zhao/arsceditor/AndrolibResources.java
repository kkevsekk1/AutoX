/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Modified Copyright 2015 ZhaoHai <2801045898@qq.com>
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

package zhao.arsceditor;

import android.annotation.SuppressLint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.ARSCDecoder;
import zhao.arsceditor.ResDecoder.AXMLDecoder;
import zhao.arsceditor.ResDecoder.GetResValues;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResTable;
import zhao.arsceditor.ResDecoder.data.ResValuesFile;

final public class AndrolibResources {

	// TODO: dirty static hack. I have to refactor decoding mechanisms.
	public static boolean sKeepBroken = true;


	// ARSC解析器
	public ARSCDecoder mARSCDecoder;

	// AXML解析器
	public AXMLDecoder mAXMLDecoder;

	// ELF解析器
	//public Elf elfParser;

	private ResPackage pkg;

	public AndrolibResources() {}

	// 解析ARSC的方法
	public void decodeARSC(ResTable resTable, ARSCCallBack callback) throws IOException {

		for (ResPackage pkg : resTable.listMainPackages()) {

			System.out.println("Decoding values */* XMLs...");
			for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
				generateValuesFile(valuesFile, callback);
			}
		}
	}

	/**
	 * 解析AXML的方法
	 *
	 * @param AXMLStream
	 * @return 返回一个解析后的临时文件对象
	 * @throws IOException
	 */
	public void decodeAXML(InputStream AXMLStream) throws IOException {
		mAXMLDecoder = AXMLDecoder.read(AXMLStream);
	}

	@SuppressLint("NewApi")
	private void generateValuesFile(ResValuesFile valuesFile, ARSCCallBack callback) throws IOException {
		for (ResResource res : valuesFile.listResources()) {
			if (valuesFile.isSynthesized(res)) {
				continue;
			}
			((GetResValues) res.getValue()).getResValues(callback, res);
		}
	}

	public ResPackage getFramPackage() {
		return pkg;
	}

	private ResPackage[] getResPackagesFromARSC(ARSCDecoder decoder, InputStream ARSCStream, ResTable resTable,
                                                boolean keepBroken) throws IOException {
		return decoder.decode(decoder, new BufferedInputStream(ARSCStream), false, keepBroken, resTable).getPackages();
	}

	public ResTable getResTable() {
		ResTable resTable = new ResTable(this);
		return resTable;
	}

	public ResTable getResTable(InputStream ARSCStream) throws IOException {
		return getResTable(ARSCStream, true);
	}

	public ResTable getResTable(InputStream ARSCStream, boolean loadMainPkg) throws IOException {
		ResTable resTable = new ResTable(this);
		if (loadMainPkg) {
			loadMainPkg(resTable, ARSCStream);
		}
		return resTable;
	}

	public ResPackage loadMainPkg(ResTable resTable, InputStream ARSCStream) throws IOException {
		System.out.println("Loading resource table...");
		mARSCDecoder = new ARSCDecoder(new BufferedInputStream(ARSCStream), resTable, sKeepBroken);
		ResPackage[] pkgs = getResPackagesFromARSC(mARSCDecoder, ARSCStream, resTable, sKeepBroken);
		ResPackage pkg = null;

		switch (pkgs.length) {
		case 1:
			pkg = pkgs[0];
			break;
		case 2:
			if (pkgs[0].getName().equals("android")) {
				System.out.println("Skipping \"android\" package group");
				pkg = pkgs[1];
			} else if (pkgs[0].getName().equals("com.htc")) {
				System.out.println("Skipping \"htc\" package group");
				pkg = pkgs[1];
			}
			break;
		default:
			pkg = selectPkgWithMostResSpecs(pkgs);
		}

		if (pkg == null) {
			throw new IOException("Arsc files with zero or multiple packages");
		}

		resTable.addPackage(pkg, true);
		System.out.println("Loaded.");
		return pkg;
	}

	public void pushFramResPackage(ResPackage pkg) {
		this.pkg = pkg;
	}

	public ResPackage selectPkgWithMostResSpecs(ResPackage[] pkgs) throws IOException {
		int id = 0;
		int value = 0;

		for (ResPackage resPackage : pkgs) {
			if (resPackage.getResSpecCount() > value && !resPackage.getName().equalsIgnoreCase("android")) {
				value = resPackage.getResSpecCount();
				id = resPackage.getId();
			}
		}

		// if id is still 0, we only have one pkgId which is "android" -> 1
		return (id == 0) ? pkgs[0] : pkgs[1];
	}
}
