package pulad.chb.dto;

import java.io.Serializable;

public class AboneImageDto extends AbstractAboneDto implements Serializable {
	private String hash;
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
}
