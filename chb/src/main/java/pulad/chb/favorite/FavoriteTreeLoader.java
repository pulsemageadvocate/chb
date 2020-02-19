package pulad.chb.favorite;

import org.thymeleaf.util.StringUtils;

import pulad.chb.App;
import pulad.chb.board.BoardManager;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ThreadDto;

public class FavoriteTreeLoader extends TreeLoader {

	public FavoriteTreeLoader(App app) {
		super(app);
	}

	@Override
	protected TreeItemType getTreeItemType(String[] token) {
		switch (token.length) {
		case 2:
			// 板
			return TreeItemType.Board;
		case 3:
		case 4:
			if ("C".equals(token[1]) || "E".equals(token[1])) {
				// フォルダ
				return TreeItemType.Folder;
			} else {
				// スレ
				return TreeItemType.Thread;
			}
		default:
			return TreeItemType.Unknown;
		}
	}

	@Override
	protected String getBoardUrl(String[] token) {
		return token[1];
	}

	@Override
	protected String getDatFileName(String[] token) {
		return token[2];
	}

	@Override
	protected String getName(String[] token) {
		switch (getTreeItemType(token)) {
		case Board:
		{
			// BoardManagerから取得する
			String boardUrl = getBoardUrl(token);
			BoardDto boardDto = BoardManager.get(boardUrl, false);
			return (boardDto == null) ? "" : boardDto.getTitleOrig();
		}
		case Folder:
			return token[2];
		case Thread:
		{
			if (!StringUtils.isEmpty(token[3])) {
				return token[3];
			}
			// BoardManagerから取得する
			String boardUrl = getBoardUrl(token);
			String datFileName = getDatFileName(token);
			BoardDto boardDto = BoardManager.get(boardUrl, false);
			if (boardDto == null) {
				return datFileName;
			}
			ThreadDto threadDto = boardDto.getLogThread().get(datFileName);
			return (threadDto == null) ? datFileName : threadDto.getTitle();
		}
		default:
			return "";
		}
	}
}
