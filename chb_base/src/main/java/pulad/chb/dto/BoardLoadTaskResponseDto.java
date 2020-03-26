package pulad.chb.dto;

public class BoardLoadTaskResponseDto {
	private BoardDto dto;
	private String errorMessage;
	public BoardDto getDto() {
		return dto;
	}
	public void setDto(BoardDto dto) {
		this.dto = dto;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
}
