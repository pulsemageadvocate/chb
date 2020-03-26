package pulad.chb.dto;

import java.io.Serializable;

public class ConfigFileDto implements Serializable {
	private Boolean maximized;
	private Integer x;
	private Integer y;
	private Integer width;
	private Integer height;
	private String editor;
	public Boolean getMaximized() {
		return maximized;
	}
	public void setMaximized(Boolean maximized) {
		this.maximized = maximized;
	}
	public Integer getX() {
		return x;
	}
	public void setX(Integer x) {
		this.x = x;
	}
	public Integer getY() {
		return y;
	}
	public void setY(Integer y) {
		this.y = y;
	}
	public Integer getWidth() {
		return width;
	}
	public void setWidth(Integer width) {
		this.width = width;
	}
	public Integer getHeight() {
		return height;
	}
	public void setHeight(Integer height) {
		this.height = height;
	}
	public String getEditor() {
		return editor;
	}
	public void setEditor(String editor) {
		this.editor = editor;
	}
}
