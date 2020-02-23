package pulad.chb;

import java.util.function.Consumer;

import org.thymeleaf.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MouseEvent;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import pulad.chb.bbs.BBS;
import pulad.chb.bbs.BBSManager;
import pulad.chb.dto.ThreadLoadTaskResponseDto;

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
		getWebView(tab);

		// ここでremoteにオフラインボタンの状態を加味する。
		ThreadLoadService service = new ThreadLoadService(url, !App.offline && remote);
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

		// ここでremoteにオフラインボタンの状態を加味する。
		int scrollY = (int) threadView.getEngine().executeScript("document.body.scrollTop");
		ThreadLoadService service = new ThreadLoadService(url, !App.offline && remote);
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
		WebView webView;
		javafx.scene.Node n = tab.getContent();
		if (n instanceof WebView) {
			webView = (WebView) n;
		} else {
			webView = new WebView();
			tab.setContent(webView);
			WebEngine engine = webView.getEngine();
			// 検索テキスト入力時
			Consumer<String> f = text -> {
				engine.executeScript("window.find(\"" + StringUtils.escapeJavaScript(text) + "\", false, false, true, false, true, false)");
			};
			webView.getProperties().put(App.PROPERTY_SEARCH_FUNCTION, f);
		}
		return webView;
	}

	private static class ThreadLoadService extends Service<ThreadLoadTaskResponseDto> {
		private String url;
		private boolean remote;

		public ThreadLoadService(String url, boolean remote) {
			this.url = url;
			this.remote = remote;
		}

		@Override
		protected Task<ThreadLoadTaskResponseDto> createTask() {
			BBS bbsObject = BBSManager.getBBSFromUrl(url);
			Task<ThreadLoadTaskResponseDto> task = bbsObject.createThreadLoadTask(url, remote);
			// なぜかこれを呼ぶとServiceのOnSucceededも呼ばれるようになる
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
			ThreadLoadTaskResponseDto threadLoadTaskResponseDto = (ThreadLoadTaskResponseDto) event.getSource().getValue();
			tab.getProperties().put(App.TAB_PROPERTY_STATUS_ERROR, threadLoadTaskResponseDto.getErrorMessage());
			App.getInstance().notifyChangeStatus();
			String html = threadLoadTaskResponseDto.getHtml();
			if (html == null) {
				return;
			}

			WebView threadView = (WebView) tab.getContent();
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
						EventListener clickEventListener = new ClickEventListener(engine, url, rightClickEventListener);
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
						EventListener imgLoadEventListener = new ImgLoadEventListener(tab, engine, length, scrollNew);
						for (int i = 0; i < length; i++) {
							Element img = (Element) imgList.item(i);
							((EventTarget) img).addEventListener("load", imgLoadEventListener, false);
							((EventTarget) img).addEventListener("error", imgLoadEventListener, false);
							img.setAttribute("src", img.getAttribute("data-src"));
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

//	private static class MouseOverEventListener implements EventListener {
//		private WebEngine engine;
//		private String url;
//		private EventListener clickEventListener;
//		private EventListener rightClickEventListener;
//
//		public MouseOverEventListener(WebEngine engine, String url,
//				EventListener clickEventListener,
//				EventListener rightClickEventListener) {
//			this.engine = engine;
//			this.url = url;
//			this.clickEventListener = clickEventListener;
//			this.rightClickEventListener = rightClickEventListener;
//		}
//
//		@Override
//		public void handleEvent(Event evt) {
//			if (!(evt instanceof MouseEvent)) {
//				return;
//			}
//			MouseEvent event = (MouseEvent) evt;
//			if (!(event.getTarget() instanceof Node)) {
//				return;
//			}
//			Node target = (Node) event.getTarget();
//			String nodeName = target.getNodeName();
//			// <a href="#xxx">
//			if ("a".equalsIgnoreCase(nodeName)) {
//				NamedNodeMap attributes = target.getAttributes();
//				if (attributes == null) {
//					return;
//				}
//				// レス一覧リンク
//				Node chainNode = attributes.getNamedItem("chain");
//				if (chainNode != null) {
//					String chain = chainNode.getNodeValue();
//					if (chain == null) {
//						return;
//					}
//					App.getInstance().openPopupThread(url, chain, this, clickEventListener, rightClickEventListener);
//					return;
//				}
//			}
//		}
//	}

	private static class ClickEventListener implements EventListener {
		private WebEngine engine;
		private String url;
		private ThreadViewRightClickEventListener rightClickEventListener;

		public ClickEventListener(WebEngine engine, String url,
				ThreadViewRightClickEventListener rightClickEventListener) {
			this.engine = engine;
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
				NamedNodeMap attributes = target.getAttributes();
				if (attributes == null) {
					return true;
				}
				// レスアンカー
//				Node hrefNode = attributes.getNamedItem("href");
//				if (hrefNode != null) {
//					String href = hrefNode.getNodeValue();
//					if (href == null) {
//						return;
//					}
//					if (href.startsWith("#")) {
//						try {
//							engine.executeScript("document.getElementsByName(\"" + href.substring(1) + "\")[0].scrollIntoView(true)");
//						} catch (Exception e) {
//						}
//					}
//					return;
//				}
				// レス一覧リンク
				Node chainNode = attributes.getNamedItem("chain");
				if (chainNode != null) {
					String chain = chainNode.getNodeValue();
					if (chain == null) {
						return true;
					}
					App.getInstance().openPopupThread(url, chain, this, rightClickEventListener);
					return true;
				}
			} else if ("img".equalsIgnoreCase(nodeName)) {
				NamedNodeMap attributes = target.getAttributes();
				if (attributes == null) {
					return true;
				}
				Node srcNode = attributes.getNamedItem("src");
				if (srcNode == null) {
					return true;
				}
				String src = srcNode.getNodeValue();
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
		private Object o = new Object();
		private Tab tab;
		private WebEngine engine;
		private int imgCount;
		private int imgLoadedCount = 0;
		private boolean scrollNew;

		public ImgLoadEventListener(Tab tab, WebEngine engine, int imgCount, boolean scrollNew) {
			this.tab = tab;
			this.engine = engine;
			this.imgCount = imgCount;
			this.scrollNew = scrollNew;
		}

		@Override
		public void handleEvent(Event evt) {
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
	}
}
