package pulad.chb;

import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import pulad.chb.bbs.BBS;
import pulad.chb.bbs.BBSManager;
import pulad.chb.dto.TreeItemDto;
import pulad.chb.favorite.TreeItemType;
import pulad.chb.read.bbsmenu.BBSMenuTreeLoader;
import pulad.chb.util.FileUtil;

/**
 * 2chの板一覧
 * @author pulad
 *
 */
public class BBSMenuTreeProcessor {

	public static void open(Tab tab, App app) {
		tab.setClosable(false);

		BBS bbsObject = BBSManager.ch;
		String bbs = bbsObject.getBBSDirectoryName();
		TreeView<TreeItemDto> tree = new BBSMenuTreeLoader(app).load(tab, FileUtil.realCapitalPath(App.bbsFolder.resolve(bbs).resolve("bbstree.txt")));
		if (tree.getRoot() == null) {
			// 読み込めない場合は初期化
			TreeItemDto dto = new TreeItemDto();
			dto.setType(TreeItemType.Folder);
			dto.setText("2ch");
			TreeItem<TreeItemDto> item = new TreeItem<TreeItemDto>(dto);
			item.setExpanded(true);
			tree.setRoot(item);
		}
		tab.setContent(tree);

		tree.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (event.getClickCount() == 2) {
					TreeItem<TreeItemDto> selectedItem = tree.getSelectionModel().getSelectedItem();
					if (selectedItem == null) {
						return;
					}
					TreeItemDto dto = selectedItem.getValue();
					switch (dto.getType()) {
					case Board:
						String boardUrl = dto.getBoardUrl();
						app.openBoard(boardUrl);
						break;
					case Thread:
						boardUrl = dto.getBoardUrl();
						BBS bbsObject = BBSManager.getBBSFromUrl(boardUrl);
						app.openThread(bbsObject.getThreadUrlFromBoardUrlAndDatFileName(boardUrl, dto.getDatFileName()));
						break;
					default:
						break;
					}
				}
			}
		});
	}
	
}
