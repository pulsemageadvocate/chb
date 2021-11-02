package pulad.chb;

import java.util.ArrayList;
import java.util.List;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.constant.TreeItemType;
import pulad.chb.dto.TreeItemDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.bbsmenu.BBSMenuTreeLoader;
import pulad.chb.util.FileUtil;

/**
 * 5chの板一覧
 * @author pulad
 *
 */
public class BBSMenuTreeProcessor {

	/**
	 * 5chの板一覧を読み込む。
	 * @param tab
	 * @param app
	 */
	public static void open(Tab tab, App app) {
		tab.setClosable(false);

		BBS bbsObject = BBSManager.getBBSFromLogDirectoryName("2ch_");
		if (bbsObject == null) {
			// 5ch拡張が無い
			return;
		}
		String bbs = bbsObject.getBBSDirectoryName();
		TreeView<TreeItemDto> tree = new BBSMenuTreeLoader(app).load(tab, FileUtil.realCapitalPath(Config.getBBSFolder().resolve(bbs).resolve("bbstree.txt")));
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
				switch (event.getButton()) {
				case PRIMARY:
					if (event.getClickCount() == 2) {
						TreeItem<TreeItemDto> selectedItem = tree.getSelectionModel().getSelectedItem();
						if (selectedItem == null) {
							return;
						}
						TreeItemDto dto = selectedItem.getValue();
						switch (dto.getType()) {
						case Board:
							String boardUrl = dto.getBoardUrl();
							app.openUrl(boardUrl);
							break;
						case Thread:
							boardUrl = dto.getBoardUrl();
							BBS bbsObject = BBSManager.getBBSFromUrl(boardUrl);
							app.openUrl(bbsObject.getThreadUrlFromBoardUrlAndDatFileName(boardUrl, dto.getDatFileName()));
							break;
						default:
							break;
						}
					}
					break;
				case SECONDARY:
					List<MenuItem> itemList = new ArrayList<MenuItem>();
					MenuItem item = new MenuItem("板一覧の更新");
					item.setOnAction(e -> {
						BoardListLoadProcessor.request(tab, app);
					});
					itemList.add(item);
					ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
					menu.setAutoHide(true);
					menu.show(tab.getTabPane().getScene().getWindow(), event.getScreenX(), event.getScreenY());
					break;
				default:
					break;
				}
			}
		});
	}

	/**
	 * 板一覧の更新後にツリーを更新する。
	 * @param tab
	 * @param app
	 */
	public static void reload(Tab tab, App app) {
		open(tab, app);
	}
	
}
