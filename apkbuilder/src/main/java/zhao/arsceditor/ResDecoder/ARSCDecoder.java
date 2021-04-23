/**
 * Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
 * Modified Copyright 2015 ZhaoHai <2801045898@qq.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zhao.arsceditor.ResDecoder;

import android.util.TypedValue;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.IO.LEDataInputStream;
import zhao.arsceditor.ResDecoder.IO.LEDataOutputStream;
import zhao.arsceditor.ResDecoder.data.ResConfigFlags;
import zhao.arsceditor.ResDecoder.data.ResID;
import zhao.arsceditor.ResDecoder.data.ResPackage;
import zhao.arsceditor.ResDecoder.data.ResResSpec;
import zhao.arsceditor.ResDecoder.data.ResResource;
import zhao.arsceditor.ResDecoder.data.ResTable;
import zhao.arsceditor.ResDecoder.data.ResType;
import zhao.arsceditor.ResDecoder.data.ResTypeSpec;
import zhao.arsceditor.ResDecoder.data.value.ResBagValue;
import zhao.arsceditor.ResDecoder.data.value.ResBoolValue;
import zhao.arsceditor.ResDecoder.data.value.ResFileValue;
import zhao.arsceditor.ResDecoder.data.value.ResScalarValue;
import zhao.arsceditor.ResDecoder.data.value.ResStringValue;
import zhao.arsceditor.ResDecoder.data.value.ResValue;
import zhao.arsceditor.ResDecoder.data.value.ResValueFactory;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 * @author ZhaoHai
 */
public class ARSCDecoder {

    public static class ARSCData {

        private final ResPackage[] mPackages;

        private final ResTable mResTable;

        public ARSCData(ResPackage[] packages, ResTable resTable) {
            mPackages = packages;
            mResTable = resTable;
        }

        public int findPackageWithMostResSpecs() {
            int count = -1;
            int id = 0;

            // set starting point to package id 0.
            count = mPackages[0].getResSpecCount();

            // loop through packages looking for largest
            for (int i = 0; i < mPackages.length; i++) {
                if (mPackages[i].getResSpecCount() >= count) {
                    count = mPackages[i].getResSpecCount();
                    id = i;
                }
            }

            return id;
        }

        public ResPackage getOnePackage() throws IOException {
            if (mPackages.length <= 0) {
                throw new IOException("Arsc file contains zero packages");
            } else if (mPackages.length != 1) {
                int id = findPackageWithMostResSpecs();
                LOGGER.info("Arsc file contains multiple packages. Using package " + mPackages[id].getName()
                        + " as default.");

                return mPackages[id];
            }
            return mPackages[0];
        }

        public ResPackage[] getPackages() {
            return mPackages;
        }

        public ResTable getResTable() {
            return mResTable;
        }
    }

    public static class Header {
        public final static short TYPE_NONE = -1, TYPE_TABLE = 0x0002, TYPE_PACKAGE = 0x0200, TYPE_TYPE = 0x0201,
                TYPE_SPEC_TYPE = 0x0202, TYPE_LIBRARY = 0x0203;

        public static Header read(LEDataInputStream in) throws IOException {

            short type;
            try {
                type = in.readShort();
            } catch (EOFException ex) {
                return new Header(TYPE_NONE, 0, (byte) 0, (byte) 0);
            }
            byte byte1 = in.readByte();
            byte byte2 = in.readByte();
            int chunkSize = in.readInt();
            return new Header(type, chunkSize, byte1, byte2);
        }

        // 未知字节
        public final byte byte1;
        // 未知字节
        public final byte byte2;

        // chunkSize
        public final int chunkSize;

        // 资源类型
        public final short type;

        public Header(short type, int size, byte byte1, byte byte2) {
            this.type = type;
            this.chunkSize = size;
            this.byte1 = byte1;
            this.byte2 = byte2;
        }
    }

    private final static short ENTRY_FLAG_COMPLEX = 0x0001;

    private static final int KNOWN_CONFIG_BYTES = 52;

