package pulad.chb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.thymeleaf.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;
import org.w3c.dom.html.HTMLImageElement;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.dto.AboneImageDto;
import pulad.chb.dto.NGFileDto;
import pulad.chb.dto.ThreadLoadTaskResponseDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.thread.LinkHistManager;
import pulad.chb.read.thread.LocalURLStreamHandler;
import pulad.chb.read.thread.ThreadLoadTask;
import pulad.chb.util.DomUtil;

/**
 * スレッドを開く処理。
 * @author pulad
 *
 */
public class ThreadViewProcessor {

	/**
	 * スレッドを開く。
	 * @param tab
	 * @param url
	 */
	public static void open(Tab tab, String url) {
		open(tab, url, true, -1);
	}

	/**
	 * スレッドを開く。
	 * @param tab
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 */
	public static void open(Tab tab, String url, boolean remote) {
		open(tab, url, remote, -1);
	}

	/**
	 * スレッドを開く。
	 * @param tab
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 * @param scrollY 開いた後でY軸をスクロールする。
	 */
	public static void open(Tab tab, String url, boolean remote, int scrollY) {
		WebView threadView = getWebView(tab);
		threadView.setCursor(Cursor.WAIT);

		// ここでremoteにオフラインボタンの状態を加味する。
		ThreadLoadService service = new ThreadLoadService(url, !App.offline && remote, App.replaceEmoji);
		service.setOnSucceeded(new ThreadLoadSucceededEventHandler(tab, url, !App.offline && remote, scrollY));
		service.start();
	}

	/**
	 * スレッドを再読み込みする。
	 * @param tab
	 * @param url
	 * @param remote ネットワークに接続するか。ここではオフラインボタンの状態を含まない。
	 */
	public static void reload(Tab tab, String url, boolean remote) {
		WebView threadView = getWebView(tab);
		threadView.setCursor(Cursor.WAIT);

		// ここでremoteにオフラインボタンの状態を加味する。
		int scrollY = (int) threadView.getEngine().executeScript("document.body.scrollTop");
		ThreadLoadService service = new ThreadLoadService(url, !App.offline && remote, App.replaceEmoji);
		service.setOnSucceeded(new ThreadLoadSucceededEventHandler(tab, url, !App.offline && remote, scrollY));
		service.start();
	}

	/**
	 * TabのWebViewを取得する。
	 * 未設定ならば初期化する。
	 * @param tab
	 * @return
	 */
	private static WebView getWebView(Tab tab) {
		BorderPane rootPane;
		WebView webView;
		javafx.scene.Node n = tab.getContent();
		if (n instanceof BorderPane) {
			rootPane = (BorderPane) n;
			webView = (WebView) rootPane.getCenter();
		} else {
			rootPane = new BorderPane();
			webView = new WebView();
			rootPane.setCenter(webView);

			Button writeButton = new Button("Write");
			Label nameLabel = new Label("名前");
			TextField nameInput = new TextField();
			Label mailLabel = new Label("メール");
			TextField mailInput = new TextField();
			mailInput.setPromptText("sage");
			HBox writeToolbox = new HBox(writeButton, nameLabel, nameInput, mailLabel, mailInput);
			writeToolbox.setSpacing(10d);
			writeToolbox.setAlignment(Pos.CENTER_LEFT);
			TextArea bodyInput = new TextArea();
			bodyInput.setPrefRowCount(3);
			VBox writePane = new VBox(writeToolbox, bodyInput);
			rootPane.setBottom(writePane);

			// 検索テキスト入力時
			WebEngine engine = webView.getEngine();
			Consumer<String> f = text -> {
				engine.executeScript("window.find(\"" + StringUtils.escapeJavaScript(text) + "\", false, false, true, false, true, false)");
			};
			rootPane.getProperties().put(App.PROPERTY_SEARCH_FUNCTION, f);
			tab.setContent(rootPane);
		}
		return webView;
	}

	private static class ThreadLoadService extends Service<ThreadLoadTaskResponseDto> {
		private String url;
		private boolean remote;
		private boolean replaceEmoji;

		public ThreadLoadService(String url, boolean remote, boolean replaceEmoji) {
			this.url = url;
			this.remote = remote;
			this.replaceEmoji = replaceEmoji;
		}

