package pulad.chb.util;

public class NumberUtil {
	/**
	 * Integerオブジェクトの0～1001のキャッシュ。
	 */
	private static final Integer[] integerObjectCache;

	static {
		integerObjectCache = new Integer[1002];
		for (int i = 0; i < 1002; i++) {
			integerObjectCache[i] = Integer.valueOf(i);
		}
	}

	/**
	 * Integerオブジェクトのキャッシュを返す。0～1001でなければ{@link Integer#valueOf(int)}を返す。
	 * @param value int
	 * @return Integer型
	 */
	public static Integer integerCache(int value) {
		return (0 <= value && value < 1002) ? integerObjectCache[value] : Integer.valueOf(value);
	}

	public static int parseInt(String value, int defaultValue) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static long parseLong(String value, long defaultValue) {
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static String toStringDefaultEmpty(int value, int defaultValue) {
		return (value == defaultValue) ? "" : Integer.toString(value);
	}

	public static String toStringDefaultEmpty(long value, long defaultValue) {
		return (value == defaultValue) ? "" : Long.toString(value);
	}
}
