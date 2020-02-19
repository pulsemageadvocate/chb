package pulad.chb.read.board;

import java.util.List;

import pulad.chb.dto.ThreadDto;

public interface ThreadProcessor {
	public void process(List<ThreadDto> list);
}
