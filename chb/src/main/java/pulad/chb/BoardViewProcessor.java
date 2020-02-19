package pulad.chb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import pulad.chb.bbs.BBS;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ThreadDto;
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
	 */
	public static void open(Tab tab, App app, String url) {
		open(tab, app, url, true);
	}

	/**
	 * 板を開く。
	 * @param tab
	 * @param app
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 */
	public static void open(Tab tab, App app, String url, boolean remote) {
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
		tableView.getSortOrder().add(newCountColumn);

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
						Path boardPath = App.logFolder.resolve(bbs).resolve(board);
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
		BoardLoadService service = new BoardLoadService(url, !App.offline && remote);
		service.setOnSucceeded(new BoardLoadSucceededEventHandler(tab));
		service.start();
	}

	/**
	 * 板を再読み込みする。
	 * @param tab
	 * @param app
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 */
	public static void reload(Tab tab, App app, String url, boolean remote) {
		// ここでremoteにオフラインボタンの状態を加味する。
		BoardLoadService service = new BoardLoadService(url, !App.offline && remote);
		service.setOnSucceeded(new BoardLoadSucceededEventHandler(tab));
		service.start();
	}

	private static class BoardLoadService extends Service<BoardDto> {
		private String url;
		private boolean remote;

		public BoardLoadService(String url, boolean remote) {
			this.url = url;
			this.remote = remote;
		}

		@Override
		protected Task<BoardDto> createTask() {
			BoardLoadTask task = new BoardLoadTask(url, remote);
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
			BoardDto boardDto = (BoardDto) event.getSource().getValue();
			if (boardDto == null) {
				return;
			}

			@SuppressWarnings("unchecked")
			TableView<ThreadDto> tableView = (TableView<ThreadDto>) tab.getContent();
			ObservableList<ThreadDto> thread = tableView.getItems();
			thread.clear();
			thread.addAll(boardDto.getThread());
			tableView.sort();

			tab.setText(boardDto.getTitleOrig());
			tableView.requestFocus();
		}
	}
}
