package pulad.chb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.config.Config;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.BoardLoadTaskResponseDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.board.BoardLoadTask;

/**
 * 板を開く処理。
 * @author pulad
 *
 */
public class BoardViewProcessor extends AbstractBoardViewProcessor {
	
	/**
	 * 板を開く。
	 * @param tab
	 * @param app
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 * @param pastLog 過去ログ表示
	 */
	public static void open(Tab tab, App app, String url, boolean remote, boolean pastLog) {
		TableView<ThreadDto> tableView = createTable();
		tableView.setCursor(Cursor.WAIT);

		TableColumn<ThreadDto, ?> lastResColumn = getColumn(tableView, COLUMN_LAST_RES);
		if (pastLog) {
			tableView.getSortOrder().add(lastResColumn);
		} else {
			TableColumn<ThreadDto, ?> newCountColumn = getColumn(tableView, COLUMN_NEW_COUNT);
			tableView.getColumns().remove(lastResColumn);
			tableView.getSortOrder().add(newCountColumn);
		}

		tab.setContent(tableView);

		tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				switch (event.getButton()) {
				case PRIMARY:
					if (event.getClickCount() == 2) {
						// 左ボタンダブルクリック
						ThreadDto dto = tableView.getSelectionModel().getSelectedItem();
						if (dto == null) {
							return;
						}

						BBS bbsObject = BBSManager.getBBSFromUrl(dto.getBoardUrl());
						app.openUrl(bbsObject.getThreadUrlFromBoardUrlAndDatFileName(dto.getBoardUrl(), dto.getDatName()));
					}
					break;
				case SECONDARY:
					// 右ボタンクリック
					ThreadDto dto = tableView.getSelectionModel().getSelectedItem();
					if (dto == null) {
						return;
					}

					List<MenuItem> itemList = new ArrayList<MenuItem>();
					MenuItem item = new MenuItem("板名とURLをコピー");
					item.setOnAction(x -> {
						BBS bbsObject = BBSManager.getBBSFromUrl(dto.getBoardUrl());
						String text = dto.getTitle().trim() + System.lineSeparator() +
								bbsObject.getThreadUrlFromBoardUrlAndDatFileName(dto.getBoardUrl(), dto.getDatName());
						HashMap<DataFormat, Object> content = new HashMap<>();
						content.put(DataFormat.PLAIN_TEXT, text);
						Clipboard.getSystemClipboard().setContent(content);
					});
					itemList.add(item);
					item = new MenuItem("ログを削除");
					item.setOnAction(x -> {
						String boardUrl = dto.getBoardUrl();
						BBS bbsObject = BBSManager.getBBSFromUrl(boardUrl);
						String bbs = bbsObject.getLogDirectoryName();
						String board = bbsObject.getBoardFromBoardUrl(boardUrl);
						String datName = dto.getDatName();
						Path boardPath = Config.getLogFolder().resolve(bbs).resolve(board);
						try {
							Files.deleteIfExists(boardPath.resolve(datName));
						} catch (IOException e) {
						}
						try {
							Files.deleteIfExists(boardPath.resolve(datName + ".chb.txt"));
						} catch (IOException e) {
						}

						BoardDto boardDto = BoardManager.get(boardUrl);
						ConcurrentHashMap<String, ThreadDto> logThread = boardDto.getLogThread();
						if (logThread.remove(datName) != null) {
							BoardManager.updateThreadst(boardDto);
						}

						dto.setLogCount(0);
						tableView.refresh();
					});
					itemList.add(item);
					ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
					menu.setAutoHide(true);
					menu.show(tableView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
					break;
				default:
					break;
				}
			}
		});

		// ここでremoteにオフラインボタンの状態を加味する。
		BoardLoadService service = new BoardLoadService(url, !App.offline && remote, App.replaceEmoji, pastLog);
		service.setOnSucceeded(new BoardLoadSucceededEventHandler(tab));
		service.start();
	}

	/**
	 * 板を再読み込みする。
	 * @param tab
	 * @param app
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 * @param pastLog 過去ログ表示
	 */
	public static void reload(Tab tab, App app, String url, boolean remote, boolean pastLog) {
		@SuppressWarnings("unchecked")
		TableView<ThreadDto> tableView = (TableView<ThreadDto>) tab.getContent();
		tableView.setCursor(Cursor.WAIT);

		// ここでremoteにオフラインボタンの状態を加味する。
		BoardLoadService service = new BoardLoadService(url, !App.offline && remote, App.replaceEmoji, pastLog);
		service.setOnSucceeded(new BoardLoadSucceededEventHandler(tab));
		service.start();
	}

	private static class BoardLoadService extends Service<BoardLoadTaskResponseDto> {
		private String url;
		private boolean remote;
		private boolean replaceEmoji;
		private boolean pastLog;

		private BoardLoadService(String url, boolean remote, boolean replaceEmoji, boolean pastLog) {
			this.url = url;
			this.remote = remote;
			this.replaceEmoji = replaceEmoji;
			this.pastLog = pastLog;
		}

		@Override
		protected Task<BoardLoadTaskResponseDto> createTask() {
			Task<BoardLoadTaskResponseDto> task = new BoardLoadTask(url, remote, replaceEmoji, pastLog);
			// なぜかこれを呼ぶとServiceのOnSucceededも呼ばれるようになる
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
				}
			});
			return task;
		}
	}

	private static class BoardLoadSucceededEventHandler implements EventHandler<WorkerStateEvent> {
		private Tab tab;

		private BoardLoadSucceededEventHandler(Tab tab) {
			this.tab = tab;
		}

		@Override
		public void handle(WorkerStateEvent event) {
			App.logger.debug("BoardLoadSucceededEventHandler start");

			@SuppressWarnings("unchecked")
			TableView<ThreadDto> tableView = (TableView<ThreadDto>) tab.getContent();
			tableView.setCursor(null);

			BoardLoadTaskResponseDto boardLoadTaskResponseDto = (BoardLoadTaskResponseDto) event.getSource().getValue();
			tab.getProperties().put(App.TAB_PROPERTY_STATUS_ERROR, boardLoadTaskResponseDto.getErrorMessage());
			App.getInstance().notifyChangeStatus();
			BoardDto boardDto = boardLoadTaskResponseDto.getDto();
			if (boardDto == null) {
				return;
			}

			ObservableList<ThreadDto> thread = tableView.getItems();
			thread.clear();
			thread.addAll(boardDto.getThread());
			tableView.sort();

			boolean pastLog = (Boolean) tab.getProperties().getOrDefault(App.TAB_PROPERTY_PAST, Boolean.FALSE);
			tab.setText((pastLog ? "(過去)" : "") + boardDto.getTitleOrig());
			tableView.requestFocus();

			App.logger.debug("BoardLoadSucceededEventHandler end");
		}
	}
}
