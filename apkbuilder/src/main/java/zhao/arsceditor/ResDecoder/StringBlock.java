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

package zhao.arsceditor.ResDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;

import zhao.arsceditor.ResDecoder.IO.LEDataInputStream;
import zhao.arsceditor.ResDecoder.IO.LEDataOutputStream;


/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 * @author Dmitry Skiba
 * @author ZhaoHai
 * 
 *         Block of strings, used in binary xml and arsc.
 * 
 *         TODO: - implement get()
 * 
 */
public class StringBlock {

	private static final int CHUNK_NULL_TYPE = 0x00000000;

	private static final int CHUNK_STRINGPOOL_TYPE = 0x001C0001;

	// UTF-16解码器
	private static final CharsetDecoder UTF16LE_DECODER = Charset.forName("UTF-16LE").newDecoder();

	// UTF-8解码器
	private static final CharsetDecoder UTF8_DECODER = Charset.forName("UTF-8").newDecoder();

	private static final int UTF8_FLAG = 0x00000100;

	private static final int getShort(byte[] array, int offset) {
		return (array[offset + 1] & 0xff) << 8 | array[offset] & 0xff;
	}

	private static final int[] getVarint(byte[] array, int offset) {
		int val = array[offset];
		boolean more = (val & 0x80) != 0;
		val &= 0x7f;

		if (!more) {
			return new int[] { val, 1 };
		} else {
			return new int[] { val << 8 | array[offset + 1] & 0xff, 2 };
		}
	}

	/**
	 * Reads whole (including chunk type) string block from stream. Stream must
	 * be at the chunk type.
	 */
	public static StringBlock read(LEDataInputStream reader) throws IOException {
		StringBlock block = new StringBlock();

		block.ChunkTypeInt = reader.skipCheckChunkTypeInt(CHUNK_STRINGPOOL_TYPE, CHUNK_NULL_TYPE);
		// 获取块大小
		block.chunkSize = reader.readInt();
		// System.out.println("chunkSize=" + block.chunkSize);
		// 获取字符串数量
		block.stringCount = reader.readInt();
		// System.out.println("stringCount=" + block.stringCount);
		// 获取字符串样式偏移地址数量
		block.styleOffsetCount = reader.readInt();
		// System.out.println("styleOffsetCount=" + block.styleOffsetCount);
		//// 用来描述字符串资源串的属性
		block.flags = reader.readInt();
		// System.out.println("flag=" + block.flags);
		// 获取字符串偏移的地址
		block.stringsOffset = reader.readInt();
		// System.out.println("stringsOffset=" + block.stringsOffset);
		// 获取字符串样式偏移的地址
		block.stylesOffset = reader.readInt();
		// System.out.println("stylesOffset=" + block.stylesOffset);
		// 根据flags判断是否是UTF-8编码
		block.m_isUTF8 = (block.flags & UTF8_FLAG) != 0;
		// 获取所有的字符串偏移的地址，储存到一个数组中
		block.m_stringOffsets = reader.readIntArray(block.stringCount);

		// 如果字符串样式偏移的数量不为0
		if (block.styleOffsetCount != 0) {
			// 获取所有字符串样式的偏移
			block.m_styleOffsets = reader.readIntArray(block.styleOffsetCount);
			// System.out.println("m_styleOffsets size=" +
			// block.m_styleOffsets.length);
		}
		// 使用局部变量，需要加个{}，以免和全局变量混到一起
		{
			int size = ((block.stylesOffset == 0) ? block.chunkSize : block.stylesOffset) - block.stringsOffset;
			if ((size % 4) != 0) {
				throw new IOException("String data size is not multiple of 4 (" + size + ").");
			}
			block.m_strings = new byte[size];
			reader.readFully(block.m_strings);
			// System.out.println("m_strings size=" + size);

			block.strings = new ArrayList<String>();
			for (int i = 0; i < block.stringCount; i++) {
				block.strings.add(block.getString(i));
			}
		}
		// 如果字符串样式偏移不为0
		if (block.stylesOffset != 0) {
			int size = (block.chunkSize - block.stylesOffset);
			if ((size % 4) != 0) {
				throw new IOException("Style data size is not multiple of 4 (" + size + ").");
			}
			block.m_styles = reader.readIntArray(size / 4);
			// System.out.println("m_styles size=" + block.m_styles.length);
			// read remaining bytes
			int remaining = size % 4;
			if (remaining >= 1) {
				while (remaining-- > 0) {
					reader.skipByte();
				}
			}
		}

		return block;
	}

	// chunkSize
	private int chunkSize;

	//
	private int ChunkTypeInt;

	// 用来描述字符串资源串的属性
	private int flags;

	// 是否是UTF-8编码
	private boolean m_isUTF8;

	// 所有字符串的偏移
	private int[] m_stringOffsets;
	// 所有字符串的字节数组
	public byte[] m_strings;

