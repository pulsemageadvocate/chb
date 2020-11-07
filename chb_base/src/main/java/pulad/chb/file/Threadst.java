package pulad.chb.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.thymeleaf.util.ObjectUtils;

import pulad.chb.config.Config;
import pulad.chb.dto.ThreadDto;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.FileUtil;
import pulad.chb.util.NumberUtil;

/**
 * threadst.txtの読み書きを行う。
 * このクラスはスレッドセーフ。
 * @author pulad
 *
 */
public class Threadst {

	private Threadst() {}

	public static ConcurrentHashMap<String, ThreadDto> read(String bbs, String board) throws IOException {
		return read(FileUtil.realCapitalPath(Config.getLogFolder().resolve(bbs).resolve(board)));
	}

	public static ConcurrentHashMap<String, ThreadDto> read(Path dir) throws IOException {
		Path threadstFilePath = dir.resolve("threadst.txt");

		ConcurrentHashMap<String, ThreadDto> thread = new ConcurrentHashMap<>(1024, 0.75f, 1);
		if (Files.exists(threadstFilePath)) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(threadstFilePath.toString(), Charset.forName("UTF-8")));
				// ヘッダを捨てる
				String str = br.readLine();
				while ((str = br.readLine()) != null) {
					String[] token = str.split(",", 17);
					if (token.length == 17) {
						ThreadDto threadDto = new ThreadDto();
						threadDto.setBoardUrl(token[0]);
						threadDto.setDatName(token[1]);
						threadDto.setNumber(NumberUtil.parseInt(token[2], 0));
						threadDto.setState(NumberUtil.parseInt(token[3], 0));
						threadDto.setLogCount(NumberUtil.parseInt(token[4], 0));
						threadDto.setiNewRes(NumberUtil.parseInt(token[5], 0));
						threadDto.settLastGet(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[6], 0L)));
						threadDto.settLastWrite(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[7], 0L)));
						threadDto.setResCount(NumberUtil.parseInt(token[8], 0));
						threadDto.setnLastNRes(NumberUtil.parseInt(token[9], 0));
						threadDto.setBuildTime(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[10], 0L)));
						threadDto.settLast(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[11], 0L)));
						threadDto.setnLogSize(NumberUtil.parseLong(token[12], 0L));
						threadDto.setDate(DateTimeUtil.httpLongToLocalDateTime(NumberUtil.parseLong(token[13], 0L)));
						threadDto.setLabel(token[14]);
						threadDto.setTitle(token[15]);
						threadDto.setTitleAlias(token[16]);
						thread.put(token[1], threadDto);
					}
				}
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
					}
					br = null;
				}
			}
		}
		return thread;
	}

	public static void write(ConcurrentHashMap<String, ThreadDto> thread, String bbs, String board) throws IOException {
		Path threadstFilePath = FileUtil.realCapitalPath(Config.getLogFolder().resolve(bbs).resolve(board).resolve("threadst.txt"));

		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(threadstFilePath.toString(), Charset.forName("UTF-8"), false));
			bw.write("BOARDURL,DATNAME,NUMBER,STATE,NRESGET,INEWRES,TLASTGET,TLASTWRITE,NRES,NLASTNRES,TFIRST,TLAST,NLOGSIZE,DATE,LABEL,TITLE,TITLEALIAS\r\n");
			for (ThreadDto threadDto : thread.values()) {
				bw.write(threadDto.getBoardUrl());
				bw.write(',');
				bw.write(threadDto.getDatName());
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getNumber(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getState(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getLogCount(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getiNewRes(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.gettLastGet()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.gettLastWrite()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getResCount(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getnLastNRes(), 0));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.getBuildTime()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.gettLast()), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(threadDto.getnLogSize(), 0L));
				bw.write(',');
				bw.write(NumberUtil.toStringDefaultEmpty(DateTimeUtil.localDateTimeToHttpLong(threadDto.getDate()), 0L));
				bw.write(',');
				bw.write(ObjectUtils.nullSafe(threadDto.getLabel(), ""));
				bw.write(',');
				bw.write(ObjectUtils.nullSafe(threadDto.getTitle(), ""));
				bw.write(',');
				bw.write(ObjectUtils.nullSafe(threadDto.getTitleAlias(), ""));
				bw.write("\r\n");
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
	}
}
