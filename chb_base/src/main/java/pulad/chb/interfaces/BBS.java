package pulad.chb.interfaces;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

import javafx.concurrent.Task;
import pulad.chb.dto.ThreadWriteTaskResponseDto;

/**
 * BBS固有の処理。
 * 実装はスレッドセーフでなければならない。
 * @author pulad
 *
 */
public interface BBS {
	public String getBBSDirectoryName();
	public String getLogDirectoryName();
	public boolean isUrl(String url);
	public boolean isBoardUrl(String url);
	public boolean isThreadUrl(String url);
	public boolean isDatFileName(String fileName); // 未使用
	public String getBoardFromBoardUrl(String boardUrl);
	public String getBoardFromThreadUrl(String threadUrl);
	public String getBoardUrlFromThreadUrl(String threadUrl);
	public String getThreadFromThreadUrl(String threadUrl);
	public String getDatFileNameFromThreadUrl(String threadUrl);
	public String getThreadUrlFromBoardUrl(String boardUrl, String thread);
	public String getThreadUrlFromBoardUrlAndDatFileName(String boardUrl, String datFileName);
	/**
	 * 末尾のレス番指定を削除する。
	 * @param rawUrl
	 * @return
	 */
	public String getThreadUrlFromRawUrl(String rawUrl);
	public String getThreadWriteUrlFromThreadUrl(String threadUrl);
	public Charset getCharset();
	public String getSubjectTxtUrl(String boardUrl);
	/**
	 * SETTING.TXTのURLを取得する。
	 * @param boardUrl
	 * @return
	 */
	public String getSettingTxtUrl(String boardUrl);
	public ConcurrentHashMap<String, String> readSettingTxt(String boardUrl) throws IOException;
	public BoardListLoader createBoardListLoader(String url);
	public ThreadLoader createThreadLoader(String url);
	public Task<ThreadWriteTaskResponseDto> createWriteTask(String url, String name, String mail, String body);
}
