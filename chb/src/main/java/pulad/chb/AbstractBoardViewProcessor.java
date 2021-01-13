package pulad.chb;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.thymeleaf.util.StringUtils;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.util.Callback;
import pulad.chb.dto.ThreadDto;
import pulad.chb.file.Threadst;
import pulad.chb.util.ResCountComparator;

/**
 * スレ一覧を表示するビューの基底クラス。
 * @author pulad
 *
 */
public abstract class AbstractBoardViewProcessor {
	protected static final String COLUMN_NO = "no";
	protected static final String COLUMN_TITLE = "title";
	protected static final String COLUMN_RES_COUNT = "resCount";
	protected static final String COLUMN_NEW_COUNT = "newCount";
	protected static final String COLUMN_LOG_COUNT = "logCount";
	protected static final String COLUMN_BUILD_TIME = "buildTime";
	protected static final String COLUMN_LAST_RES = "lastRes";
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm");

	/**
	 * スレ一覧のTableViewを作成する。
	 * @return
	 */
	protected static TableView<ThreadDto> createTable() {
		TableView<ThreadDto> tableView = new TableView<>();
		tableView.getStyleClass().add("boardView");
		TableColumn<ThreadDto, Integer> numberColumn = new TableColumn<>("No.");
		numberColumn.setId(COLUMN_NO);
		numberColumn.getStyleClass().addAll("boardView_number", "rightColumn", "numberColumn");
		numberColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				int i = p.getValue().getNumber();
				return new ReadOnlyObjectWrapper<Integer>((i == 0) ? null : Integer.valueOf(i));
			}
		});
		tableView.getColumns().add(numberColumn);
		TableColumn<ThreadDto, String> titleColumn = new TableColumn<>("タイトル");
		titleColumn.setId(COLUMN_TITLE);
		titleColumn.getStyleClass().addAll("boardView_title");
		titleColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<ThreadDto, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getTitle());
			}
		});
		tableView.getColumns().add(titleColumn);
		TableColumn<ThreadDto, Integer> resCountColumn = new TableColumn<>("レス");
		resCountColumn.setId(COLUMN_RES_COUNT);
		resCountColumn.getStyleClass().addAll("boardView_resCount", "rightColumn", "numberColumn");
		resCountColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				int i = p.getValue().getResCount();
				return new ReadOnlyObjectWrapper<Integer>((i == 0) ? null : Integer.valueOf(i));
			}
		});
		resCountColumn.setComparator(new ResCountComparator());
		tableView.getColumns().add(resCountColumn);
		TableColumn<ThreadDto, Integer> newCountColumn = new TableColumn<>("未読");
		newCountColumn.setId(COLUMN_NEW_COUNT);
		newCountColumn.getStyleClass().addAll("boardView_newCount", "rightColumn", "numberColumn");
		newCountColumn.setStyle("-fx-font-color: RED;");
		newCountColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				ThreadDto dto = p.getValue();
				int res = dto.getResCount();
				int log = dto.getLogCount();
				return new ReadOnlyObjectWrapper<Integer>((log == 0 || ((dto.getState() & Threadst.STATE_OLD) == Threadst.STATE_OLD)) ? null : Integer.valueOf(res - log));
			}
		});
		newCountColumn.setComparator(new ResCountComparator());
		tableView.getColumns().add(newCountColumn);
		TableColumn<ThreadDto, Integer> logCountColumn = new TableColumn<>("既得");
		logCountColumn.setId(COLUMN_LOG_COUNT);
		logCountColumn.getStyleClass().addAll("boardView_logCount", "rightColumn", "numberColumn");
		logCountColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<ThreadDto, Integer> p) {
				int i = p.getValue().getLogCount();
				return new ReadOnlyObjectWrapper<Integer>((i == 0) ? null : Integer.valueOf(i));
			}
		});
		logCountColumn.setComparator(new ResCountComparator());
		tableView.getColumns().add(logCountColumn);
		TableColumn<ThreadDto, String> buildTimeColumn = new TableColumn<>("スレ立");
		buildTimeColumn.setId(COLUMN_BUILD_TIME);
		buildTimeColumn.getStyleClass().addAll("boardView_buildTime");
		buildTimeColumn.setCellValueFactory(new Callback<CellDataFeatures<ThreadDto, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<ThreadDto, String> p) {
				LocalDateTime d = p.getValue().getBuildTime();
				return new ReadOnlyObjectWrapper<String>((d == null) ? "" : d.format(dateTimeFormatter));
			}
		});
		tableView.getColumns().add(buildTimeColumn);
		TableColumn<ThreadDto, String> lastResColumn = new TableColumn<>("最終レス");
		lastResColumn.setId(COLUMN_LAST_RES);
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

		// 検索テキスト入力時
		Consumer<String> f = text -> {
			if (StringUtils.isEmpty(text)) {
				return;
			}
			final Pattern regSubject = Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
			List<ThreadDto> items = tableView.getItems();
			TableViewSelectionModel<ThreadDto> selection = tableView.getSelectionModel();
			int selectedIndex = selection.getSelectedIndex();
			// -1でもそのままでよい
			for (int i = selectedIndex + 1; i < items.size(); i++) {
				ThreadDto dto = items.get(i);
				Matcher matcher = regSubject.matcher(dto.getTitle());
				if (matcher.find()) {
					selection.focus(i);
					selection.clearAndSelect(i);
					tableView.scrollTo(i);
					return;
				}
			}
			for (int i = 0; i <= selectedIndex; i++) {
				ThreadDto dto = items.get(i);
				Matcher matcher = regSubject.matcher(dto.getTitle());
				if (matcher.find()) {
					selection.focus(i);
					selection.clearAndSelect(i);
					tableView.scrollTo(i);
					return;
				}
			}
			selection.clearSelection();
		};
		tableView.getProperties().put(App.PROPERTY_SEARCH_FUNCTION, f);

		return tableView;
	}

	/**
	 * TableColumnを取得する。
	 * @param tableView
	 * @param id
	 * @return
	 */
	protected static TableColumn<ThreadDto, ?> getColumn(TableView<ThreadDto> tableView, String id) {
		return tableView.getColumns().stream().filter(x -> id.equals(x.getId())).findFirst().get();
	}
}
