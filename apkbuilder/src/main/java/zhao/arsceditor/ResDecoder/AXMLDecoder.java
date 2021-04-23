/**
 *  Copyright 2015 ZhaoHai <2801045898@qq.com>
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
package zhao.arsceditor.ResDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.util.List;

import zhao.arsceditor.ResDecoder.IO.LEDataInputStream;
import zhao.arsceditor.ResDecoder.IO.LEDataOutputStream;

public class AXMLDecoder {

	// AXML文件头
	private static final int AXML_CHUNK_TYPE = 0x00080003;
	// 字符串常量池类
	public StringBlock mTableStrings;
	// 二进制文件输入流
	private final LEDataInputStream mIn;
	// 二进制文件输入流2
	private LEDataInputStream mIn2;
	// chunkSize
	private int chunkSize;
	// AXML文件数据的字节数组
	private static byte[] bytes;

	// 构造函数
	private AXMLDecoder(LEDataInputStream in) {
		this.mIn = in;
	}

	// 读取字符串
	private void readStrings() throws IOException {
		// type
		int type = mIn.readInt();
		// 检查头
		checkChunk(type, AXML_CHUNK_TYPE);
		// Chunk size
		chunkSize = mIn.readInt();
		// 读取字符串常量池
		mTableStrings = StringBlock.read(this.mIn);
	}

	public static AXMLDecoder read(InputStream input) throws IOException {
		AXMLDecoder axml = new AXMLDecoder(new LEDataInputStream(input));
		axml.readStrings();
		bytes = LEDataInputStream.toByteArray(input);
		return axml;
	}

	public void getStrings(List<String> m_strings) throws CharacterCodingException {
		for (int i = 0; i < mTableStrings.getCount(); i++) {
			m_strings.add(mTableStrings.getString(i));
		}
	}

	public void write(List<String> stringlist_src, List<String> stringlist_tar, OutputStream out) throws IOException {
		write(stringlist_src, stringlist_tar, new LEDataOutputStream(out));
	}

	private void write(List<String> stringlist_src, List<String> stringlist_tar, LEDataOutputStream lmOut)
			throws IOException {
		// 排序列表中的字符串，以方便一一写入
		for (int i = 0; i < stringlist_src.size(); i++)
			mTableStrings.sortStringBlock(stringlist_src.get(i), stringlist_tar.get(i));
		// 先将字符串数据写入到一个临时的流中
		ByteArrayOutputStream mStrings = mTableStrings.writeString(mTableStrings.getList());
		// 写入文件头
		lmOut.writeInt(AXML_CHUNK_TYPE);
		// 写入chunkSize
		lmOut.writeInt(chunkSize + (mStrings.size() - mTableStrings.m_strings.length));
		// 写入字符串常量池
		mTableStrings.writeFully(lmOut, mStrings);
		// 创建二进制文件输入流对象2，用来写入余下的内容
		mIn2 = new LEDataInputStream(new ByteArrayInputStream(bytes));
		// 写入剩下的数据
		int num;
		while ((num = mIn2.readByte()) != -1)
			lmOut.writeByte((byte) num);
	}

	private void checkChunk(int type, int expectedType) throws IOException {
		if (type != expectedType)
			throw new IOException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x",
					new Object[] { Integer.valueOf(expectedType), Short.valueOf((short) type) }));
	}

}
