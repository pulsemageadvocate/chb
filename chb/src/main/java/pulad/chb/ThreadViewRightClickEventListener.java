package pulad.chb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import org.thymeleaf.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.MouseEvent;
import org.w3c.dom.html.HTMLImageElement;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.web.WebView;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.constant.ChainIdentifier;
import pulad.chb.dto.AboneBodyDto;
import pulad.chb.dto.AboneIDDto;
import pulad.chb.dto.AboneIPDto;
import pulad.chb.dto.AboneImageDto;
import pulad.chb.dto.AboneNameDto;
import pulad.chb.dto.AboneWacchoiDto;
import pulad.chb.dto.NGFileDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.thread.LinkHistManager;
import pulad.chb.read.thread.LocalURLStreamHandler;
import pulad.chb.util.DomUtil;

public class ThreadViewRightClickEventListener implements EventListener {
	//private static final Pattern regWacchoiLower = Pattern.compile("-[^ -]{4}");
	private static final ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

	private Tab tab;
	/** 親 */
	private WebView threadView;
	/** 操作中のウィンドウ */
	private WebView currentThreadView;
	private String url;

	/**
	 * 親のThreadViewで使用するリスナーを作成する。
	 * @param tab
	 * @param threadView
	 * @param url
	 */
	public ThreadViewRightClickEventListener(Tab tab, WebView threadView, String url) {
		this.tab = tab;
		this.threadView = threadView;
		this.currentThreadView = threadView;
		this.url = url;
	}

	private ThreadViewRightClickEventListener(Tab tab, WebView threadView, WebView currentThreadView, String url) {
		this.tab = tab;
		this.threadView = threadView;
		this.currentThreadView = currentThreadView;
		this.url = url;
	}

	/**
	 * PopupThreadViewで使用するリスナーを作成する。
	 * PopupThreadViewの場合はそっちを親としてメニューを開く。
	 * スレッド更新およびスクロールは親を対象とする。
	 * @param tab
	 * @param threadView
	 * @return
	 */
	public ThreadViewRightClickEventListener duplicate(WebView currentThreadView) {
		return new ThreadViewRightClickEventListener(tab, threadView, currentThreadView, url);
	}

	@Override
	public void handleEvent(Event evt) {
		if (!(evt instanceof MouseEvent)) {
			return;
		}
		MouseEvent event = (MouseEvent) evt;
		evt.preventDefault();
		if (!(evt.getTarget() instanceof Node)) {
			handleDefault(event);
			return;
		}

		if (handleImg(event)) {
			return;
		} else if (handleAboneable(event)) {
			return;
		}
		handleDefault(event);
	}

