package pulad.chb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.ObjectUtils;
import org.thymeleaf.util.StringUtils;
import org.w3c.dom.events.EventListener;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.dto.ConfigFileDto;
import pulad.chb.dto.SearchConditionDto;
import pulad.chb.dto.TreeItemDto;
import pulad.chb.favorite.FavoriteManager;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.thread.LocalURLStreamHandler;
import pulad.chb.util.NumberUtil;
import pulad.chb.util.UrlUtil;

/**
 * JavaFX App
 */
public class App extends Application {
	public static Logger logger = LoggerFactory.getLogger(App.class);
	private static FileOutputStream lockFile;

	/**
	 * Tabのcontent(Node型）のgetPropertyで設定する値。
	 * Consumer&lt;String&gt;型。
	 */
	public static final String PROPERTY_SEARCH_FUNCTION = "searchFunction";
	/**
	 * TabのProperty値のURL。
	 */
	public static final String TAB_PROPERTY_URL = "url";
	/**
	 * TabのProperty値の過去ログ。
	 */
	public static final String TAB_PROPERTY_PAST = "past";
	/**
	 * TabのProperty値のステータスバーに表示する文字。
	 */
	public static final String TAB_PROPERTY_STATUS = "status";
	/**
	 * TabのProperty値のステータスバーに表示するエラーメッセージ。
	 */
	public static final String TAB_PROPERTY_STATUS_ERROR = "statusError";
	/**
	 * 検索条件タブのURL。
	 */
	public static final String URL_SEARCH = "chb://search/";
	/**
	 * 検索結果タブのURL。
	 */
	public static final String URL_SEARCH_RESULT = "chb://searchresult/";

	private static App app = null;
	private Stage stage = null;
	public static volatile boolean offline = false;
	public static volatile boolean replaceEmoji = false;
	public static volatile boolean pastLog = false;

	private TextField urlField = null;
	private TextField searchField = null;
	private Label statusBar = null;
	private TabPane favoriteTreeTabPane = null;
	private Tab favoriteTab = null;
	private TabPane threadTabPane = null;
	//private ChangeListener<String> statusListener
	private ConcurrentLinkedQueue<Popup> popupThreadViewList = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Popup> imageViewList = new ConcurrentLinkedQueue<>();
	private ExecutorService executor = null;

	public App() {
		super();
	}

	public static int main(String[] args) {
		if (args.length < 1) {
			Platform.exit();
			return -1;
		}
		Config.init(args[0], App.class);
		BBSManager.init();

		try {
			lockFile = new FileOutputStream(Config.getRootFolder().resolve("chb_lock").toFile());
			FileLock lock = lockFile.getChannel().tryLock();
			if (lock == null) {
				// 多重起動
				Platform.exit();
				return 0;
			}
		} catch (IOException e) {
			App.logger.error("chb_lock失敗", e);
			Platform.exit();
			return 0;
		}

		launch();
		System.exit(0);
		return 0;
	}

