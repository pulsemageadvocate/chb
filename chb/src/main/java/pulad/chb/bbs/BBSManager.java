package pulad.chb.bbs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BBSManager {
	private static final Pattern reg5ch = Pattern.compile("^(?<root>https?://[^/]+\\.[25]ch\\.[^/]+/)");
	private static final Pattern regShitaraba = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)");
	private static final Pattern reg5chBoard = Pattern.compile("^(?<root>https?://[^/]+\\.[25]ch\\.[^/]+/)(?<board>[^/]+)/");
	private static final Pattern regShitarabaBoard = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)(?<board>[^/]+)/(?<num>[0-9]+)/");
	//private static final Pattern regOtherBoard = Pattern.compile("^(?<root>https?://(?<bbs>[^/]+)/)(?<board>[^/]+)/");
	private static final Pattern reg5chThread = Pattern.compile("^(?<root>https?://[^/]+\\.[25]ch\\.[^/]+/)test/read\\.cgi/(?<board>[^/]+)/(?<thread>[0-9]+)/");
	private static final Pattern regShitarabaThread = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)bbs/read\\.cgi/(?<board>[^/]+)/(?<num>[0-9]+)/(?<thread>[0-9]+)/");

	public static final _5ch ch = new _5ch();
	public static final Shitaraba shitaraba = new Shitaraba();

	public static BBS getBBSFromUrl(String url) {
		Matcher matcher = reg5ch.matcher(url);
		if (matcher.find()) {
			//2ch/5ch
			return ch;
		}
		matcher = regShitaraba.matcher(url);
		if (matcher.find()) {
			//shitaraba
			return shitaraba;
		}
		return null;
	}

	public static BBS getBBSFromDirectoryName(String bbs) {
		switch (bbs) {
		case "2ch_":
			return ch;
		case "jbbs_":
			return shitaraba;
		}
		return null;
	}

	private BBSManager() {}
}
