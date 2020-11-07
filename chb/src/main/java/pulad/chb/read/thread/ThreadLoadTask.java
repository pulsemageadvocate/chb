package pulad.chb.read.thread;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javafx.concurrent.Task;
import pulad.chb.App;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.constant.AboneLevel;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadLoadTaskResponseDto;
import pulad.chb.dto.ThreadResponseDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.ThreadLoader;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.NumberUtil;

public class ThreadLoadTask extends Task<ThreadLoadTaskResponseDto> {
	private static final Logger logger = LoggerFactory.getLogger(ThreadLoadTask.class);
	protected static final String templateFileName = "ThreadTemplate";
	protected static final String errorTemplateFileName = "ErrorTemplate";

	private ThreadLoadProcessor threadLoadProcessor;
	private final TemplateEngine templateEngine;
	protected final String urlStr;
	protected final boolean filtered;
	protected final boolean remote;

	public ThreadLoadTask(ThreadLoader threadLoader, String url) {
		this(threadLoader, url, true, true, null);
	}

	public ThreadLoadTask(ThreadLoader threadLoader, String url, boolean remote) {
		this(threadLoader, url, remote, true, null);
	}

	public ThreadLoadTask(ThreadLoader threadLoader, String url, boolean remote, boolean replaceEmoji) {
		this(threadLoader, url, remote, replaceEmoji, null);
	}

	public ThreadLoadTask(ThreadLoader threadLoader, String url, boolean remote, boolean replaceEmoji, Collection<Integer> resFilter) {
		this.threadLoadProcessor = new ThreadLoadProcessor(threadLoader, url, replaceEmoji, resFilter);
		this.urlStr = url;
		this.remote = remote;
		this.filtered = (resFilter != null);

		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setCharacterEncoding("UTF-8");
		// キャッシュはデフォルトで有効、時間無制限。
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setPrefix("templates/");
		resolver.setSuffix(".html");
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(resolver);
	}

	@Override
	protected ThreadLoadTaskResponseDto call() {
		logger.debug("ThreadLoadTask start");

		ThreadLoadTaskResponseDto threadLoadTaskResponseDto = new ThreadLoadTaskResponseDto();

		try {
			if (this.isCancelled()) {
				throw new InterruptedException();
			}

			long now = DateTimeUtil.localDateTimeToHttpLong(LocalDateTime.now());

			// dat読み込み
			BBS bbsObject = BBSManager.getBBSFromUrl(urlStr);
			BoardDto boardDto = BoardManager.get(bbsObject.getBoardUrlFromThreadUrl(urlStr), remote);
			ConcurrentHashMap<String, String> setting = boardDto.getSetting();

			TreeMap<Integer, ResDto> res = new TreeMap<>();
			threadLoadProcessor.readDat(setting, res);
			int lastResCount = res.isEmpty() ? 0 : res.lastKey();
			int newResCount = 0;

			// read.cgi/rawmode.cgiから取得
			if (remote) {
				logger.debug("ThreadLoadTask ThreadLoadProcessor.request start");
				ThreadResponseDto threadResponseDto = threadLoadProcessor.request(setting, res, now);
				logger.debug("ThreadLoadTask ThreadLoadProcessor.request end");
				newResCount = res.lastKey() - lastResCount;
				if (threadResponseDto.getData() == null) {
					// エラー時
					threadLoadTaskResponseDto.setErrorMessage(threadResponseDto.getResponseMessage());
				}
			}

			if (this.isCancelled()) {
				throw new InterruptedException();
			}

			// 加工
			logger.debug("ThreadLoadTask ThreadLoadProcessor.applyResProcessor start");
			threadLoadProcessor.applyResProcessor(res, now);
			logger.debug("ThreadLoadTask ThreadLoadProcessor.applyResProcessor end");

			if (this.isCancelled()) {
				throw new InterruptedException();
			}

			// resFilter適用
			// 加工の後にしないとレス数等がカウントできない
			logger.debug("ThreadLoadTask ThreadLoadProcessor.applyFilter start");
			res = threadLoadProcessor.applyFilter(res);
			logger.debug("ThreadLoadTask ThreadLoadProcessor.applyFilter end");

			if (this.isCancelled()) {
				throw new InterruptedException();
			}

			// html生成
			org.thymeleaf.context.Context context = new org.thymeleaf.context.Context(Locale.JAPANESE);
			// 件名を入れる
			context.setVariable("title", res.isEmpty() ? "不明" : res.firstEntry().getValue().getTitle().trim() + (remote ? ("(" + newResCount + ")") : ""));
			context.setVariable("resMap", res);
			context.setVariable("lastResCount", NumberUtil.integerCache(lastResCount));
			context.setVariable("newResCount", NumberUtil.integerCache(remote ? newResCount : -1));
			context.setVariable("filtered", Boolean.valueOf(filtered));
			context.setVariable("errorMessage", threadLoadTaskResponseDto.getErrorMessage());
			// enum定数 SpELでないからT()が使えない
			context.setVariable("ABONE_LEVEL_WHITE", AboneLevel.WHITE);
			context.setVariable("ABONE_LEVEL_NONE", AboneLevel.NONE);
			context.setVariable("ABONE_LEVEL_ABONE", AboneLevel.ABONE);
			context.setVariable("ABONE_LEVEL_INVISIBLE", AboneLevel.INVISIBLE);
			logger.debug("ThreadLoadTask TemplateEngine start");
			threadLoadTaskResponseDto.setHtml(templateEngine.process(templateFileName, context));
			logger.debug("ThreadLoadTask TemplateEngine end");

			logger.debug("ThreadLoadTask end");
			return threadLoadTaskResponseDto;
		} catch (Exception e) {
			logger.debug("ThreadLoadTask error", e);
			return errorProcess(e);
		}
	}

	private ThreadLoadTaskResponseDto errorProcess(Exception e) {
		App.logger.error("AbstractThreadLoadTask失敗", e);
		ThreadLoadTaskResponseDto threadLoadTaskResponseDto = new ThreadLoadTaskResponseDto();
		threadLoadTaskResponseDto.setErrorMessage(e.getMessage());

		// html生成
		org.thymeleaf.context.Context context = new org.thymeleaf.context.Context(Locale.JAPANESE);
		context.setVariable("exception", e);
		logger.debug("ThreadLoadTask TemplateEngine start");
		threadLoadTaskResponseDto.setHtml(templateEngine.process(errorTemplateFileName, context));
		logger.debug("ThreadLoadTask TemplateEngine end");

		return threadLoadTaskResponseDto;
	}
}
