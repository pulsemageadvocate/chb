package pulad.chb.board;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.thymeleaf.util.ObjectUtils;
import org.thymeleaf.util.StringUtils;

import pulad.chb.App;
import pulad.chb.DownloadProcessor;
import pulad.chb.bbs.BBS;
import pulad.chb.bbs.BBSManager;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.FileUtil;
import pulad.chb.util.NumberUtil;

public class BoardManager {
	private static final Pattern regSettingValue = Pattern.compile("^(?<key>[^=]+)=(?<value>[^=]+)$");
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
			String bbs = bbsObject.getLogDirectoryName();
			String board = bbsObject.getBoardFromBoardUrl(url);
			Path settingFilePath = FileUtil.realCapitalPath(App.logFolder.resolve(bbs).resolve(board).resolve("setting.txt"));
			dto = new BoardDto();
			dto.setUrl(url);
			// SETTING.TXTを（無ければ）ダウンロード
			if (!Files.exists(settingFilePath) && remote) {
				DownloadProcessor.download(bbsObject.getSettingTxtUrl(url), settingFilePath, 1048576);
			}

			// SETTING.TXT読み込み
			ConcurrentHashMap<String, String> setting = new ConcurrentHashMap<>();
			dto.setSetting(setting);
			dto.setTitleOrig("");
			dto.setTitle("");
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(settingFilePath.toString(), bbsObject.getCharset()));
				String str = null;
				while ((str = br.readLine()) != null) {
					Matcher matcher = regSettingValue.matcher(str);
					if (matcher.find()) {
						String key = matcher.group("key");
						String value = matcher.group("value");
						setting.put(key, value);
						switch (key) {
						case "BBS_TITLE_ORIG":
							dto.setTitleOrig(value);
							break;
						case "BBS_TITLE":
							dto.setTitle(value);
							break;
						}
					}
				}
			} catch (Exception e) {
				App.logger.error("SETTING.TXT読み込み失敗", e);
				dto.setTitleOrig("SETTING.TXT読み込み失敗");
				dto.setTitle("SETTING.TXT読み込み失敗");
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						App.logger.error("SETTING.TXT close失敗", e);
					}
					br = null;
				}
			}
			if (StringUtils.isEmpty(dto.getTitleOrig()) &&
					!StringUtils.isEmpty(dto.getTitle())) {
				dto.setTitleOrig(dto.getTitle());
			}

			// threadst.txt読み込み
			readThreadst(dto, bbs, board);

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
			writeThreadst(dto, bbs, board);
		}
	}

	private static void readThreadst(BoardDto dto, String bbs, String board) {
		Path threadstFilePath = FileUtil.realCapitalPath(App.logFolder.resolve(bbs).resolve(board).resolve("threadst.txt"));

		ConcurrentHashMap<String, ThreadDto> thread = new ConcurrentHashMap<>(1024, 0.75f, 1);
		if (Files.exists(threadstFilePath)) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(threadstFilePath.toString(), Charset.forName("UTF-8")));
				// ヘッダを捨てる
				String str = br.readLine();
				while ((str = br.readLine()) != null) {
					String[] token = str.split(",", 17);
					if (token.length == 17) {
						ThreadDto threadDto = new ThreadDto();
						threadDto.setBoardUrl(token[0]);
						threadDto.setDatName(token[1]);
						threadDto.setNumber(NumberUtil.parseInt(token[2], 0));
						threadDto.setState(NumberUtil.parseInt(token[3], 0));
						threadDto.setLogCount(NumberUtil.parseInt(token[4], 0));
						threadDto.setiNewRes(NumberUtil.parseInt(token[5], 0));
						threadDto.settLastGet(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[6], 0L)));
						threadDto.settLastWrite(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[7], 0L)));
						threadDto.setResCount(NumberUtil.parseInt(token[8], 0));
						threadDto.setnLastNRes(NumberUtil.parseInt(token[9], 0));
						threadDto.setBuildTime(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[10], 0L)));
						threadDto.settLast(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[11], 0L)));
						threadDto.setnLogSize(NumberUtil.parseLong(token[12], 0L));
						threadDto.setDate(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[13], 0L)));
						threadDto.setLabel(token[14]);
						threadDto.setTitle(token[15]);
						threadDto.setTitleAlias(token[16]);
						thread.put(token[1], threadDto);
					}
				}
			} catch (Exception e) {
				App.logger.error("readThreadst失敗", e);
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						App.logger.error("readThreadst close失敗", e);
					}
					br = null;
				}
			}
		}
		dto.setLogThread(thread);
	}

	private static void writeThreadst(BoardDto dto, String bbs, String board) {
		Path threadstFilePath = FileUtil.realCapitalPath(App.logFolder.resolve(bbs).resolve(board).resolve("threadst.txt"));

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(threadstFilePath.toString(), Charset.forName("UTF-8"), false));
			bw.write("BOARDURL,DATNAME,NUMBER,STATE,NRESGET,INEWRES,TLASTGET,TLASTWRITE,NRES,NLASTNRES,TFIRST,TLAST,NLOGSIZE,DATE,LABEL,TITLE,TITLEALIAS\r\n");
			for (ThreadDto threadDto : dto.getLogThread().values()) {
				bw.write(threadDto.getBoardUrl());
				bw.write(',');
				bw.write(threadDto.getDatName());
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getNumber(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getState(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getLogCount(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getiNewRes(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.gettLastGet()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.gettLastWrite()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getResCount(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getnLastNRes(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.getBuildTime()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.gettLast()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getnLogSize(), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.getDate()), 0L));
				bw.write(',');
				bw.write(ObjectUtils.nullSafe(threadDto.getLabel(), ""));
				bw.write(',');
				bw.write(ObjectUtils.nullSafe(threadDto.getTitle(), ""));
				bw.write(',');
				bw.write(ObjectUtils.nullSafe(threadDto.getTitleAlias(), ""));
				bw.write("\r\n");
			}
			bw.flush();
		} catch (Exception e) {
			App.logger.error("writeThreadst失敗", e);
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
				}
				bw = null;
			}
		}
	}

	private BoardManager() {}
}
