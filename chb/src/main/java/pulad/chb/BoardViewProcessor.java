package pulad.chb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.thymeleaf.util.StringUtils;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.config.Config;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.BoardLoadTaskResponseDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.board.BoardLoadTask;
import pulad.chb.util.ResCountComparator;

/**
 * 板を開く処理。
 * @author pulad
 *
 */
public class BoardViewProcessor {
	
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm");

	/**
	 * 板を開く。
	 * @param tab
	 * @param app
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 * @param pastLog 過去ログ表示
	 */
	public static void open(Tab tab, App app, String url, boolean remote, boolean pastLog) {
		TableView<ThreadDto> tableView = new TableView<>();
		tableView.getStyleClass().add("boardView");
//		TableColumn<ThreadDto, Integer> hasLogColumn = new TableColumn<>("!");
//		hasLogColumn.getStyleClass().addAll("boardView_hasLog");
//		hasLogColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
//			@Override
//			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
//				return new ReadOnlyObjectWrapper<Integer>((p.getValue().getLogCount() == 0) ? 0 : -1);
//			}
//		});
//		tableView.getColumns().add(hasLogColumn);
		TableColumn<ThreadDto, Integer> numberColumn = new TableColumn<>("No.");
		numberColumn.getStyleClass().addAll("boardView_number", "rightColumn", "numberColumn");
		numberColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				return new ReadOnlyObjectWrapper<Integer>(p.getValue().getNumber());
			}
		});
		tableView.getColumns().add(numberColumn);
		TableColumn<ThreadDto, String> titleColumn = new TableColumn<>("タイトル");
		titleColumn.getStyleClass().addAll("boardView_title");
		titleColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<ThreadDto, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getTitle());
			}
		});
		tableView.getColumns().add(titleColumn);
		TableColumn<ThreadDto, Integer> resCountColumn = new TableColumn<>("レス");
		resCountColumn.getStyleClass().addAll("boardView_resCount", "rightColumn", "numberColumn");
		resCountColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				int i = p.getValue().getResCount();
				return new ReadOnlyObjectWrapper<Integer>((i == 0) ? null : i);
			}
		});
		resCountColumn.setComparator(new ResCountComparator());
		tableView.getColumns().add(resCountColumn);
		TableColumn<ThreadDto, Integer> newCountColumn = new TableColumn<>("未読");
		newCountColumn.getStyleClass().addAll("boardView_newCount", "rightColumn", "numberColumn");
		newCountColumn.setStyle("-fx-font-color: RED;");
		newCountColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				ThreadDto dto = p.getValue();
				int res = dto.getResCount();
				int log = dto.getLogCount();
				return new ReadOnlyObjectWrapper<Integer>((log == 0) ? null : (res - log));
			}
		});
		newCountColumn.setComparator(new ResCountComparator());
		tableView.getColumns().add(newCountColumn);
		TableColumn<ThreadDto, Integer> logCountColumn = new TableColumn<>("既得");
		logCountColumn.getStyleClass().addAll("boardView_logCount", "rightColumn", "numberColumn");
		logCountColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				int i = p.getValue().getLogCount();
				return new ReadOnlyObjectWrapper<Integer>((i == 0) ? null : i);
			}
		});
		logCountColumn.setComparator(new ResCountComparator());
		tableView.getColumns().add(logCountColumn);
		TableColumn<ThreadDto, String> buildTimeColumn = new TableColumn<>("スレ立");
		buildTimeColumn.getStyleClass().addAll("boardView_buildTime");
		buildTimeColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<ThreadDto, String> p) {
				LocalDateTime d = p.getValue().getBuildTime();
				return new ReadOnlyObjectWrapper<String>((d == null) ? "" : d.format(dateTimeFormatter));
			}
		});
		tableView.getColumns().add(buildTimeColumn);

		if (pastLog) {
			TableColumn<ThreadDto, String> lastResColumn = new TableColumn<>("最終レス");
			lastResColumn.getStyleClass().addAll("boardView_lastRes");
			lastResColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, String>, ObservableValue<String>>() {
				@Override
				public ObservableValue<String> call(CellDataFeatures<ThreadDto, String> p) {
					LocalDateTime d = p.getValue().gettLast();
					if (d == null) {
						d = p.getValue().getBuildTime();
					}
					return new ReadOnlyObjectWrapper<String>((d == null) ? "" : d.format(dateTimeFormatter));
				}
			});
			lastResColumn.setComparator(Comparator.nullsLast(Comparator.reverseOrder()));
			tableView.getColumns().add(lastResColumn);
			tableView.getSortOrder().add(lastResColumn);
		} else {
			tableView.getSortOrder().add(newCountColumn);
		}

		tab.setContent(tableView);
		// 検索テキスト入力時
		Consumer<String> f = text -> {
			if (StringUtils.isEmpty(text)) {
				return;
			}
			List<ThreadDto> items = tableView.getItems();
			TableViewSelectionModel<ThreadDto> selection = tableView.getSelectionModel();
			int selectedIndex = selection.getSelectedIndex();
			// -1でもそのままでよい
			for (int i = selectedIndex + 1; i < items.size(); i++) {
				ThreadDto dto = items.get(i);
				if (dto.getTitle().contains(text)) {
					selection.focus(i);
					selection.clearAndSelect(i);
					tableView.scrollTo(i);
					return;
				}
			}
			for (int i = 0; i <= selectedIndex; i++) {
				ThreadDto dto = items.get(i);
				if (dto.getTitle().contains(text)) {
					selection.focus(i);
					selection.clearAndSelect(i);
					tableView.scrollTo(i);
					return;
				}
			}
			selection.clearSelection();
		};
		tableView.getProperties().put(App.PROPERTY_SEARCH_FUNCTION, f);

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
						app.openThread(bbsObject.getThreadUrlFromBoardUrlAndDatFileName(dto.getBoardUrl(), dto.getDatName()));
					}
					break;
				case SECONDARY:
					// 右ボタンクリック
					ThreadDto dto = tableView.getSelectionModel().getSelectedItem();
					if (dto == null) {
						return;
					}

					List<MenuItem> itemList = new ArrayList<MenuItem>();
					MenuItem item = new MenuItem("ログを削除");
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

		public BoardLoadService(String url, boolean remote, boolean replaceEmoji, boolean pastLog) {
			this.url = url;
			this.remote = remote;
			this.replaceEmoji = replaceEmoji;
			this.pastLog = pastLog;
		}

		@Override
		protected Task<BoardLoadTaskResponseDto> createTask() {
			BoardLoadTask task = new BoardLoadTask(url, remote, replaceEmoji, pastLog);
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

		public BoardLoadSucceededEventHandler(Tab tab) {
			this.tab = tab;
		}

		@Override
		public void handle(WorkerStateEvent event) {
			BoardLoadTaskResponseDto boardLoadTaskResponseDto = (BoardLoadTaskResponseDto) event.getSource().getValue();
			tab.getProperties().put(App.TAB_PROPERTY_STATUS_ERROR, boardLoadTaskResponseDto.getErrorMessage());
			App.getInstance().notifyChangeStatus();
			BoardDto boardDto = boardLoadTaskResponseDto.getDto();
			if (boardDto == null) {
				return;
			}

			@SuppressWarnings("unchecked")
			TableView<ThreadDto> tableView = (TableView<ThreadDto>) tab.getContent();
			ObservableList<ThreadDto> thread = tableView.getItems();
			thread.clear();
			thread.addAll(boardDto.getThread());
			tableView.sort();

			boolean pastLog = (Boolean) tab.getProperties().getOrDefault(App.TAB_PROPERTY_PAST, Boolean.FALSE);
			tab.setText((pastLog ? "(過去)" : "") + boardDto.getTitleOrig());
			tableView.requestFocus();
		}
	}
}
