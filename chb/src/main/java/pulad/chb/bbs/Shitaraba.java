package pulad.chb.bbs;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.read.thread.AbstractThreadLoadTask;
import pulad.chb.read.thread.ShitarabaThreadLoadTask;
import pulad.chb.write.AbstractWriteTask;

public class Shitaraba implements BBS {
	private static final Pattern regShitaraba = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)");
	private static final Pattern regShitarabaBoard = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)(?<board>[^/]+)/(?<num>[0-9]+)/");
	private static final Pattern regShitarabaThread = Pattern.compile("^(?<root>https?://jbbs\\.shitaraba\\.(net|com)/)bbs/read\\.cgi/(?<board>[^/]+)/(?<num>[0-9]+)/(?<thread>[0-9]+)/");

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
	public AbstractThreadLoadTask createThreadLoadTask(String url) {
		return new ShitarabaThreadLoadTask(url);
	}

	@Override
	public AbstractThreadLoadTask createThreadLoadTask(String url, boolean remote) {
		return new ShitarabaThreadLoadTask(url, remote);
	}

	@Override
	public AbstractThreadLoadTask createThreadLoadTask(String url, boolean remote, Collection<Integer> resFilter) {
		return new ShitarabaThreadLoadTask(url, remote, resFilter);
	}

	@Override
	public AbstractWriteTask createWriteTask(String url, String name, String mail, String body) {
		throw new UnsupportedOperationException("実装されていません。");
	}
}
