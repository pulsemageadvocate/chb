package pulad.chb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.StringUtils;

import javafx.collections.ObservableList;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import pulad.chb.bbs.BBSManager;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.SearchConditionDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.file.Threadst;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.ThreadLoader;
import pulad.chb.read.thread.ThreadLoadProcessor;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.RegexPredicate;
import pulad.chb.util.SimpleWordPredicate;

/**
 * 検索結果タブを開く処理。
 * @author pulad
 *
 */
public class SearchResultViewProcessor extends AbstractBoardViewProcessor {
	private static Logger logger = LoggerFactory.getLogger(SearchResultViewProcessor.class);

	/**
	 * 検索結果タブを開く。
	 * @param tab
	 */
	public static void open(Tab tab, SearchConditionDto dto) {
		TableView<ThreadDto> tableView = createTable();
		TableColumn<ThreadDto, ?> lastResColumn = getColumn(tableView, COLUMN_LAST_RES);
		tableView.getSortOrder().add(lastResColumn);

		tab.setContent(tableView);

		Predicate<Map.Entry<Integer, ResDto>> textPredicate = (!dto.isTextRe() && dto.isTextAa())
				? new SimpleWordPredicate<Map.Entry<Integer, ResDto>>(
						dto.getText(),
						x -> x.getValue().getBody())
				: new RegexPredicate<Map.Entry<Integer, ResDto>>(
						dto.isTextRe() ? dto.getText() : Pattern.quote(dto.getText()),
						dto.isTextAa() ? 0 : Pattern.CASE_INSENSITIVE,
						x -> x.getValue().getBody());
		// 正規表現ではなく大文字小文字を区別する場合は単純なテキストサーチを使用する。
		Predicate<ThreadDto> titlePredicate;
		if (StringUtils.isEmpty(dto.getTitle())) {
			titlePredicate = x -> true;
		} else if (!dto.isTitleRe() && dto.isTitleAa()) {
			titlePredicate = new SimpleWordPredicate<ThreadDto>(
					dto.getTitle(),
					ThreadDto::getTitle);
		} else {
			titlePredicate = new RegexPredicate<ThreadDto>(
					dto.isTitleRe() ? dto.getTitle() : Pattern.quote(dto.getTitle()),
					dto.isTitleAa() ? 0 : Pattern.CASE_INSENSITIVE,
					ThreadDto::getTitle);
		}
		ExecutorService executor = App.getInstance().getExecutor();
		DirectorySearchProcess directorySearchProcess = new DirectorySearchProcess(executor, tab, dto, titlePredicate, textPredicate);
		executor.execute(directorySearchProcess);
	}

	/**
	 * 検索結果タブを再読み込みする。
	 * @param tab
	 */
	public static void reload(Tab tab, SearchConditionDto dto) {
		open(tab, dto);
	}

	private static class DirectorySearchProcess implements Runnable {
		private ExecutorService executor;
		private Tab tab;
		private SearchConditionDto dto;
		private Predicate<ThreadDto> titlePredicate;
		private Predicate<Map.Entry<Integer, ResDto>> textPredicate;
		
		private DirectorySearchProcess(ExecutorService executor, Tab tab, SearchConditionDto dto, Predicate<ThreadDto> titlePredicate, Predicate<Map.Entry<Integer, ResDto>> textPredicate) {
			this.executor = executor;
			this.tab = tab;
			this.dto = dto;
			this.titlePredicate = titlePredicate;
			this.textPredicate = textPredicate;
		}

		@Override
		public void run() {
			try {
				processDirectory(Paths.get(dto.getDirectory()));
			} catch (Exception e) {
				logger.error("DirectorySearchProcess失敗", e);
				tab.getProperties().put(App.TAB_PROPERTY_STATUS_ERROR, e.toString());
				App.getInstance().notifyChangeStatus();
			}
		}

