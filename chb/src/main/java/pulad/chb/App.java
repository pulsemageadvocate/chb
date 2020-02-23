package pulad.chb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TabPane.TabDragPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import pulad.chb.config.Config;
import pulad.chb.dto.ConfigFileDto;
import pulad.chb.favorite.FavoriteManager;
import pulad.chb.read.thread.LocalURLStreamHandler;
import pulad.chb.util.NumberUtil;

/**
 * JavaFX App
 */
public class App extends Application {
	public static Logger logger = LoggerFactory.getLogger(App.class);
	public static Path rootFolder;
	public static Path bbsFolder;
	public static Path logFolder;
	public static Path scriptFolder;
	public static Path imageFolder;
	public static Path styleFolder;
	public static Path linkhistFile;
	public static Path configFile;
	public static String ua = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36";
	public static String editorCommand = "C:\\Programs\\sakura\\sakura.exe $LINK";
	public static String styleCss;
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
	 * TabのProperty値のステータスバーに表示する文字。
	 */
	public static final String TAB_PROPERTY_STATUS = "status";
	/**
	 * TabのProperty値のステータスバーに表示するエラーメッセージ。
	 */
	public static final String TAB_PROPERTY_STATUS_ERROR = "statusError";

	private static App app = null;
	private Stage stage = null;
	public static volatile boolean offline = false;

	private TextField urlField = null;
	private TextField searchField = null;
	private Label statusBar = null;
	private TabPane favoriteTreeTabPane = null;
	private Tab favoriteTab = null;
	private TabPane threadTabPane = null;
	//private ChangeListener<String> statusListener
	private ConcurrentLinkedQueue<Popup> popupThreadViewList = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<Popup> imageViewList = new ConcurrentLinkedQueue<>();

	public App() {
		super();
	}

