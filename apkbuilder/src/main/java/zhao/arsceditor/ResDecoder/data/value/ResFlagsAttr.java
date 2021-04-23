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
import java.util.Arrays;
import java.util.Comparator;

import zhao.arsceditor.ResDecoder.ARSCCallBack;
import zhao.arsceditor.ResDecoder.IO.Duo;
import zhao.arsceditor.ResDecoder.data.ResResource;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResFlagsAttr extends ResAttr {
	private static class FlagItem {
		public final int flag;
		public String value;

		public FlagItem(ResReferenceValue ref, int flag) {
			this.flag = flag;
		}

		public String getValue() throws IOException {
			if (value == null) {
				// value = ref.getReferent().getName();
			}
			return value;
		}
	}

	private FlagItem[] mFlags;

	private final FlagItem[] mItems;

	private FlagItem[] mZeroFlags;

	ResFlagsAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n,
                 Duo<ResReferenceValue, ResIntValue>[] items) {
		super(parent, type, min, max, l10n);

		mItems = new FlagItem[items.length];
		for (int i = 0; i < items.length; i++) {
			mItems[i] = new FlagItem(items[i].m1, items[i].m2.getValue());
		}
	}

	@Override
	public String convertToResXmlFormat(ResScalarValue value) throws IOException {
		if (value instanceof ResReferenceValue) {
			return value.encodeAsResValue();
		}
		if (!(value instanceof ResIntValue)) {
			return super.convertToResXmlFormat(value);
		}
		loadFlags();
		int intVal = ((ResIntValue) value).getValue();

		if (intVal == 0) {
			return renderFlags(mZeroFlags);
		}

		FlagItem[] flagItems = new FlagItem[mFlags.length];
		int[] flags = new int[mFlags.length];
		int flagsCount = 0;
		for (int i = 0; i < mFlags.length; i++) {
			FlagItem flagItem = mFlags[i];
			int flag = flagItem.flag;

			if ((intVal & flag) != flag) {
				continue;
			}

			if (!isSubpartOf(flag, flags)) {
				flags[flagsCount] = flag;
				flagItems[flagsCount++] = flagItem;
			}
		}
		return renderFlags(Arrays.copyOf(flagItems, flagsCount));
	}

	private boolean isSubpartOf(int flag, int[] flags) {
		for (int i = 0; i < flags.length; i++) {
			if ((flags[i] & flag) == flag) {
				return true;
			}
		}
		return false;
	}

	private void loadFlags() {
		if (mFlags != null) {
			return;
		}

		FlagItem[] zeroFlags = new FlagItem[mItems.length];
		int zeroFlagsCount = 0;
		FlagItem[] flags = new FlagItem[mItems.length];
		int flagsCount = 0;

		for (int i = 0; i < mItems.length; i++) {
			FlagItem item = mItems[i];
			if (item.flag == 0) {
				zeroFlags[zeroFlagsCount++] = item;
			} else {
				flags[flagsCount++] = item;
			}
		}

		mZeroFlags = Arrays.copyOf(zeroFlags, zeroFlagsCount);
		mFlags = Arrays.copyOf(flags, flagsCount);

		Arrays.sort(mFlags, new Comparator<FlagItem>() {
			@Override
			public int compare(FlagItem o1, FlagItem o2) {
				return Integer.valueOf(Integer.bitCount(o2.flag)).compareTo(Integer.bitCount(o1.flag));
			}
		});
	}

	private String renderFlags(FlagItem[] flags) throws IOException {
		String ret = "";
		for (int i = 0; i < flags.length; i++) {
			ret += "|" + flags[i].getValue();
		}
		if (ret.equals("")) {
			return ret;
		}
		return ret.substring(1);
	}

	@Override
	protected void serializeBody(ARSCCallBack back, ResResource res) throws IOException, IOException {
		for (int i = 0; i < mItems.length; i++) {
			FlagItem item = mItems[i];
			back.back(res.getConfig().toString(), res.getResSpec().getType().getName(), item.getValue(),
					String.format("0x%08x", item.flag));
		}
	}
}