	@Override
	public void start(Stage stage) {
		// フォント一覧
//		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//		for (java.awt.Font font : ge.getAllFonts()) {
//			System.out.println(font.getFamily() + "," + font.getName());
//		}

		App.app = this;
		this.stage = stage;
		URL.setURLStreamHandlerFactory(new LocalURLStreamHandler.Factory());
		CookieStore.initialize(Config.getRootFolder().resolve("chb_cookie.txt").toFile());
		CookieHandler.setDefault(new CookieManager(CookieStore.get(), null));

//		final Menu menu1 = new Menu("ファイル(F)");
//		final Menu menu2 = new Menu("ヘルプ(H)");
//		MenuBar menuBar = new MenuBar();
//		menuBar.getMenus().addAll(menu1, menu2);

		ToggleButton offlineButton = new ToggleButton();
		offlineButton.setText("Offline");
		offlineButton.getStyleClass().add("toggle");
		offlineButton.setOnAction(event -> {
			offline = !offline;
			offlineButton.setSelected(offline);
		});
		ToggleButton emojiButton = new ToggleButton();
		emojiButton.setText("&#12;");
		emojiButton.getStyleClass().add("toggle");
		emojiButton.setOnAction(event -> {
			replaceEmoji = !replaceEmoji;
			emojiButton.setSelected(replaceEmoji);
		});
		ToggleButton pastButton = new ToggleButton();
		pastButton.setText("過去ログ");
		pastButton.getStyleClass().add("toggle");
		pastButton.setOnAction(event -> {
			pastLog = !pastLog;
			pastButton.setSelected(pastLog);
		});
		Button searchButton = new Button();
		searchButton.setText("ログ検索");
		searchButton.setOnAction(event -> {
			openSearch();
		});
		HBox buttonBox = new HBox(offlineButton, emojiButton, pastButton, searchButton);

		urlField = new TextField();
		urlField.getStyleClass().add("urlField");
		searchField = new TextField();
		searchField.getStyleClass().add("searchField");
		BorderPane toolbar = new BorderPane();
		toolbar.setLeft(buttonBox);
		toolbar.setCenter(urlField);
		toolbar.setRight(searchField);
		urlField.setOnAction(event -> {
			openUrl(((TextField) event.getSource()).getText());
		});
		searchField.setOnAction(event -> {
			String word = searchField.getText();
			if (StringUtils.isEmpty(word)) {
				return;
			}
			Tab tab = threadTabPane.getSelectionModel().getSelectedItem();
			if (tab == null) {
				return;
			}
			Node content = tab.getContent();
			if (content == null) {
				return;
			}
			// タブ毎に設定された独自Consumerを実行する
			@SuppressWarnings("unchecked")
			Consumer<String> f = (Consumer<String>) content.getProperties().get(PROPERTY_SEARCH_FUNCTION);
			if (f != null) {
				f.accept(word);
			}
		});

		statusBar = new Label();
		VBox bottomPane = new VBox(statusBar);

		favoriteTreeTabPane = new TabPane();
		favoriteTreeTabPane.getStyleClass().add("favoriteTreeTabPane");

		favoriteTab = new Tab("お気に入り");
		reloadFavorite();
		favoriteTreeTabPane.getTabs().add(favoriteTab);

		Tab chBoardTab = new Tab("2ch");
		BBSMenuTreeProcessor.open(chBoardTab, this);
		favoriteTreeTabPane.getTabs().add(chBoardTab);

		VBox topPane = new VBox(/*menuBar, */toolbar);
		//rootPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		//rootPane.prefWidthProperty().bind(stage.widthProperty());
		//rootPane.prefHeightProperty().bind(stage.heightProperty());

		threadTabPane = new TabPane();
		threadTabPane.setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		threadTabPane.setTabDragPolicy(TabDragPolicy.REORDER);
		// TODO: Ctrl + 右クリックで閉じたいがTabが取得できない
//		threadTabPane.setOnMouseClicked(e -> {
//			if (e.getButton() != MouseButton.SECONDARY || !e.isControlDown()) {
//				return;
//			}
//			EventTarget target = e.getTarget();
//			Node stackPane = (Node) target;
//			while (stackPane != null && (stackPane instanceof Parent)) {
//				stackPane = ((Parent) stackPane).getParent();
//			}
//		});

		BorderPane rootPane = new BorderPane();
		rootPane.setTop(topPane);
		rootPane.setLeft(favoriteTreeTabPane);
		rootPane.setBottom(bottomPane);
		rootPane.setCenter(threadTabPane);

		Scene scene = new Scene(rootPane, 1280, 480);
		scene.getStylesheets().add(Config.styleCss);
		// Ctrl+fで検索をフォーカスする
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> {
			searchField.requestFocus();
		});
		// Ctrl+wでタブを閉じる
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN), () -> {
			try {
				Tab tab = threadTabPane.getTabs().remove(threadTabPane.getSelectionModel().getSelectedIndex());
				if (tab != null) {
					Event.fireEvent(tab, new Event(Tab.CLOSED_EVENT));
				}
			} catch (IndexOutOfBoundsException e) {}
		});
		stage.setScene(scene);
		stage.setMaximized(true);
		stage.setOnHidden(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				closePopupThreads();
				closeImages();
			}
		});
		stage.setTitle("chb");

		threadTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
				App.getInstance().notifyChangeStatus();
			}
		});

		// ウィンドウの位置とcookieを書き込み
		stage.showingProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (oldValue == true && newValue == false) {
					ConfigFileDto configFileDto = Config.read();
					configFileDto.setMaximized(stage.isMaximized());
					configFileDto.setX((int)stage.getX());
					configFileDto.setY((int)stage.getY());
					configFileDto.setWidth((int)stage.getWidth());
					configFileDto.setHeight((int)stage.getHeight());
					Config.write(configFileDto);

					CookieStore.get().save();
				}
			}
		});
		// configを読み込み
		try {
			ConfigFileDto configFileDto = Config.read();
			if (configFileDto.getX() != null) {
				stage.setX(configFileDto.getX());
			}
			if (configFileDto.getY() != null) {
				stage.setY(configFileDto.getY());
			}
			if (configFileDto.getWidth() != null) {
				stage.setWidth(configFileDto.getWidth());
			}
			if (configFileDto.getHeight() != null) {
				stage.setHeight(configFileDto.getHeight());
			}
			if (configFileDto.getMaximized() != null) {
				stage.setMaximized(configFileDto.getMaximized());
			}
		} catch (Exception e) {
		}

