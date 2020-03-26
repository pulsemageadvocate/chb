package pulad.chb.favorite;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import pulad.chb.App;
import pulad.chb.constant.TreeItemType;
import pulad.chb.dto.TreeItemDto;

public abstract class TreeLoader {
	protected App app;

	public TreeLoader(App app) {
		this.app = app;
	}

	public TreeView<TreeItemDto> load(Tab tab, Path filePath) {
		TreeView<TreeItemDto> tree = new TreeView<TreeItemDto>();

		// favorite.txt読み込み
		TreeItem<TreeItemDto> current = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filePath.toString(), Charset.forName("UTF-8")));
			String str = null;
			int currentDepth = 0;
			while ((str = br.readLine()) != null) {
				try {
					// ツリーの深さ、C、フォルダ名
					// ツリーの深さ、板URL、DATファイル名
					String[] token = str.split(",", 4);
					int depth = Integer.parseInt(token[0]);
	
					TreeItemType treeItemType = getTreeItemType(token);
					switch (treeItemType) {
					case Board:
						// 板
					{
						String boardUrl = getBoardUrl(token);
						// 名前は外部から取得する場合がある
						String name = getName(token);
						current = append(current, depth - currentDepth, treeItemType, name, boardUrl, null);
						currentDepth = depth;
						break;
					}
					case Folder:
						// フォルダ
					{
						String name = getName(token);
						if (depth == 0) {
							if (tree.getRoot() != null) {
								// 0が2つある場合は強制終了
								break;
							}
							TreeItemDto dto = new TreeItemDto();
							dto.setType(treeItemType);
							dto.setText(name);
							current = new TreeItem<TreeItemDto>(dto);
							current.setExpanded(true);
							tree.setRoot(current);
							currentDepth = 0;
						} else {
							current = append(current, depth - currentDepth, treeItemType, name, null, null);
							currentDepth = depth;
						}
						break;
					}
					case Thread:
						// スレ
					{
						String boardUrl = getBoardUrl(token);
						String datFileName = getDatFileName(token);
						// 名前は外部から取得する場合がある
						String name = getName(token);
						current = append(current, depth - currentDepth, treeItemType, name, boardUrl, datFileName);
						currentDepth = depth;
						break;
					}
					default:
						continue;
					}
				} catch (Exception e) {
					continue;
				}
			}
		} catch (Exception e) {
			// 読み込めない場合は初期化
			tree.setRoot(null);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					App.logger.error("load失敗", e);
				}
				br = null;
			}
		}

		return tree;
	}

	private TreeItem<TreeItemDto> append(TreeItem<TreeItemDto> previous, int depthChange, TreeItemType type, String text, String boardUrl, String datFileName) {
		TreeItem<TreeItemDto> current = previous;
		TreeItem<TreeItemDto> nextItem = null;
		TreeItemDto dto = null;
		if (depthChange > 0) {
			while (depthChange-- > 1) {
				dto = new TreeItemDto();
				dto.setType(TreeItemType.Folder);
				dto.setText("階層エラー");
				nextItem = new TreeItem<TreeItemDto>(dto);
				nextItem.setExpanded(true);
				current.getChildren().add(nextItem);
				current = nextItem;
			}
		} else {
			// 親を取得
			while (depthChange++ <= 0) {
				current = current.getParent();
			}
		}
		dto = new TreeItemDto();
		dto.setType(type);
		dto.setText(text);
		dto.setBoardUrl(boardUrl);
		dto.setDatFileName(datFileName);
		nextItem = new TreeItem<TreeItemDto>(dto);
		nextItem.setExpanded(true);
		current.getChildren().add(nextItem);
		return nextItem;
	}

	protected abstract TreeItemType getTreeItemType(String[] token);

	protected abstract String getBoardUrl(String[] token);

	protected abstract String getDatFileName(String[] token);

	protected abstract String getName(String[] token);
}
