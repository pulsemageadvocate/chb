package pulad.chb.bbs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import pulad.chb.App;
import pulad.chb.config.Config;
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

	/**
	 * URLから対応するBBSを取得する。
	 * @param url
	 * @return
	 */
	public static BBS getBBSFromUrl(String url) {
		for (BBS bbs : bbsList) {
			if (bbs.isUrl(url)) {
				return bbs;
			}
		}
		return null;
	}

	/**
	 * ログフォルダ直下のフォルダ名から対応するBBSを取得する。
	 * @param logDirectoryName
	 * @return
	 */
	public static BBS getBBSFromLogDirectoryName(String logDirectoryName) {
		for (BBS bbs : bbsList) {
			if (bbs.getLogDirectoryName().equals(logDirectoryName)) {
				return bbs;
			}
		}
		return null;
	}

	/**
	 * ログフォルダ内のパスから対応するBBSを取得する。
	 * ログフォルダ内ではない場合はnullを返す。
	 * @param target パス
	 * @return
	 */
	public static BBS getBBSFromLogDirectory(String target) {
		return getBBSFromLogDirectory(Paths.get(target));
	}

	/**
	 * ログフォルダ内のパスから対応するBBSを取得する。
	 * ログフォルダ内ではない場合はnullを返す。
	 * @param target パス
	 * @return
	 */
	public static BBS getBBSFromLogDirectory(Path target) {
		Path logFolderPath = Config.getLogFolder();
		Path targetPath = target.toAbsolutePath();
		try {
			return getBBSFromLogDirectoryName(targetPath.relativize(logFolderPath).getName(0).toString());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private BBSManager() {}
}
