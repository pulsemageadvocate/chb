package pulad.chb.read.thread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javafx.concurrent.Task;
import pulad.chb.App;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.config.Config;
import pulad.chb.constant.AboneLevel;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.dto.ThreadLoadTaskResponseDto;
import pulad.chb.dto.ThreadResponseDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.ResProcessor;
import pulad.chb.interfaces.ThreadLoader;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.NumberUtil;

public class ThreadLoadTask extends Task<ThreadLoadTaskResponseDto> {
	protected static final String templateFileName = "ThreadTemplate";
	protected static final String errorTemplateFileName = "ErrorTemplate";

	private ThreadLoader threadLoader;
	private List<ResProcessor> resProcessors;
	private final TemplateEngine templateEngine;
	protected final String urlStr;
	protected final boolean remote;
	protected final boolean replaceEmoji;
	protected final Set<Integer> resFilter;
	protected final BBS bbsObject;
	protected final String bbs;
	protected final String board;
	protected final String datFileName;

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
		this.threadLoader = threadLoader;
		this.urlStr = url;
		this.remote = remote;
		this.replaceEmoji = replaceEmoji;
		this.resFilter = (resFilter == null) ? null : new TreeSet<Integer>(resFilter);
		this.resProcessors = createResProcessors();

		this.bbsObject = BBSManager.getBBSFromUrl(urlStr);
		this.bbs = bbsObject.getLogDirectoryName();
		this.board = bbsObject.getBoardFromThreadUrl(urlStr);
		this.datFileName = bbsObject.getDatFileNameFromThreadUrl(urlStr);

		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setCharacterEncoding("UTF-8");
		// キャッシュはデフォルトで有効、時間無制限。
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setPrefix("templates/");
		resolver.setSuffix(".html");
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(resolver);
	}

	private List<ResProcessor> createResProcessors() {
		List<ResProcessor> resProcessors = new LinkedList<>();
		resProcessors.add(new ReplaceStrResProcessor());
		resProcessors.add(new RemoveAnchorResProcessor());
		resProcessors.add(new AnchorLinkResProcessor());
		resProcessors.add(new ImageResProcessor());
		resProcessors.add(new CountResProcessor());
		resProcessors.add(new LinkPopupResProcessor());
		resProcessors.add(new AboneResProcessor());
		resProcessors.add(new BodyNGAnchorResProcessor());
		if (replaceEmoji) {
			resProcessors.add(new ReplaceNumericalCharacterReferenceResProcessor());
		}
		resProcessors.add(new ReplaceErrorCharacterResProcessor());
		return resProcessors;
	}

	@Override
	protected ThreadLoadTaskResponseDto call() {
		ThreadLoadTaskResponseDto threadLoadTaskResponseDto = new ThreadLoadTaskResponseDto();

		try {
			long now = DateTimeUtil.localDateTimeToHttpLong(LocalDateTime.now());

			// dat読み込み
			BoardDto boardDto = BoardManager.get(bbsObject.getBoardUrlFromThreadUrl(urlStr), remote);

			TreeMap<Integer, ResDto> res = new TreeMap<>();
			readDat(boardDto, res);
			int lastResCount = res.isEmpty() ? 0 : res.lastKey();
			int newResCount = 0;

			// read.cgi/rawmode.cgiから取得
			if (remote) {
				ThreadResponseDto threadResponseDto = threadLoader.request(res, now);
				if (threadResponseDto.getData() != null) {
					int lastRes = res.isEmpty() ? 0 : res.lastKey();

					readDat(boardDto, res, threadResponseDto.getData());
					newResCount = res.lastKey() - lastResCount;
					writeDat(res, lastRes, threadResponseDto);
				} else if (threadResponseDto.getResponseCode() != 200) {
					// エラー時
					threadLoadTaskResponseDto.setErrorMessage(threadResponseDto.getResponseMessage());
				}
			}

			// 加工
			for (ResProcessor processor : resProcessors) {
				processor.process(urlStr, res, remote, now);
			}

			// resFilter適用
			// 加工の後にしないとレス数等がカウントできない
			if (resFilter != null) {
				TreeMap<Integer, ResDto> filtered = res.entrySet().stream()
						.filter(x -> resFilter.contains(x.getKey()))
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, TreeMap::new));
				res.clear();
				res = filtered;
			}

			// html生成
			org.thymeleaf.context.Context context = new org.thymeleaf.context.Context(Locale.JAPANESE);
			// 件名を入れる
			context.setVariable("title", res.isEmpty() ? "不明" : res.firstEntry().getValue().getTitle().trim() + (remote ? ("(" + newResCount + ")") : ""));
			context.setVariable("resMap", res);
			context.setVariable("lastResCount", NumberUtil.integerCache(lastResCount));
			context.setVariable("newResCount", NumberUtil.integerCache(remote ? newResCount : -1));
			context.setVariable("filtered", Boolean.valueOf(resFilter != null));
			context.setVariable("errorMessage", threadLoadTaskResponseDto.getErrorMessage());
			// enum定数 SpELでないからT()が使えない
			context.setVariable("ABONE_LEVEL_NONE", AboneLevel.NONE);
			context.setVariable("ABONE_LEVEL_ABONE", AboneLevel.ABONE);
			context.setVariable("ABONE_LEVEL_INVISIBLE", AboneLevel.INVISIBLE);
			threadLoadTaskResponseDto.setHtml(templateEngine.process(templateFileName, context));

			return threadLoadTaskResponseDto;
		} catch (Exception e) {
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
		threadLoadTaskResponseDto.setHtml(templateEngine.process(errorTemplateFileName, context));

		return threadLoadTaskResponseDto;
	}

	/**
	 * DATファイルからレスを取得してListに追加する。
	 * @param boardDto
	 * @param res
	 */
	private void readDat(BoardDto boardDto, TreeMap<Integer, ResDto> res) {
		BBS bbsObject = BBSManager.getBBSFromLogDirectoryName(bbs);
		try {
			threadLoader.readDat(boardDto, res, new BufferedReader(new FileReader(Config.getLogFolder().resolve(bbs).resolve(board).resolve(datFileName).toString(), bbsObject.getCharset())));
		} catch (Exception e) {
			// 読めない場合は再取得
		}
	}

	/**
	 * readcgi.js処理済みの文字列からレスを取得してListに追加する。
	 * @param boardDto
	 * @param res
	 * @param source
	 */
	private void readDat(BoardDto boardDto, TreeMap<Integer, ResDto> res, String source) throws IOException {
		threadLoader.readDat(boardDto, res, new BufferedReader(new StringReader(source)));
	}

	/**
	 * 新着レスをDATファイルに書き込む。
	 * threadst.txtを更新する。
	 * @param res
	 * @param startIndex
	 * @param threadResponseDto
	 */
	private void writeDat(TreeMap<Integer, ResDto> res, int lastRes, ThreadResponseDto threadResponseDto) throws IOException {
		Path path = Config.getLogFolder().resolve(bbs).resolve(board);
		File file = path.resolve(datFileName).toFile();
		BufferedWriter bw = null;
		try {
			// ディレクトリを作成する
			Files.createDirectories(path);
			bw = new BufferedWriter(new FileWriter(
					file,
					bbsObject.getCharset(),
					true));
			for (ResDto resDto : res.tailMap(NumberUtil.integerCache(lastRes), false).values()) {
				bw.write(resDto.getSource());
				bw.write("\n");
			}
			bw.flush();
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
				}
				bw = null;
			}
		}

		// threadst.txt
		String boardUrl = bbsObject.getBoardUrlFromThreadUrl(urlStr);
		BoardDto boardDto = BoardManager.get(boardUrl);
		if (boardDto == null) {
			return;
		}
		ThreadDto threadDto = boardDto.getLogThread().get(datFileName);
		if (threadDto == null) {
			threadDto = new ThreadDto();
			threadDto.setBoardUrl(boardUrl);
			threadDto.setDatName(datFileName);
			boardDto.getLogThread().put(datFileName, threadDto);
		}
		Map.Entry<Integer, ResDto> first = res.firstEntry();
		Map.Entry<Integer, ResDto> last = res.lastEntry();
		threadDto.setLogCount(last.getKey());
		threadDto.settLastGet(DateTimeUtil.httpLongToLocalDateTime(threadResponseDto.getDate()));
		threadDto.setResCount(last.getKey());
		threadDto.setnLastNRes(lastRes);
		threadDto.setBuildTime(DateTimeUtil.httpLongToLocalDateTime(first.getValue().getTimeLong()));
		// 最終書き込みを検索するがover1000を回避する
		Map.Entry<Integer, ResDto> tLastEntry = last;
		long tLast = 0L;
		while (tLast <= 0L && tLastEntry != null) {
			tLast = tLastEntry.getValue().getTimeLong();
			tLastEntry = res.lowerEntry(tLastEntry.getKey());
		}
		threadDto.settLast(DateTimeUtil.httpLongToLocalDateTime(tLast));
		threadDto.setnLogSize(file.length());
		threadDto.setDate(DateTimeUtil.httpLongToLocalDateTime(threadResponseDto.getDate()));
		threadDto.setTitle(first.getValue().getTitle());
		BoardManager.updateThreadst(boardDto);
	}
}
