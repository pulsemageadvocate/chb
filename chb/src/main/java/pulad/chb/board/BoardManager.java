package pulad.chb.board;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.thymeleaf.util.StringUtils;

import pulad.chb.App;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.dto.BoardDto;
import pulad.chb.file.Threadst;
import pulad.chb.interfaces.BBS;
import pulad.chb.util.DownloadProcessor;
import pulad.chb.util.FileUtil;

public class BoardManager {
	private static ConcurrentHashMap<String, BoardDto> map = new ConcurrentHashMap<>();

	/**
	 * BoardDtoのクローンをキャッシュから取得する。
	 * 無ければSETTING.TXTから読み込んで生成する。
	 * SETTING.TXTが無ければダウンロードする。
	 * @param url
	 * @return
	 */
	public static BoardDto get(String url) {
		return get(url, true);
	}

	/**
	 * BoardDtoのクローンをキャッシュから取得する。
	 * 無ければSETTING.TXTから読み込んで生成する。
	 * SETTING.TXTが無ければダウンロードする。
	 * 対応していないURLの場合はnullを返す。
	 * @param url
	 * @param remote
	 * @return
	 */
	public static BoardDto get(String url, boolean remote) {
		BoardDto dto = map.get(url);
		if (dto != null) {
			return (BoardDto) dto.clone();
		}

		synchronized (BoardManager.class) {
			dto = map.get(url);
			if (dto != null) {
				return (BoardDto) dto.clone();
			}

			BBS bbsObject = BBSManager.getBBSFromUrl(url);
			if (bbsObject == null) {
				return null;
			}
			String bbs = bbsObject.getLogDirectoryName();
			String board = bbsObject.getBoardFromBoardUrl(url);
			Path settingFilePath = FileUtil.realCapitalPath(Config.getLogFolder().resolve(bbs).resolve(board).resolve("setting.txt"));
			dto = new BoardDto();
			dto.setUrl(url);
			// SETTING.TXTを（無ければ）ダウンロード
			if (!Files.exists(settingFilePath) && remote) {
				try {
					DownloadProcessor.download(bbsObject.getSettingTxtUrl(url), settingFilePath, 1048576);
				} catch (IOException e) {
				}
			}

			// SETTING.TXT読み込み
			ConcurrentHashMap<String, String> setting;
			try {
				setting = bbsObject.readSettingTxt(url);
			} catch (Exception e) {
				App.logger.error("SETTING.TXT読み込み失敗", e);
				setting = new ConcurrentHashMap<String, String>();
				setting.put("BBS_TITLE_ORIG", "SETTING.TXT読み込み失敗");
				setting.put("BBS_TITLE", "SETTING.TXT読み込み失敗");
			}
			dto.setSetting(setting);
			dto.setTitleOrig(setting.getOrDefault("BBS_TITLE_ORIG", ""));
			dto.setTitle(setting.getOrDefault("BBS_TITLE", ""));
			if (StringUtils.isEmpty(dto.getTitleOrig()) &&
					!StringUtils.isEmpty(dto.getTitle())) {
				dto.setTitleOrig(dto.getTitle());
			}

			// threadst.txt読み込み
			try {
				dto.setLogThread(Threadst.read(bbs, board));
			} catch (Exception e) {
				App.logger.error("Threadst.read失敗", e);
			}

			map.put(url, dto);
			return (BoardDto) dto.clone();
		}
	}

	/**
	 * Threadst.txtを更新する。
	 * @param dto
	 * @return
	 */
	public static void updateThreadst(BoardDto dto) {
		String url = dto.getUrl();
		synchronized (BoardManager.class) {
			map.put(url, dto);

			BBS bbsObject = BBSManager.getBBSFromUrl(url);
			String bbs = bbsObject.getLogDirectoryName();
			String board = bbsObject.getBoardFromBoardUrl(url);
			try {
				Threadst.write(dto.getLogThread(), bbs, board);
			} catch (Exception e) {
				App.logger.error("Threadst.write失敗", e);
			}
		}
	}

	private BoardManager() {}
}
