package pulad.chb.interfaces;

import java.io.IOException;

/**
 * 板一覧を取得するBBS固有の処理。
 * @author pulad
 *
 */
public interface BoardListLoader {
	/**
	 * 板一覧を要求する。
	 * 板一覧をbbsmenu.txtの形式で返す。
	 * @return
	 * @throws IOException
	 */
	String request() throws IOException;
}