//		stage.setOnCloseRequest(event -> {
//			if (executor != null) {
//				executor.shutdownNow();
//				executor = null;
//			}
//		});
		executor = Executors.newFixedThreadPool(16);

		stage.show();

		//openThread("http://egg.5ch.net/test/read.cgi/applism/1550654600/1472290387.dat", false);
		//openThread("http://jbbs.shitaraba.net/bbs/read.cgi/netgame/16124/1563530027/", false);
		//openImage("https://i.imgur.com/xCBGM3q.jpg");
	}

	@Override
	public void stop() throws Exception {
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
	}

	public static App getInstance() {
		return app;
	}

	public void reloadFavorite() {
		FavoriteTreeProcessor.open(favoriteTab, this);
	}

	@SuppressWarnings("unchecked")
	public TreeView<TreeItemDto> getFavoriteTree() {
		return (TreeView<TreeItemDto>) this.favoriteTab.getContent();
	}

	public Stage getStage() {
		return stage;
	}

	public void notifyChangeStatus() {
		synchronized (app) {
			Tab tab = threadTabPane.getSelectionModel().getSelectedItem();
			if (tab == null) {
				statusBar.setText("");
				return;
			}
			Map<Object, Object> properties = tab.getProperties();
			statusBar.setText(ObjectUtils.nullSafe((String) properties.getOrDefault(App.TAB_PROPERTY_STATUS_ERROR, null), "") +
					" " + (String) properties.getOrDefault(App.TAB_PROPERTY_STATUS, ""));
		}
	}

	public ExecutorService getExecutor() {
		return executor;
	}

	/**
	 * タブを表示する。開始時はスレッドタブペイン作成後に呼び出し可能。
	 * @param urlStr
	 */
	public void openUrl(String urlStr) {
		openUrl(urlStr, true);
	}

	/**
	 * タブを表示する。開始時はスレッドタブペイン作成後に呼び出し可能。
	 * @param urlStr
	 * @param remote
	 */
	public void openUrl(String urlStr, boolean remote) {
		// URL欄に表示するため雑にhttpをhttpsにする
		final String url = UrlUtil.toHttps(urlStr);
		BBS bbsObject = BBSManager.getBBSFromUrl(url);
		if (bbsObject == null) {
			App.logger.error("対応していないURL: {}", url);
			return;
		}
		if (bbsObject.isBoardUrl(url)) {
			boolean pastLog = App.pastLog;
			Tab tab = getTab(url, pastLog);
			if (tab == null) {
				tab = new Tab("読み込み中");
				tab.getProperties().put(TAB_PROPERTY_URL, url);
				tab.getProperties().put(TAB_PROPERTY_PAST, pastLog);
				tab.getProperties().put(TAB_PROPERTY_STATUS, url);
				tab.setOnSelectionChanged(new TabEvent(urlField));

				MenuItem item = new MenuItem("お気に入りに追加");
				item.setOnAction(event2 -> {
					FavoriteManager.addFavorite(getFavoriteTree(), url);
				});
				ContextMenu menu = new ContextMenu(item);
				menu.setAutoHide(true);
				tab.setContextMenu(menu);

				BoardViewProcessor.open(tab, this, url, remote, pastLog);
				threadTabPane.getTabs().add(tab);
			} else {
				tab.getProperties().remove(TAB_PROPERTY_STATUS_ERROR);
				BoardViewProcessor.reload(tab, this, url, remote, pastLog);
			}
			threadTabPane.getSelectionModel().select(tab);
		} else if (bbsObject.isThreadUrl(url)) {
			Tab tab = getTab(url, false); // スレッドは過去ログは暫定的にfalseとする
			if (tab == null) {
				tab = new Tab("読み込み中");
				tab.getProperties().put(TAB_PROPERTY_URL, url);
				tab.getProperties().put(TAB_PROPERTY_STATUS, url);
				tab.setOnSelectionChanged(new TabEvent(urlField));

				MenuItem item = new MenuItem("お気に入りに追加");
				item.setOnAction(event2 -> {
					FavoriteManager.addFavorite(getFavoriteTree(), url);
				});
				ContextMenu menu = new ContextMenu(item);
				menu.setAutoHide(true);
				tab.setContextMenu(menu);

				ThreadViewProcessor.open(tab, url, remote);
				threadTabPane.getTabs().add(tab);
			} else {
				tab.getProperties().remove(TAB_PROPERTY_STATUS_ERROR);
				ThreadViewProcessor.reload(tab, url, remote);
			}
			threadTabPane.getSelectionModel().select(tab);
		} else {
			return;
		}
	}

	/**
	 * 検索タブを開く。
	 */
	public void openSearch() {
		Tab tab = getTab(URL_SEARCH);
		if (tab == null) {
			tab = new Tab("ログ検索");
			tab.getProperties().put(TAB_PROPERTY_URL, URL_SEARCH);
			tab.getProperties().put(TAB_PROPERTY_STATUS, "ログ検索");
			tab.setOnSelectionChanged(new TabEvent(urlField));

			SearchViewProcessor.open(tab);
			threadTabPane.getTabs().add(tab);
		}
		threadTabPane.getSelectionModel().select(tab);
	}

	/**
	 * 検索結果タブを開く。
	 * @param dto 検索条件
	 */
	public void openSearchResult(SearchConditionDto dto) {
		Tab tab = getTab(URL_SEARCH_RESULT);
		if (tab == null) {
			tab = new Tab("ログ検索結果[" + dto.getText() + "]");
			tab.getProperties().put(TAB_PROPERTY_URL, URL_SEARCH_RESULT);
			tab.getProperties().put(TAB_PROPERTY_STATUS, "ログ検索結果[" + dto.getText() + "]");
			tab.setOnSelectionChanged(new TabEvent(urlField));

			SearchResultViewProcessor.open(tab, dto);
			threadTabPane.getTabs().add(tab);
		} else {
			SearchResultViewProcessor.reload(tab, dto);
		}
		threadTabPane.getSelectionModel().select(tab);
	}

	/**
	 * レス一覧リンクを表示する。
	 * @param url
	 * @param chain カンマ区切りのレス番の一覧。
	 */
	public void openPopupThread(
			String url,
			String chain,
			EventListener clickEventListener,
			ThreadViewRightClickEventListener rightClickEventListener) {
		List<Integer> resFilter = new ArrayList<>();
		for (String s : chain.split(",")) {
			resFilter.add(NumberUtil.integerCache(Integer.parseInt(s)));
		}

		Popup p = PopupThreadViewProcessor.open(
				getStage(),
				threadTabPane.getWidth(),
				clickEventListener,
				rightClickEventListener,
				url,
				resFilter);
		synchronized (popupThreadViewList) {
			popupThreadViewList.add(p);
		}
	}

	public void write(String url, String name, String mail, String body) {
		WriteProcessor.write(url, name, mail, body);
	}

	/**
	 * レス一覧リンクを閉じる。
	 */
	public void closePopupThreads() {
		ArrayList<Popup> popupList;
		synchronized (popupThreadViewList) {
			popupList = new ArrayList<>(popupThreadViewList);
			popupThreadViewList.clear();
		}

		for (Popup p : popupList) {
			try {
				p.hide();
				Event.fireEvent(p, new WindowEvent(p, WindowEvent.WINDOW_HIDDEN));
				//Event.fireEvent(p, new Event(p, p, Event.ANY));
			} catch (Exception e) {
				logger.error("closePopupThreads失敗", e);
			}
		}
	}

	/**
	 * 画像ポップアップを表示する。
	 * 後で閉じる用にリスト管理する。
	 * 必要か？
	 * @param url
	 */
	public void openImage(String url) {
		Popup p = ImageViewProcessor.view(getStage(), url);
		synchronized (imageViewList) {
			imageViewList.add(p);
		}
	}

	/**
	 * 画像ポップアップをすべて閉じる。
	 */
	private void closeImages() {
		ArrayList<Popup> popupList;
		synchronized (imageViewList) {
			popupList = new ArrayList<>(imageViewList);
			imageViewList.clear();
		}

		for (Popup p : popupList) {
			try {
				p.hide();
			} catch (Exception e) {
				logger.error("closeImages失敗", e);
			}
		}
	}

	private Tab getTab(String url) {
		return getTab(url, false);
	}

	private Tab getTab(String url, boolean pastLog) {
		FilteredList<Tab> list = threadTabPane.getTabs()
				.filtered(x -> url.equals(x.getProperties().getOrDefault(TAB_PROPERTY_URL, null)) &&
						pastLog == ((Boolean) x.getProperties().getOrDefault(TAB_PROPERTY_PAST, Boolean.FALSE)).booleanValue());
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * タブがアクティブになった時にURLを表示する。
	 * @author pulad
	 *
	 */
	private static class TabEvent implements EventHandler<javafx.event.Event> {
		private TextField urlField;

		private TabEvent(TextField urlField) {
			this.urlField = urlField;
		}

		@Override
		public void handle(javafx.event.Event event) {
			Tab tab = (Tab) event.getTarget();
			String url = (String) tab.getProperties().get(TAB_PROPERTY_URL);
			urlField.setText(url);
		}
	}
}