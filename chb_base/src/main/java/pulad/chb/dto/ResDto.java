package pulad.chb.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import pulad.chb.constant.AboneLevel;

/**
 * レスを表します。
 * @author pulad
 *
 */
public class ResDto implements Serializable {
	/**
	 * &lt;&gt;区切りのdatファイル内の文字列。
	 */
	private String source;
	/**
	 * datファイル内のレス番（あれば）。
	 */
	private int number;
	/**
	 * 名前
	 */
	private String name;
	/**
	 * 名無し
	 * 名前からの解析で設定。
	 */
	private boolean anonymous;
	/**
	 * ﾜｯﾁｮｲ
	 * 名前からの解析で設定。
	 */
	private String wacchoi;
	/**
	 * ﾜｯﾁｮｲ下4桁(-abcd)
	 * 名前からの解析で設定。
	 */
	private String wacchoiLower;
	/**
	 * IP
	 * 名前からの解析で設定。
	 */
	private String ip;
	/**
	 * IPの後にある文字列（上級国民）
	 * 名前からの解析で設定。
	 */
	private String ipTrailing;
	/**
	 * メール
	 */
	private String mail;
	/**
	 * 日時（ID、ホストを含む）
	 */
	private String timeIdAux;
	/**
	 * 日時（解析後）
	 * 日時からの解析で設定。
	 */
	private String time;
	/**
	 * 日時（httplong型）
	 * 日時からの解析で設定。
	 */
	private long timeLong;
	/**
	 * ID
	 * 日時からの解析で設定。
	 */
	private String id;
	/**
	 * ID末尾1文字
	 * 日時からの解析で設定。
	 */
	private String trailing;
	/**
	 * AUXSET？style.htmlに存在する項目。未使用？
	 */
	private String auxset;
	/**
	 * スレ内の同一名前での出現順。
	 */
	private int nameIndex;
	/**
	 * スレ内の同一名前での出現回数。
	 */
	private int nameCount;
	/**
	 * スレ内の同一ﾜｯﾁｮｲでの出現順。
	 */
	private int wacchoiIndex;
	/**
	 * スレ内の同一ﾜｯﾁｮｲでの出現回数。
	 */
	private int wacchoiCount;
	/**
	 * スレ内の同一ﾜｯﾁｮｲ下4桁での出現順。
	 */
	private int wacchoiLowerIndex;
	/**
	 * スレ内の同一ﾜｯﾁｮｲ下4桁での出現回数。
	 */
	private int wacchoiLowerCount;
	/**
	 * スレ内の同一IPでの出現順。
	 */
	private int ipIndex;
	/**
	 * スレ内の同一IPでの出現回数。
	 */
	private int ipCount;
	/**
	 * スレ内の同一IDでの出現順。
	 */
	private int idIndex;
	/**
	 * スレ内の同一IDでの出現回数。
	 */
	private int idCount;
	/**
	 * 本文
	 */
	private String body;
	/**
	 * 本文内の画像URL一覧。
	 * 本文からの解析で設定。
	 */
	private List<String> images;
	/**
	 * 同一名前のレス番のカンマ区切り。
	 */
	private String nameLink;
	/**
	 * 同一ﾜｯﾁｮｲのレス番のカンマ区切り。
	 */
	private String wacchoiLink;
	/**
	 * 同一ﾜｯﾁｮｲ下4桁のレス番のカンマ区切り。
	 */
	private String wacchoiLowerLink;
	/**
	 * 同一IPのレス番のカンマ区切り。
	 */
	private String ipLink;
	/**
	 * 同一IDのレス番のカンマ区切り。
	 */
	private String idLink;
	/**
	 * スレのタイトル。最初のレスに存在。
	 */
	private String title;
	/**
	 * あぼ～ん、非表示
	 */
	private AboneLevel abone = AboneLevel.NONE;
	/**
	 * TODO:あぼ～んの場合の元レス。
	 */
	private int aboneSource;
	/**
	 * あぼ～んの原因表示。
	 */
	private String aboneLabel;
	/**
	 * 参照元表示のための参照元。
	 */
	private Set<Integer> referredSet;
	/**
	 * 参照元のレス番のカンマ区切り。
	 */
	private String referredLink;
	/**
	 * 連鎖あぼ～んのための参照先。
	 */
	private Set<Integer> referSet;
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isAnonymous() {
		return anonymous;
	}
	public void setAnonymous(boolean anonymous) {
		this.anonymous = anonymous;
	}
	public String getWacchoi() {
		return wacchoi;
	}
	public void setWacchoi(String wacchoi) {
		this.wacchoi = wacchoi;
	}
	public String getWacchoiLower() {
		return wacchoiLower;
	}
	public void setWacchoiLower(String wacchoiLower) {
		this.wacchoiLower = wacchoiLower;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getIpTrailing() {
		return ipTrailing;
	}
	public void setIpTrailing(String ipTrailing) {
		this.ipTrailing = ipTrailing;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	public String getTimeIdAux() {
		return timeIdAux;
	}
	public void setTimeIdAux(String timeIdAux) {
		this.timeIdAux = timeIdAux;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public long getTimeLong() {
		return timeLong;
	}
	public void setTimeLong(long timeLong) {
		this.timeLong = timeLong;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTrailing() {
		return trailing;
	}
	public void setTrailing(String trailing) {
		this.trailing = trailing;
	}
	public String getAuxset() {
		return auxset;
	}
	public void setAuxset(String auxset) {
		this.auxset = auxset;
	}
	public int getNameIndex() {
		return nameIndex;
	}
	public void setNameIndex(int nameIndex) {
		this.nameIndex = nameIndex;
	}
	public int getNameCount() {
		return nameCount;
	}
	public void setNameCount(int nameCount) {
		this.nameCount = nameCount;
	}
	public int getWacchoiIndex() {
		return wacchoiIndex;
	}
	public void setWacchoiIndex(int wacchoiIndex) {
		this.wacchoiIndex = wacchoiIndex;
	}
	public int getWacchoiCount() {
		return wacchoiCount;
	}
	public void setWacchoiCount(int wacchoiCount) {
		this.wacchoiCount = wacchoiCount;
	}
	public int getWacchoiLowerIndex() {
		return wacchoiLowerIndex;
	}
	public void setWacchoiLowerIndex(int wacchoiLowerIndex) {
		this.wacchoiLowerIndex = wacchoiLowerIndex;
	}
	public int getWacchoiLowerCount() {
		return wacchoiLowerCount;
	}
	public void setWacchoiLowerCount(int wacchoiLowerCount) {
		this.wacchoiLowerCount = wacchoiLowerCount;
	}
	public int getIpIndex() {
		return ipIndex;
	}
	public void setIpIndex(int ipIndex) {
		this.ipIndex = ipIndex;
	}
	public int getIpCount() {
		return ipCount;
	}
	public void setIpCount(int ipCount) {
		this.ipCount = ipCount;
	}
	public int getIdIndex() {
		return idIndex;
	}
	public void setIdIndex(int idIndex) {
		this.idIndex = idIndex;
	}
	public int getIdCount() {
		return idCount;
	}
	public void setIdCount(int idCount) {
		this.idCount = idCount;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public List<String> getImages() {
		return images;
	}
	public void setImages(List<String> images) {
		this.images = images;
	}
	public String getNameLink() {
		return nameLink;
	}
	public void setNameLink(String nameLink) {
		this.nameLink = nameLink;
	}
	public String getWacchoiLink() {
		return wacchoiLink;
	}
	public void setWacchoiLink(String wacchoiLink) {
		this.wacchoiLink = wacchoiLink;
	}
	public String getWacchoiLowerLink() {
		return wacchoiLowerLink;
	}
	public void setWacchoiLowerLink(String wacchoiLowerLink) {
		this.wacchoiLowerLink = wacchoiLowerLink;
	}
	public String getIpLink() {
		return ipLink;
	}
	public void setIpLink(String ipLink) {
		this.ipLink = ipLink;
	}
	public String getIdLink() {
		return idLink;
	}
	public void setIdLink(String idLink) {
		this.idLink = idLink;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public AboneLevel getAbone() {
		return abone;
	}
	public void setAbone(AboneLevel abone) {
		this.abone = abone;
	}
	public int getAboneSource() {
		return aboneSource;
	}
	public void setAboneSource(int aboneSource) {
		this.aboneSource = aboneSource;
	}
	public String getAboneLabel() {
		return aboneLabel;
	}
	public void setAboneLabel(String aboneLabel) {
		this.aboneLabel = aboneLabel;
	}
	public Set<Integer> getReferredSet() {
		return referredSet;
	}
	public void setReferredSet(Set<Integer> referredSet) {
		this.referredSet = referredSet;
	}
	public String getReferredLink() {
		return referredLink;
	}
	public void setReferredLink(String referredLink) {
		this.referredLink = referredLink;
	}
	public Set<Integer> getReferSet() {
		return referSet;
	}
	public void setReferSet(Set<Integer> referSet) {
		this.referSet = referSet;
	}
}
