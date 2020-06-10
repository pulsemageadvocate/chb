package pulad.chb.util;

import java.util.regex.Pattern;

public class UrlUtil {
	private static final Pattern regHttp = Pattern.compile("^http://");

	/**
	 * http://をhttps://に変える
	 * @param url
	 * @return
	 */
	public static String toHttps(String url) {
		return regHttp.matcher(url).replaceFirst("https://");
	}

	private UrlUtil() {}
}
