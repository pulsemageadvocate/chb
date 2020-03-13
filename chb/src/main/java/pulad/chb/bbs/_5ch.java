package pulad.chb.bbs;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.read.thread.AbstractThreadLoadTask;
import pulad.chb.read.thread.ThreadLoadTask;

public class _5ch implements BBS {
	private static final Pattern reg5ch = Pattern.compile("^(?<root>https?://[^/]+\\.[25]ch\\.[^/]+/)");
	private static final Pattern reg5chBoard = Pattern.compile("^(?<root>https?://[^/]+\\.[25]ch\\.[^/]+/)(?<board>[^/]+)/");
	private static final Pattern reg5chThread = Pattern.compile("^(?<root>https?://[^/]+\\.[25]ch\\.[^/]+/)test/read\\.cgi/(?<board>[^/]+)/(?<thread>[0-9]+)/");

	@Override
	public String getBBSDirectoryName() {
		return "2ch";
	}

	@Override
	public String getLogDirectoryName() {
		return "2ch_";
	}

	@Override
	public boolean isUrl(String url) {
		return reg5ch.matcher(url).find();
	}

	@Override
	public String getBoardFromBoardUrl(String boardUrl) {
		Matcher matcher = reg5chBoard.matcher(boardUrl);
		if (matcher.find()) {
			//2ch/5ch
			return matcher.group("board");
		}
		return null;
	}

	@Override
	public String getBoardFromThreadUrl(String threadUrl) {
		Matcher matcher = reg5chThread.matcher(threadUrl);
		if (matcher.find()) {
			return matcher.group("board");
		}
		return null;
	}

	@Override
	public String getBoardUrlFromThreadUrl(String threadUrl) {
		Matcher matcher = reg5chThread.matcher(threadUrl);
		if (matcher.find()) {
			return matcher.group("root") + matcher.group("board") + "/";
		}
		return null;
	}

	@Override
	public String getThreadFromThreadUrl(String threadUrl) {
		Matcher matcher = reg5chThread.matcher(threadUrl);
		if (matcher.find()) {
			return matcher.group("thread");
		}
		return null;
	}

	@Override
	public String getDatFileNameFromThreadUrl(String threadUrl) {
		Matcher matcher = reg5chThread.matcher(threadUrl);
		if (matcher.find()) {
			return matcher.group("thread") + ".dat";
		}
		return null;
	}

	@Override
	public String getThreadUrlFromBoardUrl(String boardUrl, String thread) {
		Matcher matcher = reg5chBoard.matcher(boardUrl);
		if (matcher.find()) {
			return matcher.group("root") + "test/read.cgi/" + matcher.group("board") + "/" + thread + "/" ;
		}
		return null;
	}

	@Override
	public String getThreadUrlFromBoardUrlAndDatFileName(String boardUrl, String datFileName) {
		String thread = datFileName.substring(0, datFileName.lastIndexOf("."));
		return getThreadUrlFromBoardUrl(boardUrl, thread);
	}

	@Override
	public Charset getCharset() {
		return Charset.forName("MS932");
	}

	@Override
	public String getSubjectTxtUrl(String boardUrl) {
		return boardUrl + "subject.txt";
	}

	@Override
	public String getSettingTxtUrl(String boardUrl) {
		return boardUrl + "SETTING.TXT";
	}

	@Override
	public AbstractThreadLoadTask createThreadLoadTask(String url) {
		return new ThreadLoadTask(url);
	}

	@Override
	public AbstractThreadLoadTask createThreadLoadTask(String url, boolean remote) {
		return new ThreadLoadTask(url, remote);
	}

	@Override
	public AbstractThreadLoadTask createThreadLoadTask(String url, boolean remote, Collection<Integer> resFilter) {
		return new ThreadLoadTask(url, remote, resFilter);
	}
}