		private void processDirectory(Path dir) {
			if (Files.exists(dir.resolve("threadst.txt"))) {
				executor.execute(new ThreadSearchProcess(executor, tab, dir, titlePredicate, textPredicate));
			}
			try {
				Files.list(dir)
						.filter(x -> Files.isDirectory(x))
						.forEach(x -> processDirectory(x));
			} catch (IOException e) {
				logger.error("DirectorySearchProcess失敗: " + dir.toString(), e);
			}
		}
	}

	private static class ThreadSearchProcess implements Runnable {
		private ExecutorService executor;
		private Tab tab;
		private Path dir;
		private Predicate<ThreadDto> titlePredicate;
		private Predicate<Map.Entry<Integer, ResDto>> textPredicate;

		private ThreadSearchProcess(ExecutorService executor, Tab tab, Path dir, Predicate<ThreadDto> titlePredicate, Predicate<Map.Entry<Integer, ResDto>> textPredicate) {
			this.executor = executor;
			this.tab = tab;
			this.dir = dir;
			this.titlePredicate = titlePredicate;
			this.textPredicate = textPredicate;
		}

		@Override
		public void run() {
			try {
				// 板共通のものは使いまわす（スレッドセーフ必須）
				// threadst.txt
				ConcurrentHashMap<String, ThreadDto> thread = Threadst.read(dir);
				if (thread.isEmpty()) {
					return;
				}
				// bbsObject
				ThreadDto threadDto = thread.values().stream().findAny().get();
				String boardUrl = threadDto.getBoardUrl();
				BBS bbsObject = BBSManager.getBBSFromUrl(boardUrl);
				// SETTING.TXT
				ConcurrentHashMap<String, String> setting = bbsObject.readSettingTxt(boardUrl);

				thread.values().stream()
						.filter(titlePredicate)
						.forEach(x -> executor.execute(new FileSearchProcess(tab, x, textPredicate, bbsObject, setting)));
			} catch (IOException e) {
				logger.error("ThreadSearchProcess失敗: " + dir.toString(), e);
			}
			
		}
	}

	private static class FileSearchProcess implements Runnable {
		private Tab tab;
		private ThreadDto threadDto;
		private Predicate<Map.Entry<Integer, ResDto>> textPredicate;
		private BBS bbsObject;
		private ConcurrentHashMap<String, String> setting;

		private FileSearchProcess(Tab tab, ThreadDto threadDto, Predicate<Map.Entry<Integer, ResDto>> textPredicate, BBS bbsObject, ConcurrentHashMap<String, String> setting) {
			this.tab = tab;
			this.threadDto = threadDto;
			this.textPredicate = textPredicate;
			this.bbsObject = bbsObject;
			this.setting = setting;
		}

		@Override
		public void run() {
			App.logger.debug(threadDto.getDatName());

			String url = bbsObject.getThreadUrlFromBoardUrlAndDatFileName(threadDto.getBoardUrl(), threadDto.getDatName());

			ThreadLoader threadLoader = bbsObject.createThreadLoader(url);
			ThreadLoadProcessor threadLoadProcessor = new ThreadLoadProcessor(threadLoader, url, App.replaceEmoji, null);

			try {
				// now必要か？
				long now = DateTimeUtil.localDateTimeToHttpLong(LocalDateTime.now());

				TreeMap<Integer, ResDto> res = new TreeMap<>();
				threadLoadProcessor.readDat(setting, res);

				// 加工
				ArrayList<String> errorDetails = new ArrayList<>();
				threadLoadProcessor.applyResProcessor(res, now, errorDetails);

				// 検索条件
				TreeMap<Integer, ResDto> filtered = res.entrySet().stream()
						.filter(textPredicate)
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, TreeMap::new));
				res.clear();

				if (filtered.isEmpty()) {
					return;
				}
				res = filtered;

				@SuppressWarnings("unchecked")
				TableView<ThreadDto> tableView = (TableView<ThreadDto>) tab.getContent();
				ObservableList<ThreadDto> thread = tableView.getItems();
				thread.add(threadDto);
			} catch (Exception e) {
				logger.error("ThreadSearchProcess失敗: " + url, e);
			}
		}
	}
}
