package pulad.chb.interfaces;

import java.util.TreeMap;

import pulad.chb.dto.ResDto;

public interface ResProcessor {
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now);
}