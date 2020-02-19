package pulad.chb.bbs;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.Predicate;

import pulad.chb.dto.ResDto;
import pulad.chb.read.thread.AbstractThreadLoadTask;

public interface BBS {

	public String getBBSDirectoryName();
	public String getLogDirectoryName();
	public String getBoardFromBoardUrl(String boardUrl);
	public String getBoardFromThreadUrl(String threadUrl);
	public String getBoardUrlFromThreadUrl(String threadUrl);
	public String getThreadFromThreadUrl(String threadUrl);
	public String getDatFileNameFromThreadUrl(String threadUrl);
	public String getThreadUrlFromBoardUrl(String boardUrl, String thread);
	public String getThreadUrlFromBoardUrlAndDatFileName(String boardUrl, String datFileName);
	public Charset getCharset();
	public String getSubjectTxtUrl(String boardUrl);
	public String getSettingTxtUrl(String boardUrl);
	public AbstractThreadLoadTask createThreadLoadTask(String url);
	public AbstractThreadLoadTask createThreadLoadTask(String url, boolean remote);
	public AbstractThreadLoadTask createThreadLoadTask(String url, boolean remote, Collection<Integer> resFilter);
}
