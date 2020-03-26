package pulad.chb.dto;

import java.io.Serializable;

public class DownloadDto extends ResponseDto implements Serializable {
	private String imageCache;
	private String imageExt;
	private byte[] data;
	public String getImageCache() {
		return imageCache;
	}
	public void setImageCache(String imageCache) {
		this.imageCache = imageCache;
	}
	public String getImageExt() {
		return imageExt;
	}
	public void setImageExt(String imageExt) {
		this.imageExt = imageExt;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
}
