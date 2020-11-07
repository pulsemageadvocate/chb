package pulad.chb.interfaces;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadResponseDto;

/**
 * レスを取得するBBS固有の処理。
 * @author pulad
 *
 */
public interface ThreadLoader {
	/**
	 * datファイルを読み込む。
	 * 引数のBufferedReaderはメソッド内でcloseするので呼び出し元でcloseする必要は無い。
	 * @param setting 板のsetting.txt。
	 * @param res レスを取得するTreeMap。
	 * @param br datファイルのBufferedReader。
	 * @throws IOException
	 */
	void readDat(ConcurrentHashMap<String, String> setting, TreeMap<Integer, ResDto> res, BufferedReader br) throws IOException;
	/**
	 * レスをリモートから取得する。
	 * @param res レスを取得するTreeMap。
	 * @param now タイムスタンプ。
	 * @return
	 * @throws IOException
	 */
	ThreadResponseDto request(TreeMap<Integer, ResDto> res, long now) throws IOException;
}
