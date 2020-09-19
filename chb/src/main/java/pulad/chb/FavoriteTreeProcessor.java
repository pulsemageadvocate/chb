package pulad.chb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.constant.TreeItemType;
import pulad.chb.dto.TreeItemDto;
import pulad.chb.favorite.FavoriteManager;
import pulad.chb.favorite.FavoriteTreeLoader;
import pulad.chb.interfaces.BBS;
import pulad.chb.util.FileUtil;

public class FavoriteTreeProcessor {

	public static void open(Tab tab, App app) {
		tab.setClosable(false);

		TreeView<TreeItemDto> tree = new FavoriteTreeLoader(app).load(tab, FileUtil.realCapitalPath(Config.getRootFolder().resolve("favorite.txt")));
		tree.setCellFactory(new CellFactory());

		TreeItem<TreeItemDto> item = tree.getRoot();
		if (item == null) {
			// 読み込めない場合は初期化
			App.logger.warn("favorite.txtが読み込めないので初期化");
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

	private static class CellFactory implements Callback<TreeView<TreeItemDto>, TreeCell<TreeItemDto>> {
		private static final DataFormat CHB_FAVORITE = new DataFormat("application/chb-favorite");
		private static final String CLASS_CELL = "treeView_favorite";
		private static final String CLASS_INSERT_TOP = "treeView_insertTop";
		private static final String CLASS_INSERT_BOTTOM = "treeView_insertBottom";
		private static TreeCell<TreeItemDto> droppingCell = null;

		@Override
		public TreeCell<TreeItemDto> call(TreeView<TreeItemDto> tree) {
			TextFieldTreeCell<TreeItemDto> cell = new TextFieldTreeCell<TreeItemDto>();
			cell.getStyleClass().add(CLASS_CELL);
			cell.setOnDragDetected(event -> {
				TreeItem<TreeItemDto> draggedItem = cell.getTreeItem();
				if (draggedItem.getParent() == null) {
					return;
				}
				ClipboardContent map = new ClipboardContent();
				map.put(CHB_FAVORITE, cell.getIndex());
				Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
				db.setContent(map);
				db.setDragView(cell.snapshot(null, null));
				event.consume();
			});
			cell.setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if (!db.hasContent(CHB_FAVORITE)) {
					return;
				}
				Integer index = (Integer) db.getContent(CHB_FAVORITE);
				if (index == null) {
					return;
				}

				int dropIndex = cell.getIndex();
				if (dropIndex == index) {
					return;
				}
				event.acceptTransferModes(TransferMode.MOVE);
				if (droppingCell != null) {
					droppingCell.getStyleClass().removeAll(CLASS_INSERT_TOP, CLASS_INSERT_BOTTOM);
				}
				droppingCell = cell;

				TreeItem<TreeItemDto> dropTarget = cell.getTreeItem();
				switch (dropTarget.getValue().getType()) {
				case Folder:
					// フォルダにドロップしたら一番上に追加
					cell.getStyleClass().add(CLASS_INSERT_BOTTOM);
					break;
				case Board:
				case Thread:
				{
					// フォルダ以外の要素にドロップした場合、半分より上なら上に追加。半分より下なら下に追加。
					cell.getStyleClass().add((event.getY() / cell.getHeight() < 0.5d) ? CLASS_INSERT_TOP : CLASS_INSERT_BOTTOM);
				}
					break;
				default:
					return;
				}
			});
			cell.setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				if (!db.hasContent(CHB_FAVORITE)) {
					return;
				}
				Integer index = (Integer) db.getContent(CHB_FAVORITE);
				TreeItem<TreeItemDto> item = tree.getTreeItem(index);
				if (item == null) {
					return;
				}
				item.getParent().getChildren().remove(item);

				TreeItem<TreeItemDto> dropTarget = cell.getTreeItem();
				switch (dropTarget.getValue().getType()) {
				case Folder:
					// フォルダにドロップしたら一番上に追加
					dropTarget.getChildren().add(0, item);
					break;
				case Board:
				case Thread:
				{
					// フォルダ以外の要素にドロップした場合、半分より上なら上に追加。半分より下なら下に追加。
					ObservableList<TreeItem<TreeItemDto>> children = dropTarget.getParent().getChildren();
					children.add(children.indexOf(dropTarget) + ((event.getY() / cell.getHeight() < 0.5d) ? 0 : 1), item);
				}
					break;
				default:
					return;
				}
				FavoriteManager.writeFile(tree);
				event.setDropCompleted(true);
			});
			cell.setOnDragDone(event -> {
				if (droppingCell != null) {
					droppingCell.getStyleClass().removeAll(CLASS_INSERT_TOP, CLASS_INSERT_BOTTOM);
					droppingCell = null;
				}
			});
			return cell;
		}
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
						// 対応していないURLは処理しない
						String boardUrl = dto.getBoardUrl();
						BBS bbsObject = BBSManager.getBBSFromUrl(boardUrl);
						if (bbsObject != null) {
							app.openUrl(boardUrl);
						}
						break;
					case Thread:
						// 対応していないURLは処理しない
						boardUrl = dto.getBoardUrl();
						bbsObject = BBSManager.getBBSFromUrl(boardUrl);
						if (bbsObject != null) {
							app.openUrl(bbsObject.getThreadUrlFromBoardUrlAndDatFileName(boardUrl, dto.getDatFileName()));
						}
						break;
					default:
						break;
					}
				}
				break;
			case SECONDARY:
				int selectedIndex = tree.getSelectionModel().getSelectedIndex();
				if (selectedIndex < 0) {
					return;
				}
				List<MenuItem> menuItemList = new ArrayList<MenuItem>();
				MenuItem menuItem;
				ContextMenu menu;
				menuItem = new MenuItem("削除");
				menuItem.setOnAction(x -> {
					FavoriteManager.delete(tree, selectedIndex);
				});
				menuItemList.add(menuItem);
				menuItem = new MenuItem("favorite.txt編集");
				menuItem.setOnAction(x -> {
					try {
						Runtime.getRuntime().exec(Config.editorCommand.replaceAll("\\$LINK", Matcher.quoteReplacement(FileUtil.realCapital(Config.getRootFolder().resolve("favorite.txt").toString()))));
					} catch (IOException e) {
						App.logger.error("favorite.txt編集失敗", e);
					}
				});
				menuItemList.add(menuItem);
				menuItem = new MenuItem("再読込");
				menuItem.setOnAction(x -> {
					App.getInstance().reloadFavorite();
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
