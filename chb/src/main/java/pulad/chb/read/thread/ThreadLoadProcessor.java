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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.config.Config;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.dto.ThreadResponseDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.ResProcessor;
import pulad.chb.interfaces.ThreadLoader;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.NumberUtil;

/**
 * スレッドを読み込む処理を段階毎に実装。
 * @author pulad
 *
 */
public class ThreadLoadProcessor {
	private ThreadLoader threadLoader;
	private List<ResProcessor> resProcessors;
	protected final String urlStr;
	protected final boolean replaceEmoji;
	protected final Set<Integer> resFilter;
	protected final BBS bbsObject;
	protected final String bbs;
	protected final String board;
	protected final String datFileName;

	public ThreadLoadProcessor(ThreadLoader threadLoader, String url, boolean replaceEmoji, Collection<Integer> resFilter) {
		this.threadLoader = threadLoader;
		this.urlStr = url;
		this.replaceEmoji = replaceEmoji;
		this.resFilter = (resFilter == null) ? null : new TreeSet<Integer>(resFilter);

		this.resProcessors = createResProcessors();

		this.bbsObject = BBSManager.getBBSFromUrl(urlStr);
		this.bbs = bbsObject.getLogDirectoryName();
		this.board = bbsObject.getBoardFromThreadUrl(urlStr);
		this.datFileName = bbsObject.getDatFileNameFromThreadUrl(urlStr);
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

	/**
	 * DATファイルからレスを取得してListに追加する。
	 * @param setting
	 * @param res
	 */
	public void readDat(ConcurrentHashMap<String, String> setting, TreeMap<Integer, ResDto> res) {
		BBS bbsObject = BBSManager.getBBSFromLogDirectoryName(bbs);
		try {
			threadLoader.readDat(setting, res, new BufferedReader(new FileReader(Config.getLogFolder().resolve(bbs).resolve(board).resolve(datFileName).toString(), bbsObject.getCharset())));
		} catch (Exception e) {
			// 読めない場合は再取得
		}
	}

	/**
	 * readcgi.js処理済みの文字列からレスを取得してListに追加する。
	 * @param setting
	 * @param res
	 * @param source
	 */
	public void readDat(ConcurrentHashMap<String, String> setting, TreeMap<Integer, ResDto> res, String source) throws IOException {
		threadLoader.readDat(setting, res, new BufferedReader(new StringReader(source)));
	}

	/**
	 * HTTP要求を実行し、ファイルに書き込む。
	 * @param setting
	 * @param res
	 * @param now
	 * @return
	 * @throws IOException
	 */
	public ThreadResponseDto request(ConcurrentHashMap<String, String> setting, TreeMap<Integer, ResDto> res, long now) throws IOException {
		ThreadResponseDto threadResponseDto = threadLoader.request(res, now);
		if (threadResponseDto.getData() != null) {
			int lastRes = res.isEmpty() ? 0 : res.lastKey();

			readDat(setting, res, threadResponseDto.getData());
			writeDat(res, lastRes, threadResponseDto);
		}
		return threadResponseDto;
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

	/**
	 * レスを加工する。
	 * @param res
	 * @param now
	 * @param errorDetails
	 */
	public void applyResProcessor(TreeMap<Integer, ResDto> res, long now, List<String> errorDetails) {
		for (ResProcessor processor : resProcessors) {
			try {
				processor.process(urlStr, res, now);
			} catch (Exception e) {
				// ログファイルだけではなくページにも表示する
				errorDetails.add(e.toString());
			}
		}
	}

	/**
	 * フィルターのレス番のみを抽出したMapを返す。
	 * フィルターがnullの場合はresをそのまま返す。
	 * フィルターがnullでない場合、resを空にする。
	 * @param res
	 * @return
	 */
	public TreeMap<Integer, ResDto> applyFilter(TreeMap<Integer, ResDto> res) {
		if (resFilter == null) {
			return res;
		}
		TreeMap<Integer, ResDto> filtered = res.entrySet().stream()
				.filter(x -> resFilter.contains(x.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, TreeMap::new));
		res.clear();
		return filtered;
	}
}
