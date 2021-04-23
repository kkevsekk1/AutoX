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
/*******************************************************************************
 *  @(#)LEDataOutputStream.java
 * @author zhaohai
 * 二进制文件输出处理工具类
 * 2015.10
 */

package zhao.arsceditor.ResDecoder.IO;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class LEDataOutputStream {

	/** 二进制文件输出流 */
	private DataOutputStream dos;

	/** 构造函数 */
	public LEDataOutputStream(OutputStream out) {
		dos = new DataOutputStream(out);
	}

	/**
	 * 关闭流
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		dos.flush();
		dos.close();
	}

	/**
	 * 获取流的大小
	 * 
	 * @return
	 */
	public int size() {
		return dos.size();
	}

	/**
	 * 写入一个字节
	 * 
	 * @param b
	 * @throws IOException
	 */
	public void writeByte(byte b) throws IOException {
		dos.writeByte(b);
	}

	/**
	 * 写入length个空字节
	 * 
	 * @param length
	 * @throws IOException
	 */
	public void writeBytes(int length) throws IOException {
		for (int i = 0; i < length; i++)
			dos.writeByte(0);
	}

	/**
	 * 写入字符数组
	 * 
	 * @param charbuf
	 * @throws IOException
	 */
	public void writeCharArray(char[] charbuf) throws IOException {
		int length = charbuf.length;
		for (int i = 0; i < length; i++)
			writeShort((short) charbuf[i]);
	}

	/**
	 * 写入字节数组
	 * 
	 * @param b
	 * @throws IOException
	 */
	public void writeFully(byte[] b) throws IOException {
		dos.write(b, 0, b.length);
	}

	/**
	 * 写入字节数组
	 * 
	 * @param buffer
	 * @param offset
	 * @param count
	 * @throws IOException
	 */
	public void writeFully(byte[] buffer, int offset, int count) throws IOException {
		dos.write(buffer, offset, count);
	}

	/**
	 * 写入一个32位的int型数据
	 * 
	 * @param i
	 * @throws IOException
	 */
	public void writeInt(int i) throws IOException {
		dos.writeByte(i & 0xff);
		dos.writeByte((i >> 8) & 0xff);
		dos.writeByte((i >> 16) & 0xff);
		dos.writeByte((i >> 24) & 0xff);
	}

	/**
	 * 写入32位的int型数组
	 * 
	 * @param buf
	 * @throws IOException
	 */
	public void writeIntArray(int[] buf) throws IOException {
		writeIntArray(buf, 0, buf.length);
	}

	/**
	 * 写入32位的int型数组
	 * 
	 * @param buf
	 * @param s
	 * @param end
	 * @throws IOException
	 */
	private void writeIntArray(int[] buf, int s, int end) throws IOException {
		for (; s < end; s++)
			writeInt(buf[s]);
	}

	/**
	 * 写入包名
	 * 
	 * @param name
	 * @throws IOException
	 */
	public void writeNulEndedString(String name) throws IOException {
		char[] ch = name.toCharArray();
		int length = ch.length;

		for (int i = 0; i < length; i++)
			writeShort((short) ch[i]);

		writeBytes(128 * 2 - length * 2);
	}

	/** 写入一个16位的short型数据 */
	public void writeShort(short s) throws IOException {
		dos.writeByte(s & 0xff);
		dos.writeByte((s >>> 8) & 0xff);
	}
}