	public static int main(String[] args) {
		if (args.length < 1) {
			Platform.exit();
			return -1;
		}

		rootFolder = Paths.get(args[0]);
		bbsFolder = Paths.get(args[0], "BBS");
		logFolder = Paths.get(args[0], "log");
		scriptFolder = Paths.get(args[0], "script");
		imageFolder = Paths.get(args[0], "image");
		styleFolder = Paths.get(args[0], "style");
		linkhistFile = Paths.get(args[0], "linkhist.txt");
		configFile = Paths.get(args[0], "chb_config.txt");
		try {
			styleCss = App.styleFolder.resolve("style.css").toUri().toURL().toExternalForm();
		} catch (MalformedURLException e) {
			App.logger.error("style.css失敗", e);
			Platform.exit();
			return -1;
		}

		try {
			lockFile = new FileOutputStream(rootFolder.resolve("chb_lock").toFile());
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

		final Menu menu1 = new Menu("ファイル(F)");
		final Menu menu2 = new Menu("ヘルプ(H)");
		MenuBar menuBar = new MenuBar();
		menuBar.getMenus().addAll(menu1, menu2);

		ToggleButton offlineButton = new ToggleButton();
		offlineButton.setText("Offline");
		//offlineButton.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -jfx-toggle-color: yellow;");
		offlineButton.getStyleClass().add("toggle");
		offlineButton.setOnAction(event -> {
			offline = !offline;
			offlineButton.setSelected(offline);
		});
		urlField = new TextField();
		urlField.setMaxWidth(Double.MAX_VALUE);
		searchField = new TextField();
		searchField.setPrefWidth(200d);
		searchField.setMinWidth(200d);
		searchField.setMaxWidth(200d);
		BorderPane toolbar = new BorderPane();
		toolbar.setLeft(offlineButton);
		toolbar.setCenter(urlField);
		toolbar.setRight(searchField);
		urlField.setOnAction(event -> {
			openThread(((TextField) event.getSource()).getText());
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
		favoriteTreeTabPane.setPrefWidth(400d);
		favoriteTab = new Tab("お気に入り");
		reloadFavorite();
		favoriteTreeTabPane.getTabs().add(favoriteTab);

		Tab chBoardTab = new Tab("2ch");
		BBSMenuTreeProcessor.open(chBoardTab, this);
		favoriteTreeTabPane.getTabs().add(chBoardTab);

		VBox topPane = new VBox(menuBar, toolbar);
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
		scene.getStylesheets().add(styleCss);
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

		// ウィンドウの位置を書き込み
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
					configFileDto.setEditor(editorCommand);
					Config.write(configFileDto);
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
			editorCommand = configFileDto.getEditor();
			if (editorCommand == null) {
				editorCommand = "C:\\Programs\\sakura\\sakura.exe $LINK";
			}
		} catch (Exception e) {
		}
		stage.show();

		//openThread("http://egg.5ch.net/test/read.cgi/applism/1550654600/1472290387.dat", false);
		//openThread("http://jbbs.shitaraba.net/bbs/read.cgi/netgame/16124/1563530027/", false);
		//openImage("https://i.imgur.com/xCBGM3q.jpg");
	}

	public static App getInstance() {
		return app;
	}

	public void reloadFavorite() {
		FavoriteTreeProcessor.open(favoriteTab, this);
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
			statusBar.setText(ObjectUtils.nullSafe((String) tab.getProperties().getOrDefault(App.TAB_PROPERTY_STATUS_ERROR, null), "") +
					" " + (String) tab.getProperties().getOrDefault(App.TAB_PROPERTY_STATUS, ""));
		}
	}

	/**
	 * 板タブを表示する。開始時はスレッドタブペイン作成後に呼び出し可能。
	 * @param url
	 */
	public void openBoard(String url) {
		openBoard(url, true);
	}

	/**
	 * 板タブを表示する。開始時はスレッドタブペイン作成後に呼び出し可能。
	 * @param url
	 * @param remote
	 */
	public void openBoard(String url, boolean remote) {
		Tab tab = getTab(url);
		if (tab == null) {
			tab = new Tab("読み込み中");
			tab.getProperties().put(TAB_PROPERTY_URL, url);
			tab.getProperties().put(TAB_PROPERTY_STATUS, url);
			tab.setOnSelectionChanged(new TabEvent(urlField));

			MenuItem item = new MenuItem("お気に入りに追加");
			item.setOnAction(event2 -> {
				FavoriteManager.addFavorite(url);
			});
			ContextMenu menu = new ContextMenu(item);
			menu.setAutoHide(true);
			tab.setContextMenu(menu);

			BoardViewProcessor.open(tab, this, url, remote);
			threadTabPane.getTabs().add(tab);
		} else {
			tab.getProperties().remove(TAB_PROPERTY_STATUS_ERROR);
			BoardViewProcessor.reload(tab, this, url, remote);
		}
		threadTabPane.getSelectionModel().select(tab);
	}

	/**
	 * スレッドタブを表示する。開始時はスレッドタブペイン作成後に呼び出し可能。
	 * @param url
	 */
	public void openThread(String url) {
		openThread(url, true);
	}

	/**
	 * スレッドタブを表示する。開始時はスレッドタブペイン作成後に呼び出し可能。
	 * @param url
	 */
	public void openThread(String url, boolean remote) {
		Tab tab = getTab(url);
		if (tab == null) {
			tab = new Tab("読み込み中");
			tab.getProperties().put(TAB_PROPERTY_URL, url);
			tab.getProperties().put(TAB_PROPERTY_STATUS, url);
			tab.setOnSelectionChanged(new TabEvent(urlField));

			MenuItem item = new MenuItem("お気に入りに追加");
			item.setOnAction(event2 -> {
				FavoriteManager.addFavorite(url);
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
		popupThreadViewList.add(PopupThreadViewProcessor.open(
				getStage(),
				threadTabPane.getWidth(),
				clickEventListener,
				rightClickEventListener,
				url,
				resFilter));
	}

	/**
	 * レス一覧リンクを閉じる。
	 */
	public void closePopupThreads() {
		Popup popup = null;
		while ((popup = popupThreadViewList.poll()) != null) {
			popup.hide();
			Event.fireEvent(popup, new WindowEvent(popup, WindowEvent.WINDOW_HIDDEN));
			//Event.fireEvent(popup, new Event(popup, popup, Event.ANY));
		}
	}

	/**
	 * 画像ポップアップを表示する。
	 * 後で閉じる用にリスト管理する。
	 * 必要か？
	 * @param url
	 */
	public void openImage(String url) {
		imageViewList.add(ImageViewProcessor.view(getStage(), url));
	}

	/**
	 * 画像ポップアップをすべて閉じる。
	 */
	private void closeImages() {
		Popup popup = null;
		while ((popup = imageViewList.poll()) != null) {
			popup.hide();
		}
	}

	private Tab getTab(String url) {
		FilteredList<Tab> list = threadTabPane.getTabs()
				.filtered(x -> url.equals(x.getProperties().getOrDefault(TAB_PROPERTY_URL, null)));
		return list.isEmpty() ? null : list.get(0);
	}

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