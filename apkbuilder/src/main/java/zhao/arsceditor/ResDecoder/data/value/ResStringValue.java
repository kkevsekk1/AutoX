package zhao.arsceditor.ResDecoder.data.value;

public class ResStringValue extends ResScalarValue {

	public ResStringValue(String value, int rawValue) {
		this(value, rawValue, "string");
	}

	public ResStringValue(String value, int rawValue, String type) {
		super(type, rawValue, value);
	}

	@Override
	public String encodeAsResValue() {
		return mRawValue;
	}
}
