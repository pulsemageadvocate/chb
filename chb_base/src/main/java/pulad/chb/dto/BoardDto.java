package pulad.chb.dto;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BoardDto implements Serializable, Cloneable {
	/**
	 * URL
	 */
	private String url;
	/**
	 * SETTING.TXTのBBS_TITLE_ORIG。短い名前。
	 */
	private String titleOrig;
	/**
	 * SETTING.TXTのBBS_TITLE。長い名前。
	 */
	private String title;
	/**
	 * スレ一覧
	 */
	private List<ThreadDto> thread;
	/**
	 * 過去ログ（現行含む）
	 * キーはdatファイル名。
	 */
	private ConcurrentHashMap<String, ThreadDto> logThread;
	/**
	 * setting.txtの内容
	 */
	private ConcurrentHashMap<String, String> setting;
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTitleOrig() {
		return titleOrig;
	}
	public void setTitleOrig(String titleOrig) {
		this.titleOrig = titleOrig;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<ThreadDto> getThread() {
		return thread;
	}
	public void setThread(List<ThreadDto> thread) {
		this.thread = thread;
	}
	public ConcurrentHashMap<String, ThreadDto> getLogThread() {
		return logThread;
	}
	public void setLogThread(ConcurrentHashMap<String, ThreadDto> logThread) {
		this.logThread = logThread;
	}
	public ConcurrentHashMap<String, String> getSetting() {
		return setting;
	}
	public void setSetting(ConcurrentHashMap<String, String> setting) {
		this.setting = setting;
	}

	/**
	 * {@inheritDoc}
	 * threadとsettingはただの値コピー。
	 */
	@Override
	public Object clone() {
		BoardDto dest = new BoardDto();
		dest.setUrl(this.getUrl());
		dest.setTitleOrig(this.getTitleOrig());
		dest.setTitle(this.getTitle());
		dest.setThread(this.getThread());
		dest.setLogThread(this.getLogThread());
		dest.setSetting(this.getSetting());
		return dest;
	}
}
