package pulad.chb.favorite;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import pulad.chb.App;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.config.Config;
import pulad.chb.constant.TreeItemType;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.dto.TreeItemDto;
import pulad.chb.interfaces.BBS;

public class FavoriteManager {

	/**
	 * お気に入りを追加する。
	 * @param tree
	 * @param url
	 */
	public static void addFavorite(TreeView<TreeItemDto> tree, String url) {
		synchronized (FavoriteManager.class) {
			TreeItem<TreeItemDto> current = tree.getRoot();
			current.getChildren().add(createFromUrl(url));

			writeFile(tree);
		}
	}

	/**
	 * お気に入りツリーのエントリを作成する。
	 * @param url
	 * @return
	 */
	private static TreeItem<TreeItemDto> createFromUrl(String url) {
		BBS bbsObject = BBSManager.getBBSFromUrl(url);
		// NameはBoardManagerから取得する
		TreeItemDto dto = new TreeItemDto();
		if (bbsObject.isBoardUrl(url)) {
			BoardDto boardDto = BoardManager.get(url, false);
			dto.setType(TreeItemType.Board);
			dto.setText((boardDto == null) ? url : boardDto.getTitleOrig());
			dto.setBoardUrl(url);
		} else if (bbsObject.isThreadUrl(url)) {
			String boardUrl = bbsObject.getBoardUrlFromThreadUrl(url);
			String datFileName = bbsObject.getDatFileNameFromThreadUrl(url);
			BoardDto boardDto = BoardManager.get(boardUrl, false);
			String text = null;
			if (boardDto == null) {
				text = datFileName;
			} else {
				ThreadDto threadDto = boardDto.getLogThread().get(datFileName);
				text = (threadDto == null) ? datFileName : threadDto.getTitle();
			}

			dto.setType(TreeItemType.Thread);
			dto.setText(text);
			dto.setBoardUrl(boardUrl);
			dto.setDatFileName(datFileName);
		} else {
			return null;
		}
		TreeItem<TreeItemDto> treeItem = new TreeItem<TreeItemDto>(dto);
		treeItem.setExpanded(true);
		return treeItem;
	}

	/**
	 * お気に入りをファイルに書き込む。
	 * @param tree
	 */
	public static void writeFile(TreeView<TreeItemDto> tree) {
		Path path = Config.getRootFolder().resolve("favorite.txt");

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(path.toFile(), Charset.forName("UTF-8")));
			writeFile0(bw, tree.getRoot(), 0);
			bw.flush();
		} catch (IOException e) {
			App.logger.error("addFavorite失敗", e);
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
				}
				bw = null;
			}
		}
	}

	private static void writeFile0(BufferedWriter bw, TreeItem<TreeItemDto> current, int level) throws IOException {
		TreeItemDto dto = current.getValue();
		switch (dto.getType()) {
		case Folder:
			bw.write(Integer.toString(level));
			bw.write(",C,");
			bw.write(dto.getText());
			bw.newLine();
			int childLevel = level + 1;
			for (TreeItem<TreeItemDto> child : current.getChildren()) {
				writeFile0(bw, child, childLevel);
			}
			break;
		case Board:
			bw.write(Integer.toString(level));
			bw.write(",");
			bw.write(dto.getBoardUrl());
			bw.newLine();
			break;
		case Thread:
			bw.write(Integer.toString(level));
			bw.write(",");
			bw.write(dto.getBoardUrl());
			bw.write(",");
			bw.write(dto.getDatFileName());
			bw.write(",");
			bw.newLine();
			break;
		default:
			break;
		}
	}

	/**
	 * お気に入りを削除する。
	 * @param tree
	 * @param index
	 */
	public static void delete(TreeView<TreeItemDto> tree, int index) {
		synchronized (FavoriteManager.class) {
			TreeItem<TreeItemDto> target = tree.getTreeItem(index);
			target.getParent().getChildren().remove(target);

			writeFile(tree);
		}
	}

	private FavoriteManager() {}
}
