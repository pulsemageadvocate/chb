package pulad.chb;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.thymeleaf.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.dto.ThreadLoadTaskResponseDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.thread.ThreadLoadTask;

/**
 * スレッドのID等からポップアップするレス一覧。
 * マウスクリックイベントは親から流用し、アンカーをクリックすると親がアンカー先へジャンプするなどする。
 * @author pulad
 *
 */
public class PopupThreadViewProcessor {

	public static Popup open(
			Window owner,
			double width,
			EventListener clickEventListener,
			ThreadViewRightClickEventListener rightClickEventListener,
			String url,
			List<Integer> resFilter) {
		Popup popup = new Popup();
		Scene scene = popup.getScene();
		scene.getStylesheets().add(Config.styleCss);

		WebView threadView = new WebView();
		AnchorPane rootPane = new AnchorPane(threadView);
		rootPane.setPrefWidth(width);
		rootPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
		rootPane.setStyle("-fx-background-color: black;");
		AnchorPane.setLeftAnchor(threadView, 1d);
		AnchorPane.setTopAnchor(threadView, 1d);
		AnchorPane.setRightAnchor(threadView, 1d);
		AnchorPane.setBottomAnchor(threadView, 1d);
		scene.setRoot(rootPane);

		popup.setAutoFix(true);
		popup.setAutoHide(true);
		popup.show(owner);

		ThreadLoadService service = new ThreadLoadService(url, App.replaceEmoji, resFilter);
		service.setOnSucceeded(new ThreadLoadSucceededEventHandler(popup, threadView, clickEventListener, rightClickEventListener.duplicate(threadView)));
		service.start();

		return popup;
	}

	private static class ThreadLoadService extends Service<ThreadLoadTaskResponseDto> {
		private String url;
		private Boolean replaceEmoji;
		private List<Integer> resFilter;

		public ThreadLoadService(String url, Boolean replaceEmoji, List<Integer> resFilter) {
			this.url = url;
			this.replaceEmoji = replaceEmoji;
			this.resFilter = resFilter;
		}

		@Override
		protected Task<ThreadLoadTaskResponseDto> createTask() {
			BBS bbsObject = BBSManager.getBBSFromUrl(url);
			Task<ThreadLoadTaskResponseDto> task = new ThreadLoadTask(bbsObject.createThreadLoader(url), url, false, replaceEmoji, resFilter);
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
		private Popup popup;
		private WebView threadView;
		//private EventListener mouseOverEventListener;
		private EventListener clickEventListener;
		private EventListener rightClickEventListener;

		public ThreadLoadSucceededEventHandler(
				Popup popup,
				WebView threadView,
				//EventListener mouseOverEventListener,
				EventListener clickEventListener,
				EventListener rightClickEventListener) {
			this.popup = popup;
			this.threadView = threadView;
			//this.mouseOverEventListener = mouseOverEventListener;
			this.clickEventListener = clickEventListener;
			this.rightClickEventListener = rightClickEventListener;
		}

		@Override
		public void handle(WorkerStateEvent event) {
			ThreadLoadTaskResponseDto threadLoadTaskResponseDto = (ThreadLoadTaskResponseDto) event.getSource().getValue();
			String html = threadLoadTaskResponseDto.getHtml();
			if (html == null) {
				return;
			}

			WebEngine engine = threadView.getEngine();
			popup.setOnHidden(new CloseEventListener(engine));
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

						AnchorPane rootPane = (AnchorPane) popup.getScene().getRoot();
//						rootPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
//						rootPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
						double d = ((Integer) engine.executeScript("document.getElementById(\"root\").offsetWidth")).doubleValue();
						rootPane.setPrefWidth((d > 796) ? (d + 4) : 800);
						d = ((Integer) engine.executeScript("document.getElementById(\"root\").offsetHeight")).doubleValue();
						//popup.setHeight((d > 80) ? d : 80);
						rootPane.setPrefHeight((d < 796) ? (d + 4) : 800);

						Element root = document.getElementById("root");
						//((EventTarget) root).addEventListener("mouseover", mouseOverEventListener, false);
						((EventTarget) root).addEventListener("click", clickEventListener, false);
						((EventTarget) root).addEventListener("contextmenu", rightClickEventListener, false);

						NodeList imgList = document.getElementsByTagName("img");
						int length = imgList.getLength();
						EventListener imgLoadEventListener = new ImgLoadEventListener(popup, engine, length);
						for (int i = 0; i < length; i++) {
							Element img = (Element) imgList.item(i);
							((EventTarget) img).addEventListener("load", imgLoadEventListener, false);
							((EventTarget) img).addEventListener("error", imgLoadEventListener, false);
							// gifをサムネイルに表示しない
							String replaceSrc = img.getAttribute("data-view");
							img.setAttribute("src", StringUtils.isEmpty(replaceSrc) ? img.getAttribute("data-src") : replaceSrc);
						}

						//popup.getScene().setOnMouseExited(x -> {popup.hide();});
					}
				}
			});
			engine.loadContent(html, "text/html");
		}
	}

	/**
	 * メモリを解放する。
	 * @author pulad
	 *
	 */
	private static class CloseEventListener implements EventHandler<WindowEvent> {
		private WebEngine engine;

		public CloseEventListener(WebEngine engine) {
			this.engine = engine;
		}

		@Override
		public void handle(WindowEvent event) {
			engine.load("about:blank");
		}
		
	}

	/**
	 * imgの画像の読み込みが完了した時の処理。
	 * @author pulad
	 *
	 */
	private static class ImgLoadEventListener implements EventListener {
		private Popup popup;
		private WebEngine engine;
		private int imgCount;
		private AtomicInteger imgLoadedCount = new AtomicInteger(0);

		public ImgLoadEventListener(Popup popup, WebEngine engine, int imgCount) {
			this.popup = popup;
			this.engine = engine;
			this.imgCount = imgCount;
		}

		@Override
		public void handleEvent(Event evt) {
			if (imgLoadedCount.incrementAndGet() == imgCount) {
				AnchorPane rootPane = (AnchorPane) popup.getScene().getRoot();
//				rootPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
//				rootPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
				double d = ((Integer) engine.executeScript("document.getElementById(\"root\").offsetWidth")).doubleValue();
				rootPane.setPrefWidth((d > 796) ? (d + 4) : 800);
				d = ((Integer) engine.executeScript("document.getElementById(\"root\").offsetHeight")).doubleValue();
				//popup.setHeight((d > 80) ? d : 80);
				rootPane.setPrefHeight((d < 796) ? (d + 4) : 800);
			}
		}
	}
}
