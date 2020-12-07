package pulad.chb.dto;

import java.io.Serializable;
import java.util.List;

public class NGFileDto implements Serializable {
	List<AboneNameDto> name;
	List<AboneWacchoiDto> wacchoi;
	List<AboneIPDto> ip;
	List<AboneIDDto> id;
	List<AboneBodyDto> body;
	List<AboneImageDto> image;
	public List<AboneNameDto> getName() {
		return name;
	}
	public void setName(List<AboneNameDto> name) {
		this.name = name;
	}
	public List<AboneWacchoiDto> getWacchoi() {
		return wacchoi;
	}
	public void setWacchoi(List<AboneWacchoiDto> wacchoi) {
		this.wacchoi = wacchoi;
	}
	public List<AboneIPDto> getIp() {
		return ip;
	}
	public void setIp(List<AboneIPDto> ip) {
		this.ip = ip;
	}
	public List<AboneIDDto> getId() {
		return id;
	}
	public void setId(List<AboneIDDto> id) {
		this.id = id;
	}
	public List<AboneBodyDto> getBody() {
		return body;
	}
	public void setBody(List<AboneBodyDto> body) {
		this.body = body;
	}
	public List<AboneImageDto> getImage() {
		return image;
	}
	public void setImage(List<AboneImageDto> image) {
		this.image = image;
	}
}
