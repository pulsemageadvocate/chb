package pulad.chb.read.bbsmenu;

import pulad.chb.App;
import pulad.chb.favorite.TreeItemType;
import pulad.chb.favorite.TreeLoader;

public class BBSMenuTreeLoader extends TreeLoader {

	public BBSMenuTreeLoader(App app) {
		super(app);
	}

	@Override
	protected TreeItemType getTreeItemType(String[] token) {
		if ("C".equals(token[1]) || "E".equals(token[1])) {
			// フォルダ
			return TreeItemType.Folder;
		} else {
			// 板
			return TreeItemType.Board;
		}
	}

	@Override
	protected String getBoardUrl(String[] token) {
		return token[1];
	}

	@Override
	protected String getDatFileName(String[] token) {
		return "";
	}

	@Override
	protected String getName(String[] token) {
		return token[2];
	}
}
