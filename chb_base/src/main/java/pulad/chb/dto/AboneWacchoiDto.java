package pulad.chb.dto;

import java.io.Serializable;

public class AboneWacchoiDto extends AbstractAboneDto implements Serializable {
	private String word;
	private boolean regex;
	public String getWord() {
		return word;
	}
	public void setWord(String word) {
		this.word = word;
	}
	public boolean isRegex() {
		return regex;
	}
	public void setRegex(boolean regex) {
		this.regex = regex;
	}
}
