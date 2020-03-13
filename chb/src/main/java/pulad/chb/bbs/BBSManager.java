package pulad.chb.bbs;

import java.util.ServiceLoader;

/**
 * 各BBSの固有の処理を{@link ServiceLoader}の仕組みで取得..する予定です。
 * @author pulad
 *
 */
public class BBSManager {
	//private static ServiceLoader<BBS> bbsLoader = ServiceLoader.load(BBS.class);
	private static BBS[] bbsLoader = {new _5ch(), new Shitaraba()};

	public static BBS getBBSFromUrl(String url) {
		for (BBS bbs : bbsLoader) {
			if (bbs.isUrl(url)) {
				return bbs;
			}
		}
		return null;
	}

	public static BBS getBBSFromLogDirectoryName(String logDirectoryName) {
		for (BBS bbs : bbsLoader) {
			if (bbs.getLogDirectoryName().equals(logDirectoryName)) {
				return bbs;
			}
		}
		return null;
	}

	private BBSManager() {}
}
