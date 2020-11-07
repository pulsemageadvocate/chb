package pulad.chb.dto;

import java.io.Serializable;

/**
 * 検索条件を表す。
 * @author pulad
 *
 */
public class SearchConditionDto implements Serializable {
	private String text;
	private boolean textAa;
	private boolean textRe;
	private String directory;
	private String title;
	private boolean titleAa;
	private boolean titleRe;
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public boolean isTextAa() {
		return textAa;
	}
	public void setTextAa(boolean textAa) {
		this.textAa = textAa;
	}
	public boolean isTextRe() {
		return textRe;
	}
	public void setTextRe(boolean textRe) {
		this.textRe = textRe;
	}
	public String getDirectory() {
		return directory;
	}
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public boolean isTitleAa() {
		return titleAa;
	}
	public void setTitleAa(boolean titleAa) {
		this.titleAa = titleAa;
	}
	public boolean isTitleRe() {
		return titleRe;
	}
	public void setTitleRe(boolean titleRe) {
		this.titleRe = titleRe;
	}
}
