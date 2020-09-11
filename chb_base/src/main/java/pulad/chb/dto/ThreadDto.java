package pulad.chb.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import pulad.chb.constant.AboneLevel;

/**
 * subject.txt
 */
public class ThreadDto implements Serializable {
	/**
	 * BOARDURL
	 * url。
	 */
	private String boardUrl;
	/**
	 * DATNAME
	 * datファイル名。
	 */
	private String datName;
	private int number;
	/**
	 * STATE
	 * 1=スレッド終了？
	 * 8=過去ログ化？
	 */
	private int state;
	/**
	 * NRESGET
	 * 取得済みレス数。
	 */
	private int logCount;
	private int iNewRes;
	/**
	 * TLASTGET
	 * 最終取得日時？
	 */
	private LocalDateTime tLastGet;
	/**
	 * TLASTWRITE
	 * 最終書込日時？
	 */
	private LocalDateTime tLastWrite;
	/**
	 * NRES
	 * オンラインのレス数。
	 */
	private int resCount;
	/**
	 * NLASTNRES
	 * 前回板チェック時のレス数（レス－NLASTRES＝新着）
	 */
	private int nLastNRes;
	/**
	 * TFIRST
	 * 最初のレスの日時
	 * datファイル名より3桁多い。
	 */
	private LocalDateTime buildTime;
	/**
	 * TLAST
	 * 最後のレスの日時
	 */
	private LocalDateTime tLast;
	/**
	 * NLOGSIZE
	 * ログファイルのサイズ。
	 */
	private long nLogSize;
	/**
	 * DATE
	 * TLASTGETと同じ？
	 */
	private LocalDateTime date;
	private String label;
	/**
	 * TITLE
	 * スレッド名
	 */
	private String title;
	private String titleAlias;
	private AboneLevel abone;
	public String getBoardUrl() {
		return boardUrl;
	}
	public void setBoardUrl(String boardUrl) {
		this.boardUrl = boardUrl;
	}
	public String getDatName() {
		return datName;
	}
	public void setDatName(String datName) {
		this.datName = datName;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
	public int getLogCount() {
		return logCount;
	}
	public void setLogCount(int logCount) {
		this.logCount = logCount;
	}
	public int getiNewRes() {
		return iNewRes;
	}
	public void setiNewRes(int iNewRes) {
		this.iNewRes = iNewRes;
	}
	public LocalDateTime gettLastGet() {
		return tLastGet;
	}
	public void settLastGet(LocalDateTime tLastGet) {
		this.tLastGet = tLastGet;
	}
	public LocalDateTime gettLastWrite() {
		return tLastWrite;
	}
	public void settLastWrite(LocalDateTime tLastWrite) {
		this.tLastWrite = tLastWrite;
	}
	public int getResCount() {
		return resCount;
	}
	public void setResCount(int resCount) {
		this.resCount = resCount;
	}
	public int getnLastNRes() {
		return nLastNRes;
	}
	public void setnLastNRes(int nLastNRes) {
		this.nLastNRes = nLastNRes;
	}
	public LocalDateTime getBuildTime() {
		return buildTime;
	}
	public void setBuildTime(LocalDateTime buildTime) {
		this.buildTime = buildTime;
	}
	public LocalDateTime gettLast() {
		return tLast;
	}
	public void settLast(LocalDateTime tLast) {
		this.tLast = tLast;
	}
	public long getnLogSize() {
		return nLogSize;
	}
	public void setnLogSize(long nLogSize) {
		this.nLogSize = nLogSize;
	}
	public LocalDateTime getDate() {
		return date;
	}
	public void setDate(LocalDateTime date) {
		this.date = date;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTitleAlias() {
		return titleAlias;
	}
	public void setTitleAlias(String titleAlias) {
		this.titleAlias = titleAlias;
	}
	public AboneLevel getAbone() {
		return abone;
	}
	public void setAbone(AboneLevel abone) {
		this.abone = abone;
	}
}
