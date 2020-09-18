package pulad.chb.interfaces;

import java.nio.charset.Charset;

import javafx.concurrent.Task;
import pulad.chb.dto.ThreadWriteTaskResponseDto;

public interface BBS {
	public String getBBSDirectoryName();
	public String getLogDirectoryName();
	public boolean isUrl(String url);
	public boolean isBoardUrl(String url);
	public boolean isThreadUrl(String url);
	public String getBoardFromBoardUrl(String boardUrl);
	public String getBoardFromThreadUrl(String threadUrl);
	public String getBoardUrlFromThreadUrl(String threadUrl);
	public String getThreadFromThreadUrl(String threadUrl);
	public String getDatFileNameFromThreadUrl(String threadUrl);
	public String getThreadUrlFromBoardUrl(String boardUrl, String thread);
	public String getThreadUrlFromBoardUrlAndDatFileName(String boardUrl, String datFileName);
	public String getThreadWriteUrlFromThreadUrl(String threadUrl);
	public Charset getCharset();
	public String getSubjectTxtUrl(String boardUrl);
	public String getSettingTxtUrl(String boardUrl);
	public ThreadLoader createThreadLoader(String url);
	public Task<ThreadWriteTaskResponseDto> createWriteTask(String url, String name, String mail, String body);
}