	private boolean handleImg(MouseEvent event) {
		Node target = (Node) event.getTarget();
		if (!(target instanceof HTMLImageElement)) {
		//if (!"img".equalsIgnoreCase(target.getNodeName())) {
			return false;
		}

		// timeLong検索
		long timeLong = Long.parseLong(DomUtil.searchAttribute(target.getParentNode(), "data-timeLong"));
		int scrollY = (int) threadView.getEngine().executeScript("document.body.scrollTop");

		HTMLImageElement img = (HTMLImageElement) target;
		String src = img.getSrc();
		String httpsrc = LocalURLStreamHandler.getSourceUrl(src);
		String imageFileName = LinkHistManager.getCacheFileName(httpsrc);
		String httpdatasrc = LocalURLStreamHandler.getSourceUrl(DomUtil.getAttribute(img, "data-src"));
		List<MenuItem> itemList = new ArrayList<MenuItem>();
		if (imageFileName == null || imageFileName.startsWith("classpath:")) {
			// notfound.gif等
			MenuItem item = new MenuItem("再読み込み");
			item.setOnAction(e -> {
				// 動いているかよくわからない
				LinkHistManager.delete(httpsrc);
				img.setSrc("");
				img.setSrc(src);
			});
			itemList.add(item);
			item = new MenuItem("URLをコピー");
			item.setOnAction(new CopyAction(httpdatasrc));
			itemList.add(item);
		} else {
			MenuItem item = new MenuItem("ファイル名をコピー");
			item.setOnAction(new CopyAction(imageFileName));
			itemList.add(item);
			item = new MenuItem("URLをコピー");
			item.setOnAction(new CopyAction(httpdatasrc));
			itemList.add(item);
			try {
				String hash = Paths.get(imageFileName).getFileName().toString();
				hash = hash.substring(0, hash.indexOf(".")); // -1の場合は例外
				itemList.add(new SeparatorMenuItem());
				item = new MenuItem("あぼ～ん スレッド");
				item.setOnAction(new AddAboneImageAction(tab, url, getThread(url), hash, false, false, httpsrc, timeLong, scrollY));
				itemList.add(item);
				item = new MenuItem("あぼ～ん 板");
				item.setOnAction(new AddAboneImageAction(tab, url, getBoard(url), hash, false, false, httpsrc, timeLong, scrollY));
				itemList.add(item);
				item = new MenuItem("あぼ～ん BBS");
				item.setOnAction(new AddAboneImageAction(tab, url, getBBS(url), hash, false, false, httpsrc, timeLong, scrollY));
				itemList.add(item);
				item = new MenuItem("あぼ～ん 全部");
				item.setOnAction(new AddAboneImageAction(tab, url, getAll(url), hash, false, false, httpsrc, timeLong, scrollY));
				itemList.add(item);
			} catch (IndexOutOfBoundsException e) {
			}
		}
		ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
		menu.setAutoHide(true);
		menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		return true;
	}

