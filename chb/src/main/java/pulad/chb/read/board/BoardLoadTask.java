package pulad.chb.read.board;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unbescape.html.HtmlEscape;

import javafx.concurrent.Task;
import pulad.chb.App;
import pulad.chb.bbs.BBSManager;
import pulad.chb.board.BoardManager;
import pulad.chb.config.Config;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.BoardLoadTaskResponseDto;
import pulad.chb.dto.DownloadDto;
import pulad.chb.dto.ThreadDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.DownloadProcessor;
import pulad.chb.util.FileUtil;

public class BoardLoadTask extends Task<BoardLoadTaskResponseDto> {
	// immutable
	private final Pattern regSubject = Pattern.compile("^(?<dat>(?<thread>[0-9]*).*?)(\\<\\>|,)(?<title>.*)\\((?<res>[0-9]+)\\)$", Pattern.CASE_INSENSITIVE);
	private List<ThreadProcessor> threadProcessors;
	protected final String urlStr;
	protected final boolean remote;
	protected final boolean replaceEmoji;
	protected final boolean pastLog;

	public BoardLoadTask(String url) {
		this(url, true, true, false);
	}

	public BoardLoadTask(String url, boolean remote) {
		this(url, remote, true, false);
	}

	public BoardLoadTask(String url, boolean remote, boolean replaceEmoji) {
		this(url, remote, replaceEmoji, false);
	}

	public BoardLoadTask(String url, boolean remote, boolean replaceEmoji, boolean pastLog) {
		this.urlStr = url;
		this.remote = remote;
		this.replaceEmoji = replaceEmoji;
		this.pastLog = pastLog;
		threadProcessors = new LinkedList<>();
		threadProcessors.add(new IrregalBuildTimeThreadProcessor());
	}

	@Override
	protected BoardLoadTaskResponseDto call() throws Exception {
		BoardLoadTaskResponseDto boardLoadTaskResponseDto = new BoardLoadTaskResponseDto();

		BBS bbsObject = BBSManager.getBBSFromUrl(urlStr);
		String bbs = bbsObject.getLogDirectoryName();
		String board = bbsObject.getBoardFromBoardUrl(urlStr);
		Path subjectFilePath = FileUtil.realCapitalPath(Config.getLogFolder().resolve(bbs).resolve(board).resolve("subject.txt"));

		// subject.txtダウンロード
		if (this.remote && !this.pastLog) {
			try {
				DownloadDto downloadDto = DownloadProcessor.download(urlStr + "subject.txt", subjectFilePath, 1048576, 120000);
				if (downloadDto.getResponseCode() <= 0) {
					boardLoadTaskResponseDto.setErrorMessage(downloadDto.getResponseMessage());
				} else if (downloadDto.getResponseCode() != 200) {
					boardLoadTaskResponseDto.setErrorMessage("HTTP " + downloadDto.getResponseCode() + " " + downloadDto.getResponseMessage());
				}
			} catch (IOException e) {
			}
		}

		// subject.txt/threadst.txt読み込み
		BoardDto boardDto = BoardManager.get(urlStr);
		ConcurrentHashMap<String, ThreadDto> logThread = boardDto.getLogThread();
		ArrayList<ThreadDto> thread = new ArrayList<>(1024);
		boolean updateThreadst = false;
		// 過去ログ
		if (this.pastLog) {
			int number = 1;
			for (ThreadDto log : logThread.values()) {
				ThreadDto dto = new ThreadDto();
				dto.setBoardUrl(urlStr);
				dto.setDatName(log.getDatName());
				dto.setNumber(number++);
				dto.setResCount(log.getResCount());
				dto.setBuildTime(log.getBuildTime());
				dto.setTitle(log.getTitle());
				dto.setLogCount(log.getLogCount());
				dto.setResCount(log.getResCount());
				dto.settLast(log.gettLast());
				thread.add(dto);
			}
		} else {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(subjectFilePath.toString(), bbsObject.getCharset()));
				String str = null;
				int number = 0;
				while ((str = br.readLine()) != null) {
					// 中断確認
					if (isCancelled()) {
						break;
					}
					number++;
					Matcher matcher = regSubject.matcher(str);
					if (matcher.find()) {
						String dat = matcher.group("dat");
						ThreadDto dto = new ThreadDto();
						dto.setBoardUrl(urlStr);
						dto.setDatName(dat);
						dto.setNumber(number);
						dto.setResCount(Integer.parseInt(matcher.group("res")));
						dto.setBuildTime(DateTimeUtil.httpLongToLocalDateTime(Long.parseLong(matcher.group("thread")) * 1000L));
						// そのままだと数値文字参照が表示されるので解除
						String title = matcher.group("title");
						if (!replaceEmoji) {
							title = HtmlEscape.escapeHtml5Xml(HtmlEscape.unescapeHtml(title));
						}
						dto.setTitle(title);

						ThreadDto log = logThread.get(dat);
						if (log != null) {
							dto.setLogCount(log.getLogCount());
							log.setResCount(dto.getResCount());
							dto.settLast(log.gettLast());
							updateThreadst = true;
						}

						thread.add(dto);
					}
				}
			} catch (Exception e) {
				boardLoadTaskResponseDto.setErrorMessage(e.getClass().getName() + ": " + e.getMessage());
				ThreadDto dto = new ThreadDto();
				dto.setTitle(e.toString());
				thread.add(dto);
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						App.logger.error("BoardLoadTask失敗", e);
					}
					br = null;
				}
			}
		}

		if (updateThreadst) {
			BoardManager.updateThreadst(boardDto);
		}

		// 加工
		for (ThreadProcessor processor : threadProcessors) {
			processor.process(thread);
		}
		boardDto.setThread(thread);

		boardLoadTaskResponseDto.setDto(boardDto);
		return boardLoadTaskResponseDto;
	}
}