    private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class.getName());

    private Header mHeader;

    // 二进制文件输入流
    private LEDataInputStream mIn;

    private final boolean mKeepBroken;

    private boolean[] mMissingResSpecs;

    // 包
    public ResPackage mPkg;

    private int mResId;

    // ResTable
    private final ResTable mResTable;

    private HashMap<Byte, ResTypeSpec> mResTypeSpecs = new HashMap<>();

    // 资源id常量池
    private StringBlock mSpecNames;

    // 字符串常量池
    public StringBlock mTableStrings;

    // 资源类型
    private ResType mType;

    // 资源类型常量池
    private StringBlock mTypeNames;

    private ResTypeSpec mTypeSpec;

    // 包的数量
    private int packageCount;

    // 包名
    private String packageName;

    // 文件输入流的大小
    private int size1;

    // 除去字符串常量池后，文件输入流的大小
    private int size2;

    // 字符串常量池头
    private Header stringsHeader;

    /**
     * 构造函数 arscStream arsc文件输入流 resTable 资源结构表对象 keepBroken 是否保留损坏的内容
     */
    public ARSCDecoder(InputStream arscStream, ResTable resTable, boolean keepBroken) throws IOException {
        // 创建二进制文件输入流对象
        mIn = new LEDataInputStream(arscStream);
        mResTable = resTable;
        mKeepBroken = keepBroken;

    }

    private void addMissingResSpecs() throws IOException {
        int resId = mResId & 0xffff0000;

        for (int i = 0; i < mMissingResSpecs.length; i++) {
            if (!mMissingResSpecs[i]) {
                continue;
            }

            ResResSpec spec = new ResResSpec(new ResID(resId | i), String.format("APKTOOL_DUMMY_%04x", i), mPkg,
                    mTypeSpec);

            // If we already have this resID dont add it again.
            if (!mPkg.hasResSpec(new ResID(resId | i))) {
                mPkg.addResSpec(spec);
                mTypeSpec.addResSpec(spec);

                if (mType == null) {
                    mType = mPkg.getOrCreateConfig(new ResConfigFlags());
                }

                ResValue value = new ResBoolValue(false, 0, null);
                ResResource res = new ResResource(mType, spec, value);

                mPkg.addResource(res);
                mType.addResource(res);
                spec.addResource(res);
            }
        }
    }

    private void addTypeSpec(ResTypeSpec resTypeSpec) {
        mResTypeSpecs.put(resTypeSpec.getId(), resTypeSpec);
    }

    private void checkChunkType(int expectedType) throws IOException {
        if (mHeader.type != expectedType) {
            /*
			 * throw new IOException(String.format(
			 * "Invalid chunk type: expected=0x%08x, got=0x%08x", expectedType,
			 * mHeader.type));
			 */
        }
    }

    /**
     * 制作arsc共存 实际是修改一下包名 os 文件输出流 rndChar 随机英文字母
     */
    public void CloneArsc(OutputStream os, String newPackageName, boolean close) throws IOException {
        int size1 = mIn.available();
        if (size1 == 1) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            byte buffer[] = new byte[2048];
            int count;
            while ((count = mIn.read(buffer, 0, 2048)) != -1) {
                bOut.write(buffer, 0, count);
            }
            bOut.close();
            ByteArrayInputStream bIn = new ByteArrayInputStream(bOut.toByteArray());
            mIn = new LEDataInputStream(new BufferedInputStream(bIn));
            size1 = mIn.available();
        }
        mIn.mark(size1);
        // 字符串常量池头
        nextChunk();
        checkChunkType(Header.TYPE_TABLE);
        // 包的数量
        mIn.skipInt();
        // 字符串常量池的字符串
        StringBlock.read(mIn);
        // 检查下一个区的头
        nextChunk();
        // 检查头
        checkChunkType(Header.TYPE_PACKAGE);
        // id
        mIn.skipInt();
        int size2 = mIn.available();
        // 包名
        packageName = mIn.readNulEndedString(128, true);
        // 重新读取流
        mIn.reset();
        // 二进制文件输出流
        LEDataOutputStream lmOut = new LEDataOutputStream(os);
        for (int i = 0; i < size1 - size2; i++)
            lmOut.writeByte(mIn.readByte());
        // 读取掉包名
        mIn.readNulEndedString(128, true);
        // 修改包名
        lmOut.writeNulEndedString(newPackageName);
        byte[] buffer = new byte[1024];
        int count;
        // 将剩余内容写入文件
        while ((count = mIn.read(buffer, 0, buffer.length)) != -1) {
            lmOut.writeFully(buffer, 0, count);
        }
        if (close)
            lmOut.close();
    }

    public ARSCData decode(ARSCDecoder decoder, InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken)
            throws IOException {
        return decode(decoder, arscStream, findFlagsOffsets, keepBroken, new ResTable());
    }

    public ARSCData decode(ARSCDecoder decoder, InputStream arscStream, boolean findFlagsOffsets, boolean keepBroken,
                           ResTable resTable) throws IOException {
        ResPackage[] pkgs = decoder.readTable();

        return new ARSCData(pkgs, resTable);
    }

    /**
     * 获取包名的方法
     **/
    public String getPackageName() {
        return packageName;
    }

    private Header nextChunk() throws IOException {
        return mHeader = Header.read(mIn);
    }

    private ResBagValue readComplexEntry() throws IOException {
        int parent = mIn.readInt();
        int count = mIn.readInt();

        ResValueFactory factory = mPkg.getValueFactory();
        @SuppressWarnings("unchecked")
        Duo<Integer, ResScalarValue>[] items = new Duo[count];
        for (int i = 0; i < count; i++) {
            items[i] = new Duo<Integer, ResScalarValue>(mIn.readInt(), (ResScalarValue) readValue());
        }

        return factory.bagFactory(parent, items);
    }

    private ResConfigFlags readConfigFlags() throws IOException {
        int size = mIn.readInt();
        int read = 28;

        if (size < 28) {
            throw new IOException("Config size < 28");
        }

        boolean isInvalid = false;

        short mcc = mIn.readShort();
        short mnc = mIn.readShort();

        char[] language = this.unpackLanguageOrRegion(mIn.readByte(), mIn.readByte(), 'a');
        char[] country = this.unpackLanguageOrRegion(mIn.readByte(), mIn.readByte(), '0');

        byte orientation = mIn.readByte();
        byte touchscreen = mIn.readByte();

        int density = mIn.readShort();

        byte keyboard = mIn.readByte();
        byte navigation = mIn.readByte();
        byte inputFlags = mIn.readByte();
		/* inputPad0 */
        mIn.skipBytes(1);

        short screenWidth = mIn.readShort();
        short screenHeight = mIn.readShort();

        short sdkVersion = mIn.readShort();
		/* minorVersion, now must always be 0 */
        mIn.skipBytes(2);

        byte screenLayout = 0;
        byte uiMode = 0;
        short smallestScreenWidthDp = 0;
        if (size >= 32) {
            screenLayout = mIn.readByte();
            uiMode = mIn.readByte();
            smallestScreenWidthDp = mIn.readShort();
            read = 32;
        }

        short screenWidthDp = 0;
        short screenHeightDp = 0;
        if (size >= 36) {
            screenWidthDp = mIn.readShort();
            screenHeightDp = mIn.readShort();
            read = 36;
        }

        char[] localeScript = null;
        char[] localeVariant = null;
        if (size >= 48) {
            localeScript = readScriptOrVariantChar(4).toCharArray();
            localeVariant = readScriptOrVariantChar(8).toCharArray();
            read = 48;
        }

        byte screenLayout2 = 0;
        if (size >= 52) {
            screenLayout2 = mIn.readByte();
            mIn.skipBytes(3); // reserved padding
            read = 52;
        }

        int exceedingSize = size - KNOWN_CONFIG_BYTES;
        if (exceedingSize > 0) {
            byte[] buf = new byte[exceedingSize];
            read += exceedingSize;
            mIn.readFully(buf);
            BigInteger exceedingBI = new BigInteger(1, buf);

            if (exceedingBI.equals(BigInteger.ZERO)) {
                LOGGER.fine(
                        String.format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.",
                                KNOWN_CONFIG_BYTES));
            } else {
                LOGGER.warning(String.format("Config flags size > %d. Exceeding bytes: 0x%X.", KNOWN_CONFIG_BYTES,
                        exceedingBI));
                isInvalid = true;
            }
        }

        int remainingSize = size - read;
        if (remainingSize > 0) {
            mIn.skipBytes(remainingSize);
        }

        return new ResConfigFlags(mcc, mnc, language, country, orientation, touchscreen, density, keyboard, navigation,
                inputFlags, screenWidth, screenHeight, sdkVersion, screenLayout, uiMode, smallestScreenWidthDp,
                screenWidthDp, screenHeightDp, localeScript, localeVariant, screenLayout2, isInvalid, size);
    }

    private void readEntry() throws IOException {
		/* size */
        mIn.skipBytes(2);
        short flags = mIn.readShort();
        int specNamesId = mIn.readInt();

        ResValue value = (flags & ENTRY_FLAG_COMPLEX) == 0 ? readValue() : readComplexEntry();

        if (mTypeSpec.isString() && value instanceof ResFileValue) {
            value = new ResStringValue(value.toString(), ((ResFileValue) value).getRawIntValue());
        }
        if (mType == null) {
            return;
        }

        ResID resId = new ResID(mResId);
        ResResSpec spec;
        if (mPkg.hasResSpec(resId)) {
            spec = mPkg.getResSpec(resId);

            if (spec.isDummyResSpec()) {
                removeResSpec(spec);

                spec = new ResResSpec(resId, mSpecNames.getString(specNamesId), mPkg, mTypeSpec);
                mPkg.addResSpec(spec);
                mTypeSpec.addResSpec(spec);
            }
        } else {
            spec = new ResResSpec(resId, mSpecNames.getString(specNamesId), mPkg, mTypeSpec);
            mPkg.addResSpec(spec);
            mTypeSpec.addResSpec(spec);
        }
        ResResource res = new ResResource(mType, spec, value);

        mType.addResource(res);
        spec.addResource(res);
        mPkg.addResource(res);
    }

    private void readLibraryType() throws IOException {
        checkChunkType(Header.TYPE_LIBRARY);
        int libraryCount = mIn.readInt();

        int packageId;
        String packageName;

        for (int i = 0; i < libraryCount; i++) {
            packageId = mIn.readInt();
            packageName = mIn.readNulEndedString(128, true);
            LOGGER.info(String.format("Decoding Shared Library (%s), pkgId: %d", packageName, packageId));
        }

        while (nextChunk().type == Header.TYPE_TYPE) {
            readTableTypeSpec();
        }
    }

    // 读取包
    private ResPackage readPackage() throws IOException {
        // 检查头
        checkChunkType(Header.TYPE_PACKAGE);
        // id
        int id = (byte) mIn.readInt();

        if (id == 0) {
            // 这意味着我们正在处理一个库，我们应该暂时的
            // 设置到下一个可用的ID packageid。这将在运行时被设置，但是
            // APKTOOL的使用需要一个非零的packageid。
            // AOSP表明常规下，为0x01是系统的，0x7f是私人的。
            id = 2;
            if (mResTable.getPackageOriginal() == null && mResTable.getPackageRenamed() == null) {
                mResTable.setSharedLibrary(true);
            }
        }
        // 包名
        packageName = mIn.readNulEndedString(128, true);
		/* typeNameStrings */
        mIn.skipInt();
		/* typeNameCount */
        mIn.skipInt();
		/* specNameStrings */
        mIn.skipInt();
		/* specNameCount */
        mIn.skipInt();

        // 读取资源类型常量池
        mTypeNames = StringBlock.read(mIn);
        // 读取资源对应的id常量池
        mSpecNames = StringBlock.read(mIn);

        mResId = id << 24;
        // 创建包对象
        mPkg = new ResPackage(mResTable, id, packageName);
        // 检查下一个头
        nextChunk();
        // 如果检查到类型是库
        while (mHeader.type == Header.TYPE_LIBRARY) {
            // 读取库
            readLibraryType();
        }

        //
        while (mHeader.type == Header.TYPE_SPEC_TYPE) {
            readTableTypeSpec();
        }

        return mPkg;
    }

    private String readScriptOrVariantChar(int length) throws IOException {
        StringBuilder string = new StringBuilder(16);

        while (length-- != 0) {
            byte ch = mIn.readByte();
            if (ch == 0) {
                break;
            }
            string.append((char) ch);
        }
        mIn.skipBytes(length);

        return string.toString();
    }

    private ResTypeSpec readSingleTableTypeSpec() throws IOException {
        checkChunkType(Header.TYPE_TYPE);
        byte id = mIn.readByte();
        mIn.skipBytes(3);
        int entryCount = mIn.readInt();

		/* flags */
        mIn.skipBytes(entryCount * 4);
        mTypeSpec = new ResTypeSpec(mTypeNames.getString(id - 1), mResTable, mPkg, id, entryCount);
        mPkg.addType(mTypeSpec);
        return mTypeSpec;
    }

    public ResPackage[] readTable() throws IOException {
        // 获取文件输入流的大小
        size1 = mIn.available();
        if (size1 == 1) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            byte buffer[] = new byte[2048];
            int count;
            while ((count = mIn.read(buffer, 0, 2048)) != -1) {
                bOut.write(buffer, 0, count);
            }
            bOut.close();
            ByteArrayInputStream bIn = new ByteArrayInputStream(bOut.toByteArray());
            mIn = new LEDataInputStream(new BufferedInputStream(bIn));
            size1 = mIn.available();
        }
        System.out.println("size1==" + size1);
        mIn.mark(size1);
        // 获取字符串常量池头
        stringsHeader = nextChunk();
        // 检查头
        checkChunkType(Header.TYPE_TABLE);
        // 获取包的数量
        packageCount = mIn.readInt();
        // 读取字符串常量池的字符串
        mTableStrings = StringBlock.read(mIn);
        // 获取除去字符串常量池后的文件输入流的大小
        size2 = mIn.available();
        // System.out.println("size2==" + size2);
        // 创建包数组对象
        ResPackage[] packages = new ResPackage[packageCount];
        // 检查下一个区的头
        nextChunk();

        // 获取包
        for (int i = 0; i < packageCount; i++) {
            packages[i] = readPackage();
        }
        // 返回包数组对象
        return packages;
    }

    private ResType readTableType() throws IOException {
        checkChunkType(Header.TYPE_TYPE);
        byte typeId = mIn.readByte();
        if (mResTypeSpecs.containsKey(typeId)) {
            mResId = (0xff000000 & mResId) | mResTypeSpecs.get(typeId).getId() << 16;
            mTypeSpec = mResTypeSpecs.get(typeId);
        }

		/* res0, res1 */
        mIn.skipBytes(3);
        int entryCount = mIn.readInt();
		/* entriesStart */
        mIn.skipInt();

        mMissingResSpecs = new boolean[entryCount];
        Arrays.fill(mMissingResSpecs, true);

        ResConfigFlags flags = readConfigFlags();
        int[] entryOffsets = mIn.readIntArray(entryCount);

        if (flags.isInvalid) {
            String resName = mTypeSpec.getName() + flags.getQualifiers();
            if (mKeepBroken) {
                LOGGER.warning("Invalid config flags detected: " + resName);
            } else {
                LOGGER.warning("Invalid config flags detected. Dropping resources: " + resName);
            }
        }

        mType = flags.isInvalid && !mKeepBroken ? null : mPkg.getOrCreateConfig(flags);

        for (int i = 0; i < entryOffsets.length; i++) {
            if (entryOffsets[i] != -1) {
                mMissingResSpecs[i] = false;
                mResId = (mResId & 0xffff0000) | i;
                readEntry();
            }
        }

        return mType;
    }

    private ResType readTableTypeSpec() throws IOException {
        mTypeSpec = readSingleTableTypeSpec();
        addTypeSpec(mTypeSpec);

        int type = nextChunk().type;
        ResTypeSpec resTypeSpec;

        while (type == Header.TYPE_SPEC_TYPE) {
            resTypeSpec = readSingleTableTypeSpec();
            addTypeSpec(resTypeSpec);
            type = nextChunk().type;
        }

        while (type == Header.TYPE_TYPE) {
            readTableType();
            type = nextChunk().type;

            addMissingResSpecs();
        }

        return mType;
    }

    private ResValue readValue() throws IOException {
		/* size */
        mIn.skipCheckShort((short) 8);
		/* zero */
        mIn.skipCheckByte((byte) 0);
        byte type = mIn.readByte();
        int data = mIn.readInt();

        return type == TypedValue.TYPE_STRING ? mPkg.getValueFactory().factory(mTableStrings.getString(data), data)
                : mPkg.getValueFactory().factory(type, data, null);
    }

    private void removeResSpec(ResResSpec spec) throws IOException {
        if (mPkg.hasResSpec(spec.getId())) {
            mPkg.removeResSpec(spec);
            mTypeSpec.removeResSpec(spec);
        }
    }

    private char[] unpackLanguageOrRegion(byte in0, byte in1, char base) throws IOException {
        // check high bit, if so we have a packed 3 letter code
        if (((in0 >> 7) & 1) == 1) {
            int first = in1 & 0x1F;
            int second = ((in1 & 0xE0) >> 5) + ((in0 & 0x03) << 3);
            int third = (in0 & 0x7C) >> 2;

            // since this function handles languages & regions, we add the
            // value(s) to the base char
            // which is usually 'a' or '0' depending on language or region.
            return new char[]{(char) (first + base), (char) (second + base), (char) (third + base)};
        }
        return new char[]{(char) in0, (char) in1};
    }

    /***
     * 回写ARSC文件
     *
     * @throws IOException
     */
    public void write(OutputStream os,InputStream in) throws IOException {
        // 二进制文件输出流
        LEDataOutputStream lmOut = new LEDataOutputStream(os);
        // 先将字符串数据写入到一个临时的流中
        ByteArrayOutputStream mStrings = mTableStrings.writeString(mTableStrings.getList());
        /////////////////////////////////////////////////////////////////////////////////////////
        // 这里才正式开始写arsc文件
        // 写入一个short型数据，标识着该文件的种类
        lmOut.writeShort(stringsHeader.type);
        // 写入两个未知字节
        lmOut.writeByte(stringsHeader.byte1);
        lmOut.writeByte(stringsHeader.byte2);
        // 写入chunkSize
        lmOut.writeInt(stringsHeader.chunkSize + (mStrings.size() - mTableStrings.m_strings.length));
        // 写入包的数量
        lmOut.writeInt(packageCount);
        // 写入字符串
        mTableStrings.writeFully(lmOut, mStrings);
        // 二进制输入流跳过size1-size2 个字节，目的是我们只需修改前面的包含有字符串的数据，而后面的数据，则从文件中直接复制
        LEDataInputStream lein = new LEDataInputStream(in);
        lein.skipBytes(size1 - size2);
        byte[] buffer = new byte[1024];
        int count;
        // 将剩余内容写入文件
        while ((count = lein.read(buffer, 0, buffer.length)) != -1) {
            lmOut.writeFully(buffer, 0, count);
        }
        lmOut.close();
        lein.close();
    }

    /**
     * 写出arsc文件的方法 os 文件输出流 stringlist_src 未修改之前的字符串列表集合 stringlist_tar
     * 修改后的字符串列表集合
     ***/
    public void write(OutputStream os, InputStream in, List<String> stringlist_src, List<String> stringlist_tar) throws IOException {

        int index = 0;
        // 排序列表中的字符串，以方便一一写入
        for (String str : stringlist_src) {
            String tar = stringlist_tar.get(index);
            mTableStrings.sortStringBlock(str, tar);
            index++;
        }
        write(os,in);
    }
}