	private boolean handleAboneable(MouseEvent event) {
		Node target = (Node) event.getTarget();
		// aboneable検索
		String aboneable = DomUtil.searchAttribute(target, "aboneable");
		if (aboneable == null) {
			return false;
		}

		// timeLong検索
		long timeLong = Long.parseLong(DomUtil.searchAttribute(target.getParentNode(), "data-timeLong"));
		int scrollY = (int) threadView.getEngine().executeScript("document.body.scrollTop");
		String word = target.getTextContent().trim();
		switch (aboneable.toLowerCase()) {
		case "name":
		{
			List<MenuItem> itemList = new ArrayList<MenuItem>();
			MenuItem item = new MenuItem("コピー");
			item.setOnAction(new CopyAction(currentThreadView));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("あぼ～ん スレッド");
			item.setOnAction(new AddAboneNameAction(tab, url, getThread(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 板");
			item.setOnAction(new AddAboneNameAction(tab, url, getBoard(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん BBS");
			item.setOnAction(new AddAboneNameAction(tab, url, getBBS(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 全部");
			item.setOnAction(new AddAboneNameAction(tab, url, getAll(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん スレッド");
			item.setOnAction(new AddAboneNameAction(tab, url, getThread(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 板");
			item.setOnAction(new AddAboneNameAction(tab, url, getBoard(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん BBS");
			item.setOnAction(new AddAboneNameAction(tab, url, getBBS(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 全部");
			item.setOnAction(new AddAboneNameAction(tab, url, getAll(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("ホワイトリスト スレッド");
			item.setOnAction(new AddAboneNameAction(tab, url, getThread(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 板");
			item.setOnAction(new AddAboneNameAction(tab, url, getBoard(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト BBS");
			item.setOnAction(new AddAboneNameAction(tab, url, getBBS(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 全部");
			item.setOnAction(new AddAboneNameAction(tab, url, getAll(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
			menu.setAutoHide(true);
			menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		}
			break;
		case "wacchoi":
		{
			List<MenuItem> itemList = new ArrayList<MenuItem>();
			MenuItem item = new MenuItem("コピー");
			item.setOnAction(new CopyAction(currentThreadView));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			 item = new MenuItem("あぼ～ん スレッド");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getThread(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 板");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBoard(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん BBS");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBBS(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 全部");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getAll(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん スレッド");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getThread(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 板");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBoard(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん BBS");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBBS(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 全部");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getAll(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("ホワイトリスト スレッド");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getThread(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 板");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBoard(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト BBS");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBBS(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 全部");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getAll(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
			menu.setAutoHide(true);
			menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		}
			break;
		case "wacchoilower": // toLowerCaseのため全部小文字
		{
//			// 元々Lowerだから意味無いかも
//			Matcher matcher = regWacchoiLower.matcher(word);
//			if (!matcher.find()) {
//				return;
//			}
//			String lower = matcher.group();
			List<MenuItem> itemList = new ArrayList<MenuItem>();
			MenuItem item = new MenuItem("コピー");
			item.setOnAction(new CopyAction(currentThreadView));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("あぼ～ん スレッド");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getThread(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 板");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBoard(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん BBS");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBBS(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 全部");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getAll(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん スレッド");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getThread(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 板");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBoard(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん BBS");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBBS(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 全部");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getAll(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("ホワイトリスト スレッド");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getThread(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 板");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBoard(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト BBS");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getBBS(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 全部");
			item.setOnAction(new AddAboneWacchoiAction(tab, url, getAll(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
			menu.setAutoHide(true);
			menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		}
			break;
		case "ip":
		{
			List<MenuItem> itemList = new ArrayList<MenuItem>();
			MenuItem item = new MenuItem("コピー");
			item.setOnAction(new CopyAction(currentThreadView));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("あぼ～ん スレッド");
			item.setOnAction(new AddAboneIPAction(tab, url, getThread(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 板");
			item.setOnAction(new AddAboneIPAction(tab, url, getBoard(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん BBS");
			item.setOnAction(new AddAboneIPAction(tab, url, getBBS(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 全部");
			item.setOnAction(new AddAboneIPAction(tab, url, getAll(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん スレッド");
			item.setOnAction(new AddAboneIPAction(tab, url, getThread(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 板");
			item.setOnAction(new AddAboneIPAction(tab, url, getBoard(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん BBS");
			item.setOnAction(new AddAboneIPAction(tab, url, getBBS(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 全部");
			item.setOnAction(new AddAboneIPAction(tab, url, getAll(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("ホワイトリスト スレッド");
			item.setOnAction(new AddAboneIPAction(tab, url, getThread(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 板");
			item.setOnAction(new AddAboneIPAction(tab, url, getBoard(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト BBS");
			item.setOnAction(new AddAboneIPAction(tab, url, getBBS(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 全部");
			item.setOnAction(new AddAboneIPAction(tab, url, getAll(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
			menu.setAutoHide(true);
			menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		}
			break;
		case "id":
		{
			List<MenuItem> itemList = new ArrayList<MenuItem>();
			MenuItem item = new MenuItem("コピー");
			item.setOnAction(new CopyAction(currentThreadView));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("あぼ～ん スレッド");
			item.setOnAction(new AddAboneIDAction(tab, url, getThread(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 板");
			item.setOnAction(new AddAboneIDAction(tab, url, getBoard(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん BBS");
			item.setOnAction(new AddAboneIDAction(tab, url, getBBS(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("あぼ～ん 全部");
			item.setOnAction(new AddAboneIDAction(tab, url, getAll(url), word, false, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん スレッド");
			item.setOnAction(new AddAboneIDAction(tab, url, getThread(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 板");
			item.setOnAction(new AddAboneIDAction(tab, url, getBoard(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん BBS");
			item.setOnAction(new AddAboneIDAction(tab, url, getBBS(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("透明あぼ～ん 全部");
			item.setOnAction(new AddAboneIDAction(tab, url, getAll(url), word, false, true, word, timeLong, scrollY));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("ホワイトリスト スレッド");
			item.setOnAction(new AddAboneIDAction(tab, url, getThread(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 板");
			item.setOnAction(new AddAboneIDAction(tab, url, getBoard(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト BBS");
			item.setOnAction(new AddAboneIDAction(tab, url, getBBS(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 全部");
			item.setOnAction(new AddAboneIDAction(tab, url, getAll(url), word, true, false, word, timeLong, scrollY));
			itemList.add(item);
			ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
			menu.setAutoHide(true);
			menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		}
			break;
		default:
			return false;
		}
		return true;
	}

	private void handleDefault(MouseEvent event) {
		Node selectNode = (Node) currentThreadView.getEngine().executeScript("window.getSelection().anchorNode");
		String selection = (String) currentThreadView.getEngine().executeScript("window.getSelection().toString()");
		int scrollY = (int) threadView.getEngine().executeScript("document.body.scrollTop");
		// timeLong検索
		long timeLong = 0L;
		if (selectNode != null) {
			timeLong = Long.parseLong(DomUtil.searchAttribute(selectNode, "data-timeLong"));
		}
		if (StringUtils.isEmpty(selection)) {
			List<MenuItem> itemList = new ArrayList<MenuItem>();
			MenuItem item = new MenuItem("コピー");
			item.setOnAction(new CopyAction(currentThreadView));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("再読み込み");
			item.setOnAction(x -> {
				ThreadViewProcessor.open(tab, url, true, scrollY);
			});
			itemList.add(item);
			item = new MenuItem("ログ編集");
			item.setOnAction(x -> {
				BBS bbsObject = BBSManager.getBBSFromUrl(url);
				String bbs = bbsObject.getLogDirectoryName();
				String board = bbsObject.getBoardFromThreadUrl(url);
				String logFileName = bbsObject.getDatFileNameFromThreadUrl(url);
				String log = Config.getLogFolder().resolve(bbs).resolve(board).resolve(logFileName).toFile().getPath();
				
				try {
					Runtime.getRuntime().exec(Config.editorCommand.replaceAll("\\$LINK", Matcher.quoteReplacement(log)));
				} catch (IOException e) {
					App.logger.error("ログ編集失敗", e);
				}
			});
			itemList.add(item);
			item = new MenuItem("chb.txt編集 スレッド");
			item.setOnAction(new OpenAboneFileAction(getThread(url)));
			itemList.add(item);
			item = new MenuItem("chb.txt編集 板");
			item.setOnAction(new OpenAboneFileAction(getBoard(url)));
			itemList.add(item);
			item = new MenuItem("chb.txt編集 BBS");
			item.setOnAction(new OpenAboneFileAction(getBBS(url)));
			itemList.add(item);
			item = new MenuItem("chb.txt編集 全部");
			item.setOnAction(new OpenAboneFileAction(getAll(url)));
			itemList.add(item);
			ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
			menu.setAutoHide(true);
			menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		} else {
			List<MenuItem> itemList = new ArrayList<MenuItem>();
			MenuItem item = new MenuItem("コピー");
			item.setOnAction(new CopyAction(currentThreadView));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("本文あぼ～ん スレッド");
			item.setOnAction(new AddAboneBodyAction(tab, url, getThread(url), selection, false, false, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("本文あぼ～ん 板");
			item.setOnAction(new AddAboneBodyAction(tab, url, getBoard(url), selection, false, false, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("本文あぼ～ん BBS");
			item.setOnAction(new AddAboneBodyAction(tab, url, getBBS(url), selection, false, false, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("本文あぼ～ん 全部");
			item.setOnAction(new AddAboneBodyAction(tab, url, getAll(url), selection, false, false, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("本文透明あぼ～ん スレッド");
			item.setOnAction(new AddAboneBodyAction(tab, url, getThread(url), selection, false, true, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("本文透明あぼ～ん 板");
			item.setOnAction(new AddAboneBodyAction(tab, url, getBoard(url), selection, false, true, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("本文透明あぼ～ん BBS");
			item.setOnAction(new AddAboneBodyAction(tab, url, getBBS(url), selection, false, true, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("本文透明あぼ～ん 全部");
			item.setOnAction(new AddAboneBodyAction(tab, url, getAll(url), selection, false, true, selection, timeLong, scrollY));
			itemList.add(item);
			itemList.add(new SeparatorMenuItem());
			item = new MenuItem("ホワイトリスト スレッド");
			item.setOnAction(new AddAboneBodyAction(tab, url, getThread(url), selection, true, false, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 板");
			item.setOnAction(new AddAboneBodyAction(tab, url, getBoard(url), selection, true, false, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト BBS");
			item.setOnAction(new AddAboneBodyAction(tab, url, getBBS(url), selection, true, false, selection, timeLong, scrollY));
			itemList.add(item);
			item = new MenuItem("ホワイトリスト 全部");
			item.setOnAction(new AddAboneBodyAction(tab, url, getAll(url), selection, true, false, selection, timeLong, scrollY));
			itemList.add(item);
			ContextMenu menu = new ContextMenu(itemList.toArray(new MenuItem[itemList.size()]));
			menu.setAutoHide(true);
			menu.show(currentThreadView.getScene().getWindow(), event.getScreenX(), event.getScreenY());
		}
	}

	protected File getAll(String url) {
		return Config.getRootFolder().resolve("chb.txt").toFile();
	}

	protected File getBBS(String url) {
		BBS bbsObject = BBSManager.getBBSFromUrl(url);
		String bbsDir = bbsObject.getBBSDirectoryName();

		return Config.getBBSFolder().resolve(bbsDir).resolve("chb.txt").toFile();
	}

	protected File getBoard(String url) {
		BBS bbsObject = BBSManager.getBBSFromUrl(url);
		String bbs = bbsObject.getLogDirectoryName();
		String board = bbsObject.getBoardFromThreadUrl(url);

		return Config.getLogFolder().resolve(bbs).resolve(board).resolve("chb.txt").toFile();
	}

	protected File getThread(String url) {
		BBS bbsObject = BBSManager.getBBSFromUrl(url);
		String bbs = bbsObject.getLogDirectoryName();
		String board = bbsObject.getBoardFromThreadUrl(url);
		String threadNGFileName = bbsObject.getThreadFromThreadUrl(url) + ".chb.txt";

		return Config.getLogFolder().resolve(bbs).resolve(board).resolve(threadNGFileName).toFile();
	}

	private static class CopyAction implements EventHandler<ActionEvent> {
		private String text;

		private CopyAction(String text) {
			this.text = text;
		}

		private CopyAction(WebView currentThreadView) {
			//currentThreadView.getEngine().executeScript("document.execCommand(\"copy\");");
			String selection = (String) currentThreadView.getEngine().executeScript("window.getSelection().toString()");
			if (!StringUtils.isEmptyOrWhitespace(selection)) {
				this.text = selection;
			}
		}

		@Override
		public void handle(ActionEvent event) {
			if (!StringUtils.isEmptyOrWhitespace(text)) {
				HashMap<DataFormat, Object> content = new HashMap<>();
				content.put(DataFormat.PLAIN_TEXT, text);
				Clipboard.getSystemClipboard().setContent(content);
			}
		}
	}

	private static abstract class AddAboneAction implements EventHandler<ActionEvent> {
		protected Tab tab;
		protected String url;
		protected File file;
		protected boolean white;
		protected boolean invisible;
		protected String label;
		protected long createDate;
		protected int scrollY;

		protected AddAboneAction(Tab tab, String url, File file, boolean white, boolean invisible, String label, long createDate, int scrollY) {
			this.tab = tab;
			this.url = url;
			this.file = file;
			this.white = white;
			this.invisible = invisible;
			this.label = label;
			this.createDate = createDate;
			this.scrollY = scrollY;
		}

		protected NGFileDto readFile() {
			NGFileDto dto = null;
			if (file.exists()) {
				try {
					dto = mapper.readValue(file, NGFileDto.class);
				} catch (IOException e) {
					App.logger.error("readFile失敗", e);
					return null;
				}
			} else {
				dto = new NGFileDto();
			}
			return dto;
		}

		protected void writeFile(NGFileDto dto) {
			try {
				mapper.writeValue(file, dto);
			} catch (IOException e) {
				App.logger.error("writeFile失敗", e);
			}
		}
	}

	private static class AddAboneNameAction extends AddAboneAction {
		private String word;

		private AddAboneNameAction(Tab tab, String url, File file, String word, boolean white, boolean invisible, String label, long createDate, int scrollY) {
			super(tab, url, file, white, invisible, label, createDate, scrollY);
			this.word = word;
		}

		@Override
		public void handle(ActionEvent event) {
			addAboneName();
			ThreadViewProcessor.open(tab, url, false, scrollY);
		}

		private void addAboneName() {
			NGFileDto ngFileDto = readFile();
			if (ngFileDto == null) {
				return;
			}
			List<AboneNameDto> nameList = ngFileDto.getName();
			if (nameList == null) {
				nameList = new ArrayList<>();
				ngFileDto.setName(nameList);
			}
			AboneNameDto name = new AboneNameDto();
			name.setWord(word);
			name.setRegex(false);
			name.setWhite(white);
			name.setInvisible(invisible);
			name.setLabel(label);
			name.setChainIdentifier(ChainIdentifier.ID);
			name.setReferenceChain(1);
			name.setCreateDate(createDate);
			name.setDurationDay(0);
			nameList.add(name);
			writeFile(ngFileDto);
		}
	}

	private static class AddAboneWacchoiAction extends AddAboneAction {
		private String word;

		private AddAboneWacchoiAction(Tab tab, String url, File file, String word, boolean white, boolean invisible, String label, long createDate, int scrollY) {
			super(tab, url, file, white, invisible, label, createDate, scrollY);
			this.word = word;
		}

		@Override
		public void handle(ActionEvent event) {
			addAboneWacchoi();
			ThreadViewProcessor.open(tab, url, false, scrollY);
		}

		private void addAboneWacchoi() {
			NGFileDto ngFileDto = readFile();
			if (ngFileDto == null) {
				return;
			}
			List<AboneWacchoiDto> wacchoiList = ngFileDto.getWacchoi();
			if (wacchoiList == null) {
				wacchoiList = new ArrayList<>();
				ngFileDto.setWacchoi(wacchoiList);
			}
			AboneWacchoiDto wacchoi = new AboneWacchoiDto();
			wacchoi.setWord(word);
			wacchoi.setRegex(false);
			wacchoi.setWhite(white);
			wacchoi.setInvisible(invisible);
			wacchoi.setLabel(label);
			wacchoi.setChainIdentifier(ChainIdentifier.ID);
			wacchoi.setReferenceChain(1);
			wacchoi.setCreateDate(createDate);
			wacchoi.setDurationDay(7);
			wacchoiList.add(wacchoi);
			writeFile(ngFileDto);
		}
	}

	private static class AddAboneIDAction extends AddAboneAction {
		private String word;

		private AddAboneIDAction(Tab tab, String url, File file, String word, boolean white, boolean invisible, String label, long createDate, int scrollY) {
			super(tab, url, file, white, invisible, label, createDate, scrollY);
			this.word = word;
		}

		@Override
		public void handle(ActionEvent event) {
			addAboneID();
			ThreadViewProcessor.open(tab, url, false, scrollY);
		}

		private void addAboneID() {
			NGFileDto ngFileDto = readFile();
			if (ngFileDto == null) {
				return;
			}
			List<AboneIDDto> idList = ngFileDto.getId();
			if (idList == null) {
				idList = new ArrayList<>();
				ngFileDto.setId(idList);
			}
			AboneIDDto id = new AboneIDDto();
			id.setWord(word);
			id.setRegex(false);
			id.setWhite(white);
			id.setInvisible(invisible);
			id.setLabel(label);
			id.setChainIdentifier(ChainIdentifier.NONE);
			id.setReferenceChain(1);
			id.setCreateDate(createDate);
			id.setDurationDay(1);
			idList.add(id);
			writeFile(ngFileDto);
		}
	}

	private static class AddAboneIPAction extends AddAboneAction {
		private String word;

		private AddAboneIPAction(Tab tab, String url, File file, String word, boolean white, boolean invisible, String label, long createDate, int scrollY) {
			super(tab, url, file, white, invisible, label, createDate, scrollY);
			this.word = word;
		}

		@Override
		public void handle(ActionEvent event) {
			addAboneIP();
			ThreadViewProcessor.open(tab, url, false, scrollY);
		}

		private void addAboneIP() {
			NGFileDto ngFileDto = readFile();
			if (ngFileDto == null) {
				return;
			}
			List<AboneIPDto> ipList = ngFileDto.getIp();
			if (ipList == null) {
				ipList = new ArrayList<>();
				ngFileDto.setIp(ipList);
			}
			AboneIPDto ip = new AboneIPDto();
			ip.setWord(word);
			ip.setRegex(false);
			ip.setWhite(white);
			ip.setInvisible(invisible);
			ip.setLabel(label);
			ip.setChainIdentifier(ChainIdentifier.ID);
			ip.setReferenceChain(1);
			ip.setCreateDate(createDate);
			ip.setDurationDay(0);
			ipList.add(ip);
			writeFile(ngFileDto);
		}
	}

	private static class AddAboneBodyAction extends AddAboneAction {
		private String word;

		private AddAboneBodyAction(Tab tab, String url, File file, String word, boolean white, boolean invisible, String label, long createDate, int scrollY) {
			super(tab, url, file, white, invisible, label, createDate, scrollY);
			this.word = word;
		}

		@Override
		public void handle(ActionEvent event) {
			addAboneBody();
			ThreadViewProcessor.open(tab, url, false, scrollY);
		}

		private void addAboneBody() {
			NGFileDto ngFileDto = readFile();
			if (ngFileDto == null) {
				return;
			}
			List<AboneBodyDto> bodyList = ngFileDto.getBody();
			if (bodyList == null) {
				bodyList = new ArrayList<>();
				ngFileDto.setBody(bodyList);
			}
			AboneBodyDto body = new AboneBodyDto();
			body.setWord(word);
			body.setRegex(false);
			body.setWhite(white);
			body.setInvisible(invisible);
			body.setLabel(label);
			body.setChainIdentifier(ChainIdentifier.ID);
			body.setReferenceChain(1);
			body.setCreateDate(createDate);
			body.setDurationDay(0);
			bodyList.add(body);
			writeFile(ngFileDto);
		}
	}

	private static class AddAboneImageAction extends AddAboneAction {
		private String hash;

		private AddAboneImageAction(Tab tab, String url, File file, String hash, boolean white, boolean invisible, String label, long createDate, int scrollY) {
			super(tab, url, file, white, invisible, label, createDate, scrollY);
			this.hash = hash;
		}

		@Override
		public void handle(ActionEvent event) {
			addAboneImage();
			ThreadViewProcessor.open(tab, url, false, scrollY);
		}

		private void addAboneImage() {
			NGFileDto ngFileDto = readFile();
			if (ngFileDto == null) {
				return;
			}
			List<AboneImageDto> imageList = ngFileDto.getImage();
			if (imageList == null) {
				imageList = new ArrayList<>();
				ngFileDto.setImage(imageList);
			}
			AboneImageDto image = new AboneImageDto();
			image.setHash(hash);
			image.setWhite(white);
			image.setInvisible(invisible);
			image.setLabel(label);
			image.setChainIdentifier(ChainIdentifier.ID);
			image.setReferenceChain(1);
			image.setCreateDate(createDate);
			image.setDurationDay(0);
			imageList.add(image);
			writeFile(ngFileDto);
		}
	}

	private static class OpenAboneFileAction implements EventHandler<ActionEvent> {
		private File file;

		private OpenAboneFileAction(File file) {
			this.file = file;
		}

		@Override
		public void handle(ActionEvent event) {
			try {
				Runtime.getRuntime().exec(Config.editorCommand.replaceAll("\\$LINK", Matcher.quoteReplacement(file.getPath())));
			} catch (IOException e) {
				App.logger.error("OpenAboneFileAction失敗", e);
			}
		}
	}
}
