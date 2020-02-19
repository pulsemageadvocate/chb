package pulad.chb.dto;

import java.io.Serializable;

import pulad.chb.favorite.TreeItemType;

public class TreeItemDto implements Serializable {
	private TreeItemType type;
	private String text;
	private String boardUrl;
	private String datFileName;
	public TreeItemType getType() {
		return type;
	}
	public void setType(TreeItemType type) {
		this.type = type;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getBoardUrl() {
		return boardUrl;
	}
	public void setBoardUrl(String boardUrl) {
		this.boardUrl = boardUrl;
	}
	public String getDatFileName() {
		return datFileName;
	}
	public void setDatFileName(String datFileName) {
		this.datFileName = datFileName;
	}
	/**
	 * TreeViewのためにtextを返す。
	 */
	@Override
	public String toString() {
		return text;
	}
}
