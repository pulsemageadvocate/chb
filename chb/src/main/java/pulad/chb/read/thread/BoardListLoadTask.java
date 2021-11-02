package pulad.chb.read.thread;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javafx.concurrent.Task;
import pulad.chb.App;
import pulad.chb.config.Config;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.BoardListLoader;
import pulad.chb.util.FileUtil;

/**
 * 板一覧の更新タスク。
 * @author pulad
 *
 */
public class BoardListLoadTask extends Task<Boolean> {
	private BBS bbsObject;

	public BoardListLoadTask(BBS bbsObject) {
		this.bbsObject = bbsObject;
	}

	@Override
	protected Boolean call() {
		try {
			if (this.isCancelled()) {
				throw new InterruptedException();
			}

			String bbs = bbsObject.getBBSDirectoryName();
			String boardListUrl = Config.boardListUrl.get(bbs);
			if (boardListUrl == null) {
				App.logger.error("BoardListLoadTask URL定義無し");
				return Boolean.FALSE;
			}
			BoardListLoader boardListLoader = bbsObject.createBoardListLoader(boardListUrl);

			String list = boardListLoader.request();

			Files.writeString(FileUtil.realCapitalPath(Config.getBBSFolder().resolve(bbs).resolve("bbstree.txt")), list, StandardCharsets.UTF_8);
			return Boolean.TRUE;
		} catch (Exception e) {
			App.logger.error("BoardListLoadTask失敗", e);
			return Boolean.FALSE;
		}
	}
}
