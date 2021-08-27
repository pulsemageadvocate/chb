package pulad.chb.shitaraba;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.concurrent.Task;
import pulad.chb.config.Config;
import pulad.chb.dto.ThreadWriteTaskResponseDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.ThreadLoader;
import pulad.chb.util.FileUtil;

public class Shitaraba implements BBS {
	private static final Pattern regShitaraba = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)");
	private static final Pattern regShitarabaBoard = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)(?!bbs/)(?<board>[^/]+)/(?<num>[0-9]+)/");
	private static final Pattern regShitarabaThread = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)bbs/read\\.cgi/(?<board>[^/]+)/(?<num>[0-9]+)/(?<thread>[0-9]+)/");
	private static final Pattern regSettingValue = Pattern.compile("^(?<key>[^=]+)=(?<value>[^=]+)$");

	@Override
	public String getBBSDirectoryName() {
		return "JBBSShitaraba";
	}

	@Override
	public String getLogDirectoryName() {
		return "jbbs_";
	}

	@Override
	public boolean isUrl(String url) {
		return regShitaraba.matcher(url).find();
	}

	@Override
	public boolean isBoardUrl(String url) {
		return regShitarabaBoard.matcher(url).find();
	}

	@Override
	public boolean isThreadUrl(String url) {
		return regShitarabaThread.matcher(url).find();
	}

	@Override
	public boolean isDatFileName(String fileName) {
		return fileName.endsWith(".cgi");
	}

	@Override
	public String getBoardFromBoardUrl(String boardUrl) {
		Matcher matcher = regShitarabaBoard.matcher(boardUrl);
		if (matcher.find()) {
			return matcher.group("board") + File.separatorChar + matcher.group("num");
		}
		return null;
	}

	@Override
	public String getBoardFromThreadUrl(String threadUrl) {
		Matcher matcher = regShitarabaThread.matcher(threadUrl);
		if (matcher.find()) {
			return matcher.group("board") + File.separatorChar + matcher.group("num");
		}
		return null;
	}

	@Override
	public String getBoardUrlFromThreadUrl(String threadUrl) {
		Matcher matcher = regShitarabaThread.matcher(threadUrl);
		if (matcher.find()) {
			return matcher.group("root") + matcher.group("board") + "/" + matcher.group("num") + "/";
		}
		return null;
	}

	@Override
	public String getThreadFromThreadUrl(String threadUrl) {
		Matcher matcher = regShitarabaThread.matcher(threadUrl);
		if (matcher.find()) {
			//shitaraba
			return matcher.group("thread");
		}
		return null;
	}

	@Override
	public String getDatFileNameFromThreadUrl(String threadUrl) {
		Matcher matcher = regShitarabaThread.matcher(threadUrl);
		if (matcher.find()) {
			//shitaraba
			return matcher.group("thread") + ".cgi";
		}
		return null;
	}

	@Override
	public String getThreadUrlFromBoardUrl(String boardUrl, String thread) {
		Matcher matcher = regShitarabaBoard.matcher(boardUrl);
		if (matcher.find()) {
			return matcher.group("root") + "bbs/read.cgi/" + matcher.group("board") + "/" + matcher.group("num") + "/" + thread + "/" ;
		}
		return null;
	}

	@Override
	public String getThreadUrlFromBoardUrlAndDatFileName(String boardUrl, String datFileName) {
		String thread = datFileName.substring(0, datFileName.lastIndexOf("."));
		return getThreadUrlFromBoardUrl(boardUrl, thread);
	}

	@Override
	public String getThreadUrlFromRawUrl(String rawUrl) {
		Matcher matcher = regShitarabaThread.matcher(rawUrl);
		if (matcher.find()) {
			return matcher.group("root") + "bbs/read.cgi/" + matcher.group("board") + "/" + matcher.group("num") + "/" + matcher.group("thread") + "/" ;
		}
		return null;
	}

	@Override
	public String getThreadWriteUrlFromThreadUrl(String threadUrl) {
		throw new UnsupportedOperationException("実装されていません。");
	}

	@Override
	public Charset getCharset() {
		return Charset.forName("EUC-JP");
	}

	@Override
	public String getSubjectTxtUrl(String boardUrl) {
		return boardUrl + "subject.txt";
	}

	@Override
	public String getSettingTxtUrl(String boardUrl) {
		Matcher matcher = regShitarabaBoard.matcher(boardUrl);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group("root") + "bbs/api/setting.cgi/" + matcher.group("board") + "/" + matcher.group("num") + "/";
	}

	@Override
	public ConcurrentHashMap<String, String> readSettingTxt(String boardUrl) throws IOException {
		String bbs = this.getLogDirectoryName();
		String board = this.getBoardFromBoardUrl(boardUrl);
		Path settingFilePath = FileUtil.realCapitalPath(Config.getLogFolder().resolve(bbs).resolve(board).resolve("setting.txt"));

		ConcurrentHashMap<String, String> setting = new ConcurrentHashMap<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(settingFilePath.toString(), this.getCharset()));
			String str = null;
			while ((str = br.readLine()) != null) {
				Matcher matcher = regSettingValue.matcher(str);
				if (matcher.find()) {
					String key = matcher.group("key");
					String value = matcher.group("value");
					setting.put(key, value);
				}
			}
			return setting;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
				br = null;
			}
		}
	}

	@Override
	public ThreadLoader createThreadLoader(String url) {
		return new ShitarabaThreadLoader(url);
	}

	@Override
	public Task<ThreadWriteTaskResponseDto> createWriteTask(String url, String name, String mail, String body) {
		throw new UnsupportedOperationException("実装されていません。");
	}
}
