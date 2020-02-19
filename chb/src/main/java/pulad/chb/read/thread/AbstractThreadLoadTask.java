package pulad.chb.read.thread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.Collection;
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
import pulad.chb.bbs.BBS;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.constant.AboneLevel;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.dto.ThreadResponseDto;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.NumberUtil;

public abstract class AbstractThreadLoadTask extends Task<String> {

	protected static final String templateFileName = "ThreadTemplate";
	protected static final String errorTemplateFileName = "ErrorTemplate";

	private List<ResProcessor> resProcessors;
	private final TemplateEngine templateEngine;
	protected final String urlStr;
	protected final boolean remote;
	protected final Set<Integer> resFilter;
	protected final BBS bbsObject;
	protected final String bbs;
	protected final String board;
	protected final String datFileName;
	protected long now;

	public AbstractThreadLoadTask(String url) {
		this(url, true, null);
	}

	public AbstractThreadLoadTask(String url, boolean remote) {
		this(url, remote, null);
	}

	public AbstractThreadLoadTask(String url, boolean remote, Collection<Integer> resFilter) {
		this.urlStr = url;
		this.remote = remote;
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

	protected abstract List<ResProcessor> createResProcessors();

	@Override
	protected String call() {
		try {
			this.now = DateTimeUtil.localDateTimeToHttpLong(LocalDateTime.now());

			// dat読み込み
			TreeMap<Integer, ResDto> res = new TreeMap<>();
			readDat(res);
			int lastResCount = res.isEmpty() ? 0 : res.lastKey();
			int newResCount = 0;

			// read.cgi/rawmode.cgiから取得
			if (remote) {
				ThreadResponseDto threadResponseDto = request(res);
				if (threadResponseDto.getData() != null) {
					int lastRes = res.isEmpty() ? 0 : res.lastKey();
					readDat(res, threadResponseDto.getData());
					newResCount = res.lastKey() - lastResCount;
					writeDat(res, lastRes, threadResponseDto);
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
			// enum定数 SpELでないからT()が使えない
			context.setVariable("ABONE_LEVEL_NONE", AboneLevel.NONE);
			context.setVariable("ABONE_LEVEL_ABONE", AboneLevel.ABONE);
			context.setVariable("ABONE_LEVEL_INVISIBLE", AboneLevel.INVISIBLE);
			return templateEngine.process(templateFileName, context);
		} catch (Exception e) {
			return errorProcess(e);
		}
	}

	private String errorProcess(Exception e) {
		// html生成
		org.thymeleaf.context.Context context = new org.thymeleaf.context.Context(Locale.JAPANESE);
		context.setVariable("exception", e);
		return templateEngine.process(errorTemplateFileName, context);
	}

	/**
	 * DATファイルからレスを取得してListに追加する。
	 * @param res
	 * @param bbs
	 * @param board
	 * @param datFileName
	 */
	private void readDat(TreeMap<Integer, ResDto> res) {
		BBS bbsObject = BBSManager.getBBSFromDirectoryName(bbs);
		try {
			readDat(res, new BufferedReader(new FileReader(App.logFolder.resolve(bbs).resolve(board).resolve(datFileName).toString(), bbsObject.getCharset())));
		} catch (Exception e) {
			// 読めない場合は再取得
		}
	}

	/**
	 * readcgi.js処理済みの文字列からレスを取得してListに追加する。
	 * @param res
	 * @param source
	 */
	private void readDat(TreeMap<Integer, ResDto> res, String source) throws IOException {
		readDat(res, new BufferedReader(new StringReader(source)));
	}

	protected abstract void readDat(TreeMap<Integer, ResDto> res, BufferedReader br) throws IOException;

	protected abstract ThreadResponseDto request(TreeMap<Integer, ResDto> res) throws IOException;

	/**
	 * 新着レスをDATファイルに書き込む。
	 * threadst.txtを更新する。
	 * @param res
	 * @param startIndex
	 * @param threadResponseDto
	 */
	private void writeDat(TreeMap<Integer, ResDto> res, int lastRes, ThreadResponseDto threadResponseDto) throws IOException {
		File file = App.logFolder.resolve(bbs).resolve(board).resolve(datFileName).toFile();
		BufferedWriter bw = null;
		try {
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
		threadDto.settLast(DateTimeUtil.httpLongToLocalDateTime(last.getValue().getTimeLong()));
		threadDto.setnLogSize(file.length());
		threadDto.setDate(DateTimeUtil.httpLongToLocalDateTime(threadResponseDto.getDate()));
		threadDto.setTitle(first.getValue().getTitle());
		BoardManager.updateThreadst(boardDto);
	}
}
