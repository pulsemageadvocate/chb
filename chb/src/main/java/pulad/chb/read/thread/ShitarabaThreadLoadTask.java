package pulad.chb.read.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import pulad.chb.DownloadProcessor;
import pulad.chb.board.BoardManager;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.DownloadDto;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadResponseDto;
import pulad.chb.util.NumberUtil;

public class ShitarabaThreadLoadTask extends AbstractThreadLoadTask {

	public ShitarabaThreadLoadTask(String url) {
		super(url);
	}

	public ShitarabaThreadLoadTask(String url, boolean remote) {
		super(url, remote);
	}

	public ShitarabaThreadLoadTask(String url, boolean remote, Collection<Integer> resFilter) {
		super(url, remote, resFilter);
	}

	@Override
	protected List<ResProcessor> createResProcessors() {
		List<ResProcessor> resProcessors = new LinkedList<>();
		resProcessors.add(new ReplaceStrResProcessor());
		resProcessors.add(new RemoveAnchorResProcessor());
		resProcessors.add(new AnchorLinkResProcessor());
		resProcessors.add(new ImageResProcessor());
		resProcessors.add(new CountResProcessor());
		resProcessors.add(new LinkPopupResProcessor());
		resProcessors.add(new AboneResProcessor());
		return resProcessors;
	}

	@Override
	protected void readDat(TreeMap<Integer, ResDto> res, BufferedReader br) throws IOException {
		BoardDto boardDto = BoardManager.get(bbsObject.getBoardUrlFromThreadUrl(urlStr), remote);
		String noNameName = boardDto.getSetting().getOrDefault("BBS_NONAME_NAME", "");

		try {
			StringBuilder sb = new StringBuilder(4096);
			String str = null;
			while ((str = br.readLine()) != null) {
				sb.append(str);
				str = sb.toString();
				String[] token = str.split("\\<\\>", 7);
				// 本文に改行が入るので対策する
				if (token.length < 7) {
					sb.append("\n");
					continue;
				}

				ResDto dto = new ResDto();
				dto.setSource(str);
				int number = Integer.parseInt(token[0]);
				dto.setNumber(number);
				dto.setName(token[1]);
				dto.setMail(token[2]);
				dto.setTimeIdAux(token[3]);
				dto.setTime(token[3]);
				dto.setBody(token[4]);
				dto.setTitle(token[5]);
				dto.setId(token[6]);
				dto.setAnonymous(noNameName.equals(token[1]));
				res.put(NumberUtil.integerCache(number), dto);

				sb.setLength(0);
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

	@Override
	protected ThreadResponseDto request(TreeMap<Integer, ResDto> res) throws IOException {
		String urlLast = urlStr.replace("read.cgi", "rawmode.cgi");
		if (res.size() > 0) {
			urlLast = urlLast + res.size() + '-';
		} else {
			//urlLast = urlLast;
		}
		DownloadDto downloadDto = DownloadProcessor.downloadBytes(urlLast, 1048576, 120000);

		ThreadResponseDto dto = new ThreadResponseDto();
		dto.setUrl(downloadDto.getUrl());
		dto.setCheckTime(downloadDto.getCheckTime());
		dto.setAccessTime(downloadDto.getAccessTime());
		dto.setResponseCode(downloadDto.getResponseCode());
		dto.setResponseMessage(downloadDto.getResponseMessage());
		dto.setContentType(downloadDto.getContentType());
		dto.setContentLength(downloadDto.getContentLength());
		dto.setExpiration(downloadDto.getExpiration());
		dto.setDate(downloadDto.getDate());
		dto.setLastModified(downloadDto.getLastModified());
		dto.setLocation(downloadDto.getLocation());
		dto.setHeader(downloadDto.getHeader());
		dto.setData((downloadDto.getData() == null) ? null : new String(downloadDto.getData(), bbsObject.getCharset()));
		return dto;
	}
}