	// 所有字符串样式的偏移
	private int[] m_styleOffsets;
	// 所有字符串样式的整型数组
	private int[] m_styles;
	// 字符串数量
	private int stringCount;
	// 所有新的字符串的偏移
	private int[] stringOffsets;
	// 解析出的字符串
	private List<String> strings;
	// 字符串开始偏移的位置
	private int stringsOffset;
	// 字符串样式偏移的数量
	private int styleOffsetCount;
	// 字符串样式开始偏移的位置
	private int stylesOffset;

	// 解析字符串
	private String decodeString(int offset, int length) throws CharacterCodingException {
		return (m_isUTF8 ? UTF8_DECODER : UTF16LE_DECODER).decode(ByteBuffer.wrap(m_strings, offset, length))
				.toString();

	}

	/**
	 * Finds index of the string. Returns -1 if the string was not found.
	 */
	public int find(String string) {
		if (string == null) {
			return -1;
		}
		for (int i = 0; i != m_stringOffsets.length; ++i) {
			int offset = m_stringOffsets[i];
			int length = getShort(m_strings, offset);
			if (length != string.length()) {
				continue;
			}
			int j = 0;
			for (; j != length; ++j) {
				offset += 2;
				if (string.charAt(j) != getShort(m_strings, offset)) {
					break;
				}
			}
			if (j == length) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 获取字符串数量
	 */
	public int getCount() {
		return m_stringOffsets != null ? m_stringOffsets.length : 0;
	}

	// 获取排序完成后的列表数组，其中包含了修改后的内容，写入文件时，我们只需将里面的字符串按顺序写入即可
	public List<String> getList() {
		return strings;
	}

	/**
	 * 获取常量池中第index个字符串的方法
	 * 
	 * @throws CharacterCodingException
	 */
	public String getString(int index) throws CharacterCodingException {
		// 如果传入的数组下标小于0，或者是字符串地址数组为空或者传入的数组下标大于地址数组的成员个数
		if (index < 0 || m_stringOffsets == null || index >= m_stringOffsets.length) {
			// 返回空值
			return null;
		}
		// 获取第index个字符串的起始地址
		int offset = m_stringOffsets[index];
		// 第index个字符串的长度
		int length;
		// 如果第index个字符串不是UTF-8型,是UTF-16型
		if (!m_isUTF8) {
			// 获取该字符串的长度
			length = getShort(m_strings, offset) * 2;
			// 地址偏移2
			offset += 2;
		} else { // 如果是UTF-8型
			// 地址偏移getVarint(this.m_strings, offset)[1]
			offset += getVarint(m_strings, offset)[1];
			int[] varint = getVarint(m_strings, offset);
			// 地址再次偏移varint[1]
			offset += varint[1];
			// 获取字符串长度
			length = varint[0];
		}
		// 从offest地址处解析length长度个数据，转化为String类型返回
		return decodeString(offset, length);
	}

	// 排序字符串，由于字符串在arsc中是一一对应的，所以不能改变原来的一一对应，需要将列表进行排序
	public void sortStringBlock(String src, String tar) {
		// 查找原字符串在strings这个有序的数组中的位置，以此确定修改后的字符串对应的位置
		int position = strings.indexOf(src);
		// 如果在strings中查找到这个字符串，并且修改后的字符串不为空
		if (position >= 0 && !tar.equals("")) {
			// 从strings数组中移除原来的内容
			// strings.remove(position);
			// 将新字符串添加到相应的位置
			strings.set(position, tar);
		}
	}

	public void writeFully(LEDataOutputStream lmOut, ByteArrayOutputStream bOut) throws IOException {
		// 新字符串样式的偏移
		int newStylesOffset = 0;
		// 新的chunkSize
		int newChunkSize = chunkSize;
		// System.out.println("src chunksize=" + chunkSize);
		// 如果字符串样式偏移为0
		if (stylesOffset == 0)
			// 获取新的chunkSize，经过试验发现,chunkSize=原先的chunkSize +
			// 修改后的字符串占用的字节-原来字符串占用的字节
			newChunkSize = newChunkSize + (bOut.size() - m_strings.length);
		else { // 如果字符串样式偏移不为0
				// 获取新的StylesOffset，经过试验发现,StylesOffset=原先的chunkSize +
				// 修改后的字符串占用的字节-原来字符串占用的字节
			newChunkSize = newChunkSize + (bOut.size() - m_strings.length);
			newStylesOffset = stylesOffset + (bOut.size() - m_strings.length);
		}
		// System.out.println("bOutSize=" + bOut.size());
		lmOut.writeInt(ChunkTypeInt);
		// chunkSize
		lmOut.writeInt(newChunkSize);
		// System.out.println("new chunk size==" + newChunkSize);
		// 写入字符串数量
		lmOut.writeInt(stringCount);
		// System.out.println("stringCount==" + stringCount);
		// 写入字符串样式数量
		lmOut.writeInt(styleOffsetCount);
		// System.out.println("styleOffsetCount==" + styleOffsetCount);
		// 写入资源属性
		lmOut.writeInt(flags);
		// System.out.println("flags==" + flags);
		// 写入字符串偏移的地址
		lmOut.writeInt(stringsOffset);
		// System.out.println("stringOffset==" + stringOffsets);
		// 写入字符串样式偏移的地址
		lmOut.writeInt(newStylesOffset);
		// System.out.println("newstyleOffset==" + newStylesOffset);
		// 写入所有字符串一一对应的地址
		lmOut.writeIntArray(stringOffsets);
		// System.out.println("styleOffsetCount==" + styleOffsetCount);
		// 如果字符串样式偏移的数量不为0
		if (styleOffsetCount != 0) {
			// 写入所有字符串样式一一对应的地址偏移
			lmOut.writeIntArray(m_styleOffsets);
		}
		// 写入字符串
		lmOut.writeFully(bOut.toByteArray());

		if (stylesOffset != 0) {
			// 写入字符串样式
			lmOut.writeIntArray(m_styles);
			int size = (chunkSize - stylesOffset);
			// read remaining bytes
			int remaining = size % 4;
			if (remaining >= 1) {
				while (remaining-- > 0) {
					// 写入剩余内容
					lmOut.writeByte((byte) 0);
				}
			}
		}
	}

	public ByteArrayOutputStream writeString(List<String> stringlist) throws IOException {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		LEDataOutputStream mStrings = new LEDataOutputStream(bOut);

		// 字符串数量
		int size = stringlist.size();

		// 每串字符串在文件中的偏移位置
		stringOffsets = new int[size];
		// 字符串的长度
		int len = 0;

		for (int i = 0; i < size; i++) {
			// 获取列表中的字符串
			String var = stringlist.get(i);
			// 获取字符串的长度
			int length = var.length();
			// 获取字符串的偏移
			stringOffsets[i] = len;
			// 将字符串写入流中
			writeString(var, mStrings);
			// 如果第index个字符串是UTF-16型
			if ((flags & UTF8_FLAG) == 0) {
				// 为什么要大于这个数，这是我分析出来的
				if (length > 0x00007fff) {
					// 字符串在文件中储存的长度偏移2
					len += 2;
				}
				// 字符串在文件中储存的长度偏移2
				len += 2;
				// 字符串在文件中储存的长度偏移该字符串占用的字节的长度
				len += var.getBytes("UTF-16LE").length;
				// 字符串在文件中储存的长度偏移2
				len += 2;
			} else { // 如果是UTF-8型
				if (length > 0x0000007f) {
					// 字符串在文件中储存的长度偏移1
					len += 1;
				}
				// 字符串在文件中储存的长度偏移1
				len += 1;
				// 获取字符串在内存中的字节
				byte[] bytes = var.getBytes("UTF8");
				// 获取该字符串占用字节的大小
				length = bytes.length;
				if (length > 0x0000007f) {
					// 字符串在文件中储存的长度偏移1
					len += 1;
				}
				// 字符串在文件中储存的长度偏移1
				len += 1;
				// 字符串在文件中储存的长度偏移该字符串占用的字节的长度
				len += length;
				// 字符串在文件中储存的长度偏移1
				len += 1;
			}
		}

		// 偏移 0
		/*
		 * while (mStrings.size()%4!=0) { mStrings.writeByte((byte)0); }
		 */
		int size_mod = mStrings.size() % 4;// m_strings_size%4
		// 写入一些0字节用来填充，以确保arsc文件的正确性
		for (int i = 0; i < 4 - size_mod; i++) {
			mStrings.writeByte((byte) 0);
		}
		bOut.close();
		return bOut;
	}

	/**
	 * 写入第index个字符串的方法
	 * 
	 * @throws IOException
	 */
	private void writeString(String str, LEDataOutputStream lmString) throws IOException {
		// 获取字符串长度
		int length = str.length();
		// 如果第index个字符串是UTF-16型
		if ((flags & UTF8_FLAG) == 0) {
			// 该算法是我从汇编中获取的
			if (length > 0x00007fff) {
				int i5 = 0x00008000 | (length >> 16);
				lmString.writeByte((byte) i5);
				lmString.writeByte((byte) (i5 >> 8));
			}
			lmString.writeByte((byte) length);
			lmString.writeByte((byte) (length >> 8));
			lmString.writeFully(str.getBytes("UTF-16LE"));
			lmString.writeByte((byte) 0);
			lmString.writeByte((byte) 0);
		} else { // 如果是UTF-8型
			if (length > 0x0000007f) {
				lmString.writeByte((byte) ((length >> 8) | 0x00000080));
			}
			lmString.writeByte((byte) length);
			byte[] bytes = str.getBytes("UTF8");
			length = bytes.length;
			if (length > 0x0000007f) {
				lmString.writeByte((byte) ((length >> 8) | 0x00000080));
			}
			lmString.writeByte((byte) length);

			lmString.writeFully(bytes);
			lmString.writeByte((byte) 0);
		}
	}
}
