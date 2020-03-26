package pulad.chb.interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.TreeMap;

import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadResponseDto;

public interface ThreadLoader {
	void readDat(BoardDto boardDto, TreeMap<Integer, ResDto> res, BufferedReader br) throws IOException;
	ThreadResponseDto request(TreeMap<Integer, ResDto> res, long now) throws IOException;
}