		@Override
		protected Task<ThreadLoadTaskResponseDto> createTask() {
			BBS bbsObject = BBSManager.getBBSFromUrl(url);
			Task<ThreadLoadTaskResponseDto> task = new ThreadLoadTask(bbsObject.createThreadLoader(url), url, remote, replaceEmoji);
			// なぜかこれを呼ぶとServiceのOnSucceededも呼ばれるようになる
//			task.setOnSucceeded(event -> {});
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
				}
			});
			return task;
		}
	}

	private static class ThreadLoadSucceededEventHandler implements EventHandler<WorkerStateEvent> {
		private Tab tab;
		private String url;
		private boolean remote;
		private int scrollY;

		public ThreadLoadSucceededEventHandler(Tab tab, String url, boolean remote, int scrollY) {
			this.tab = tab;
			this.url = url;
			this.remote = remote;
			this.scrollY = scrollY;
		}

		@Override
		public void handle(WorkerStateEvent event) {
			WebView threadView = (WebView) getWebView(tab);
			threadView.setCursor(null);

			ThreadLoadTaskResponseDto threadLoadTaskResponseDto = (ThreadLoadTaskResponseDto) event.getSource().getValue();
			tab.getProperties().put(App.TAB_PROPERTY_STATUS_ERROR, threadLoadTaskResponseDto.getErrorMessage());
			App.getInstance().notifyChangeStatus();
			String html = threadLoadTaskResponseDto.getHtml();
			if (html == null) {
				return;
			}

			//threadView.setContextMenuEnabled(false);
			WebEngine engine = threadView.getEngine();
			tab.setOnClosed(new CloseEventListener(engine));
			// html読み込み後にイベントを設定する
			engine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
				@Override
				public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
					if (newValue == State.SUCCEEDED) {
						engine.getLoadWorker().stateProperty().removeListener(this);

						if ("about:blank".equals(engine.getLocation())) {
							return;
						}
						Document document = engine.getDocument();
						Element title = document.getElementById("title");
						if (title != null) {
							tab.setText(title.getTextContent());
						}
						ThreadViewRightClickEventListener rightClickEventListener = new ThreadViewRightClickEventListener(tab, threadView, url);
						EventListener clickEventListener = new ClickEventListener(url, rightClickEventListener);
						Element root = document.getElementById("root");
//						((EventTarget) root).addEventListener(
//								"mouseover",
//								new MouseOverEventListener(engine, url, clickEventListener, rightClickEventListener),
//								false);
						((EventTarget) root).addEventListener("click", clickEventListener, false);
						((EventTarget) root).addEventListener("contextmenu", rightClickEventListener, false);

						boolean scrollNew = false;
						if (scrollY >= 0) {
							try {
								engine.executeScript("window.scroll(0, " + scrollY + ")");
							} catch (Exception e) {}
						} else if (remote) {
							scrollNew = true;
							try {
								engine.executeScript("document.getElementsByName(\"current\")[0].scrollIntoView(true)");
							} catch (Exception e) {}
						}

						NodeList imgList = document.getElementsByTagName("img");
						int length = imgList.getLength();
						String status = "読み込み完了 img 0/" + length;
						tab.getProperties().put(App.TAB_PROPERTY_STATUS, status);
						App.getInstance().notifyChangeStatus();
						EventListener imgLoadEventListener = new ImgLoadEventListener(tab, url, engine, length, scrollNew);
						for (int i = 0; i < length; i++) {
							Element img = (Element) imgList.item(i);
							((EventTarget) img).addEventListener("load", imgLoadEventListener, false);
							((EventTarget) img).addEventListener("error", imgLoadEventListener, false);
							// gifをサムネイルに表示しない
							String replaceSrc = img.getAttribute("data-view");
							img.setAttribute("src", StringUtils.isEmpty(replaceSrc) ? img.getAttribute("data-src") : replaceSrc);
						}
					}
				}
			});
			threadView.requestFocus();
			engine.loadContent(html, "text/html");
		}
	}

	/**
	 * メモリを解放する。
	 * @author pulad
	 *
	 */
	private static class CloseEventListener implements EventHandler<javafx.event.Event> {
		private WebEngine engine;

		public CloseEventListener(WebEngine engine) {
			this.engine = engine;
		}

		@Override
		public void handle(javafx.event.Event event) {
			engine.load("about:blank");
		}
		
	}

	private static class ClickEventListener implements EventListener {
		private String url;
		private ThreadViewRightClickEventListener rightClickEventListener;

		public ClickEventListener(String url,
				ThreadViewRightClickEventListener rightClickEventListener) {
			this.url = url;
			this.rightClickEventListener = rightClickEventListener;
		}

		@Override
		public void handleEvent(Event evt) {
			try {
				if (!(evt instanceof MouseEvent)) {
					return;
				}
				MouseEvent event = (MouseEvent) evt;
				if (!(event.getTarget() instanceof Node)) {
					return;
				}
				Node target = (Node) event.getTarget();
				if (handleEventBubbling(target)) {
					return;
				}

			// ポップアップで何もない部分をクリックしたら閉じる
			// コピペできないので停止
//			if ("1".equals(target.getOwnerDocument().getElementById("filtered").getAttribute("value"))) {
//				App.getInstance().closePopupThreads();
//			}
			} catch (Exception e) {
				App.logger.error("ThreadViewProcessor.ClickEventListener失敗", e);
			}
		}

		/**
		 * 名前欄のFONTタグ等を回避するために親タグを再帰的に検索する。
		 * @param target
		 * @return イベント処理した場合はtrue。
		 */
		private boolean handleEventBubbling(Node target) {
			String nodeName = target.getNodeName();
			// <a href="#xxx">
			if ("a".equalsIgnoreCase(nodeName)) {
				String chain = DomUtil.getAttribute(target, "chain");
				if (chain == null) {
					return true;
				}
				App.getInstance().openPopupThread(url, chain, this, rightClickEventListener);
				return true;
			} else if ("img".equalsIgnoreCase(nodeName)) {
				String src = DomUtil.getAttribute(target, "data-src");
				if (src == null) {
					return true;
				}
				App.getInstance().openImage(src);
				return true;
			} else {
				Node parent = target.getParentNode();
				if (parent != null) {
					return handleEventBubbling(parent);
				}
			}

			return false;
		}
	}

	/**
	 * imgの画像の読み込みが完了した時の処理。
	 * @author pulad
	 *
	 */
	private static class ImgLoadEventListener implements EventListener {
		private final Object o = new Object();
		private final ObjectMapper mapper;
		private Tab tab;
		private WebEngine engine;
		private int imgCount;
		private int imgLoadedCount = 0;
		private boolean scrollNew;
		private List<AboneImageDto> whiteList = new ArrayList<>();
		private List<AboneImageDto> aboneList = new ArrayList<>();

		public ImgLoadEventListener(Tab tab, String url, WebEngine engine, int imgCount, boolean scrollNew) {
			this.tab = tab;
			this.engine = engine;
			this.imgCount = imgCount;
			this.scrollNew = scrollNew;

			mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			BBS bbsObject = BBSManager.getBBSFromUrl(url);
			String bbsDir = bbsObject.getBBSDirectoryName();
			String bbs = bbsObject.getLogDirectoryName();
			String board = bbsObject.getBoardFromThreadUrl(url);
			String threadNGFileName = bbsObject.getThreadFromThreadUrl(url) + ".chb.txt";

			// 全体
			readAboneFile(whiteList, aboneList, Config.getRootFolder().resolve("chb.txt").toFile());
			// BBS
			readAboneFile(whiteList, aboneList, Config.getBBSFolder().resolve(bbsDir).resolve("chb.txt").toFile());
			// Board
			readAboneFile(whiteList, aboneList, Config.getLogFolder().resolve(bbs).resolve(board).resolve("chb.txt").toFile());
			// Thread
			readAboneFile(whiteList, aboneList, Config.getLogFolder().resolve(bbs).resolve(board).resolve(threadNGFileName).toFile());
			whiteList = List.copyOf(whiteList);
			aboneList = List.copyOf(aboneList);
		}

		@Override
		public void handleEvent(Event evt) {
			// 画像あぼ～ん
			try {
				HTMLImageElement img = (HTMLImageElement) evt.getTarget();
				String src = img.getSrc();
				if (src != null) {
					String httpsrc = LocalURLStreamHandler.getSourceUrl(src);
					String imageFileName = LinkHistManager.getCacheFileName(httpsrc);
					if (imageFileName != null && !imageFileName.startsWith("classpath:")) {
						String hash = Paths.get(imageFileName).getFileName().toString();
						hash = hash.substring(0, hash.indexOf(".")); // -1の場合は例外

						boolean white = false;
						for (int i = 0; i < whiteList.size(); i++) {
							AboneImageDto aboneDto = whiteList.get(i);

							// TODO:期限切れチェック

							if (hash.equals(aboneDto.getHash())) {
								white = true;
								break;
							}
						}

						if (!white) {
							for (int i = 0; i < aboneList.size(); i++) {
								AboneImageDto aboneDto = aboneList.get(i);

								// TODO:期限切れチェック

								if (hash.equals(aboneDto.getHash())) {
									img.setSrc("image:abone.png");
									img.setTitle(aboneDto.getLabel());
									// あぼ～んの場合は読み込み完了カウントしない
									return;
								}
							}
						}
					}
				}
			} catch (IndexOutOfBoundsException e) {}

			synchronized (o) {
				try {
					String status = "読み込み完了 img " + ++imgLoadedCount + "/" + imgCount;
					tab.getProperties().put(App.TAB_PROPERTY_STATUS, status);
					App.getInstance().notifyChangeStatus();
				} catch (Exception e) {}
			}
			if (scrollNew && (imgLoadedCount == imgCount)) {
				try {
					engine.executeScript("document.getElementsByName(\"current\")[0].scrollIntoView(true)");
				} catch (Exception e) {}
			}
		}

		private void readAboneFile(List<AboneImageDto> whiteList, List<AboneImageDto> aboneList, File file) {
			try {
				if (!file.exists()) {
					return;
				}
				if (file.length() == 0) {
					file.delete();
					return;
				}
				NGFileDto ngFileDto = mapper.readValue(file, NGFileDto.class);
				if (ngFileDto == null) {
					return;
				}
				if (ngFileDto.getImage() != null) {
					for (AboneImageDto image : ngFileDto.getImage()) {
						if (image.isWhite()) {
							whiteList.add(image);
						} else {
							aboneList.add(image);
						}
					}
				}
			} catch (IOException e) {
				App.logger.error("readAboneFile失敗", e);
			}
		}
	}
}
