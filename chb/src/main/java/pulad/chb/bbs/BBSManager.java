package pulad.chb.bbs;

import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import pulad.chb.App;
import pulad.chb.interfaces.BBS;

/**
 * 各BBSの固有の処理を{@link ServiceLoader}の仕組みで取得する。
 * @author pulad
 *
 */
public class BBSManager {
	private static List<BBS> bbsList;

	public static void init() {
		App.logger.debug("pulad.chb.interfaces.BBS読み込み開始");
		bbsList = ServiceLoader.load(BBS.class, Thread.currentThread().getContextClassLoader()).stream()
				.map(Provider::get)
				.collect(Collectors.toList());
		if (App.logger.isDebugEnabled()) {
			for (BBS bbs : bbsList) {
				App.logger.debug("{}が読み込まれました。", bbs.getClass().getName());
			}
		}
		App.logger.debug("pulad.chb.interfaces.BBS読み込み終了: {}個", bbsList.size());
	}

	public static BBS getBBSFromUrl(String url) {
		for (BBS bbs : bbsList) {
			if (bbs.isUrl(url)) {
				return bbs;
			}
		}
		return null;
	}

	public static BBS getBBSFromLogDirectoryName(String logDirectoryName) {
		for (BBS bbs : bbsList) {
			if (bbs.getLogDirectoryName().equals(logDirectoryName)) {
				return bbs;
			}
		}
		return null;
	}

	private BBSManager() {}
}
