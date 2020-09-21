package pulad.chb.dto;

import java.io.Serializable;

import pulad.chb.constant.ChainIdentifier;

/**
 * NG設定の基底クラス。
 * @author pulad
 *
 */
public abstract class AbstractAboneDto implements Serializable {
	/**
	 * ホワイトリスト
	 */
	private boolean white;
	/**
	 * 透明あぼ～ん
	 */
	private boolean invisible;
	/**
	 * ラベル
	 */
	private String label;
	/**
	 * 連鎖の範囲
	 */
	private ChainIdentifier chainIdentifier;
	/**
	 * 参照連鎖数。数値は未実装で0か1以上かのみ有効。
	 */
	private int referenceChain;
	/**
	 * NG作成日。有効期限の起点。
	 */
	private long createDate;
	/**
	 * 有効期限（日）。前後〇日間有効。
	 */
	private int durationDay;
	public boolean isWhite() {
		return white;
	}
	public void setWhite(boolean white) {
		this.white = white;
	}
	public boolean isInvisible() {
		return invisible;
	}
	public void setInvisible(boolean invisible) {
		this.invisible = invisible;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public ChainIdentifier getChainIdentifier() {
		return chainIdentifier;
	}
	public void setChainIdentifier(ChainIdentifier chainIdentifier) {
		this.chainIdentifier = chainIdentifier;
	}
	public int getReferenceChain() {
		return referenceChain;
	}
	public void setReferenceChain(int referenceChain) {
		this.referenceChain = referenceChain;
	}
	public long getCreateDate() {
		return createDate;
	}
	public void setCreateDate(long createDate) {
		this.createDate = createDate;
	}
	public int getDurationDay() {
		return durationDay;
	}
	public void setDurationDay(int durationDay) {
		this.durationDay = durationDay;
	}
}
