package pulad.chb.interfaces;

import java.util.TreeMap;

import pulad.chb.dto.ResDto;

/**
 * スレッドを読み込んだ後、htmlにする前にレスを加工する処理を実装する。
 * @author pulad
 *
 */
public interface ResProcessor {
	/**
	 * レスを加工する。
	 * @param url スレッドのURL
	 * @param res レス
	 * @param now 処理開始時刻
	 */
	public void process(String url, TreeMap<Integer, ResDto> res, long now);
}
