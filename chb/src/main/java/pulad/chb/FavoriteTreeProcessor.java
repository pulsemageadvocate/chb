package pulad.chb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import pulad.chb.bbs.BBS;
import pulad.chb.bbs.BBSManager;
import pulad.chb.dto.TreeItemDto;
import pulad.chb.favorite.FavoriteTreeLoader;
import pulad.chb.favorite.TreeItemType;
import pulad.chb.util.FileUtil;

public class FavoriteTreeProcessor {

	public static void open(Tab tab, App app) {
		tab.setClosable(false);

		TreeView<TreeItemDto> tree = new FavoriteTreeLoader(app).load(tab, FileUtil.realCapitalPath(App.rootFolder.resolve("favorite.txt")));

		TreeItem<TreeItemDto> item = tree.getRoot();
		if (item == null) {
			// 読み込めない場合は初期化
			TreeItemDto dto = new TreeItemDto();
			dto.setType(TreeItemType.Folder);
			dto.setText("お気に入り");
			item = new TreeItem<TreeItemDto>(dto);
			item.setExpanded(true);
			tree.setRoot(item);
		} else {
		}
		tab.setContent(tree);

		tree.setOnMouseClicked(new ClickEventHandler(app, tree));
	}

	private static class ClickEventHandler implements EventHandler<MouseEvent> {
		private App app;
		private TreeView<TreeItemDto> tree;

		private ClickEventHandler(App app, TreeView<TreeItemDto> tree) {
			this.app = app;
			this.tree = tree;
		}

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
				break;
			case SECONDARY:
				TreeItem<TreeItemDto> selectedItem = tree.getSelectionModel().getSelectedItem();
				if (selectedItem == null) {
					return;
				}
				TreeItemDto dto = selectedItem.getValue();
				List<MenuItem> menuItemList = new ArrayList<MenuItem>();
				MenuItem menuItem;
				ContextMenu menu;
				switch (dto.getType()) {
				case Board:
				case Thread:
					menuItem = new MenuItem("削除（未実装）");
					menuItem.setOnAction(x -> {});
					menuItemList.add(menuItem);
					break;
				default:
					break;
				}
				menuItem = new MenuItem("favorite.txt編集");
				menuItem.setOnAction(x -> {
					try {
						Runtime.getRuntime().exec(App.editorCommand.replaceAll("\\$LINK", Matcher.quoteReplacement(FileUtil.realCapital(App.rootFolder.resolve("favorite.txt").toString()))));
					} catch (IOException e) {
						App.logger.error("favorite.txt編集失敗", e);
					}
				});
				menuItemList.add(menuItem);
				menu = new ContextMenu(menuItemList.toArray(new MenuItem[menuItemList.size()]));
				menu.setAutoHide(true);
				menu.show(tree.getScene().getWindow(), event.getScreenX(), event.getScreenY());
				break;
			default:
				break;
			}
		}
	}
}
