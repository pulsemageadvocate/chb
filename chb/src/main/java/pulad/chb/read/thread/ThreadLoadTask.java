package pulad.chb.read.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import pulad.chb.App;
import pulad.chb.dto.BoardDto;
import pulad.chb.dto.ResDto;
import pulad.chb.dto.ThreadResponseDto;
import pulad.chb.util.DateTimeUtil;
import pulad.chb.util.FileUtil;
import pulad.chb.util.NumberUtil;

public class ThreadLoadTask extends AbstractThreadLoadTask {
	private final Pattern regWacchoi = Pattern.compile("^(?<name>.*)</b>\\((?<wacchoi>[^\\-\\[\\]\\)]+(?<wacchoiLower>\\-[^ \\[\\]\\)]{4})[^\\-\\[\\]\\)]*)( \\[(?<ip>[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+)?(?<ipTrailing>.*)\\])?\\)<b>", Pattern.CASE_INSENSITIVE);
	private final Pattern regTime = Pattern.compile("^(?<time>[0-9]{4}/[0-9]{2}/[0-9]{2}\\(.\\) [0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]+)?)( ID:(?<id>[0-9A-Za-z/\\+=]+))?( (?<trailing>[0-9A-Za-z/\\+=]))?", Pattern.CASE_INSENSITIVE);

	public ThreadLoadTask(String url) {
		super(url);
	}

	public ThreadLoadTask(String url, boolean remote) {
		super(url, remote);
	}

	public ThreadLoadTask(String url, boolean remote, Collection<Integer> resFilter) {
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
	protected void readDat(BoardDto boardDto, TreeMap<Integer, ResDto> res, BufferedReader br) throws IOException {
		String noNameName = boardDto.getSetting().getOrDefault("BBS_NONAME_NAME", "");

		try {
			boolean hasId = false;
			boolean hasWacchoi = false;
			boolean hasIp = false;
			int number = res.size() + 1;
			String str = null;
			while ((str = br.readLine()) != null) {
				if (str.length() == 0) {
					continue;
				}
				String[] token = str.split("<>", 5);
				ResDto dto = new ResDto();
				dto.setSource(str);
				dto.setNumber(number);
				switch (token.length) {
				case 0:
					continue;
				case 1:
					dto.setName(token[0]);
					dto.setMail("ここ壊れてます");
					dto.setTimeIdAux("ここ壊れてます");
					dto.setBody("ここ壊れてます");
					dto.setTitle("ここ壊れてます");
					break;
				case 2:
					dto.setName(token[0]);
					dto.setMail(token[1]);
					dto.setTimeIdAux("ここ壊れてます");
					dto.setBody("ここ壊れてます");
					dto.setTitle("ここ壊れてます");
					break;
				case 3:
					dto.setName(token[0]);
					dto.setMail(token[1]);
					dto.setTimeIdAux(token[2]);
					dto.setBody("ここ壊れてます");
					dto.setTitle("ここ壊れてます");
					break;
				case 4:
					dto.setName(token[0]);
					dto.setMail(token[1]);
					dto.setTimeIdAux(token[2]);
					dto.setBody(token[3]);
					dto.setTitle("ここ壊れてます");
					break;
				default:
					dto.setName(token[0]);
					dto.setMail(token[1]);
					dto.setTimeIdAux(token[2]);
					dto.setBody(token[3]);
					dto.setTitle(token[4]);
					break;
				}
				res.put(NumberUtil.integerCache(number++), dto);

				// 名無し、ﾜｯﾁｮｲ、ip、上級国民
				Matcher matcher = regWacchoi.matcher(dto.getName());
				if (matcher.find()) {
					String processedName = matcher.group("name");
					if (processedName != null && !processedName.isEmpty()) {
						dto.setName(processedName);
						dto.setAnonymous(processedName.trim().startsWith(noNameName));
					}
					String wacchoi = matcher.group("wacchoi");
					if (wacchoi != null && !wacchoi.isEmpty()) {
						hasWacchoi = true;
						dto.setWacchoi(wacchoi);
					}
					String wacchoiLower = matcher.group("wacchoiLower");
					if (wacchoiLower != null && !wacchoiLower.isEmpty()) {
						dto.setWacchoiLower(wacchoiLower);
					}
					String ip = matcher.group("ip");
					if (ip != null && !ip.isEmpty()) {
						hasIp = true;
						dto.setIp(ip);
					}
					String ipTrailing = matcher.group("ipTrailing");
					if (ipTrailing != null && !ipTrailing.isEmpty()) {
						dto.setIpTrailing(ipTrailing);
					}
				} else {
					dto.setAnonymous(dto.getName().trim().startsWith(noNameName));
				}

				// 時刻、ID、末尾
				matcher = regTime.matcher(dto.getTimeIdAux());
				if (matcher.find()) {
					String time = matcher.group("time");
					if (time != null && !time.isEmpty()) {
						dto.setTime(time);
						try {
							LocalDateTime date = DateTimeUtil.parseResTime(time);
							dto.setTimeLong(DateTimeUtil.localDateTimeToHttpLong(date));
						} catch (DateTimeParseException e) {
						}
					}
					String id = matcher.group("id");
					if (id != null && !id.isEmpty()) {
						hasId = true;
						dto.setId(id);
						dto.setTrailing(id.substring(id.length() - 1));
					} else {
						String trailing = matcher.group("trailing");
						if (trailing != null && !trailing.isEmpty()) {
							dto.setTrailing(trailing);
						}
					}
				} else {
					dto.setTime(dto.getTimeIdAux());
				}
			}

			// ﾜｯﾁｮｲ消しなど
			for (ResDto dto : res.values()) {
				if (hasIp && (dto.getIp() == null)) {
					dto.setIp("");
				}
				if (hasWacchoi) {
					if (dto.getWacchoi() == null) {
						dto.setWacchoi("");
					}
					if (dto.getWacchoiLower() == null) {
						dto.setWacchoiLower("");
					}
				}
				if (hasId && (dto.getId() == null)) {
					dto.setId("");
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

	@Override
	protected ThreadResponseDto request(TreeMap<Integer, ResDto> res) throws IOException {
		ThreadResponseDto dto = new ThreadResponseDto();
		dto.setUrl(urlStr);
		dto.setCheckTime(now);
		dto.setAccessTime(now);

		HttpURLConnection connection = null;
		InputStream is = null;
		byte[] data = null;
		int actualLength = 0;
		try {
			String urlLast = null;
			if (res.size() > 0) {
				urlLast = urlStr + res.size() + '-';
			} else {
				urlLast = urlStr;
			}
			URL url = new URL(urlLast);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setReadTimeout(30000);
			connection.connect();
			dto.setResponseCode(connection.getResponseCode());
			dto.setResponseMessage(connection.getResponseMessage());
			String contentType = connection.getContentType();
			dto.setContentType(contentType);
			long lengthLong = connection.getContentLengthLong();
			dto.setContentLength(lengthLong);
			dto.setExpiration(connection.getExpiration());
			dto.setDate(connection.getDate());
			dto.setLastModified(connection.getLastModified());
			dto.setLocation(connection.getHeaderField("Location"));
			dto.setHeader(new HashMap<String, List<String>>(connection.getHeaderFields()));
			
			if (lengthLong > 1048576L) {
				throw new IOException("ダウンロードサイズ制限: " + lengthLong + "bytes " + url);
			}
			int length = (lengthLong < 0L) ? 1048576 : ((int) lengthLong);
			is = connection.getInputStream();
			data = new byte[(int) length];
			int readLength = 0;
			while ((length > actualLength) && ((readLength = is.readNBytes(data, actualLength, length - actualLength)) > 0)) {
				actualLength += readLength;
			}
		} catch (IOException e) {
			if (connection == null) {
				dto.setResponseCode(-1);
				dto.setResponseMessage(e.getClass().getName() + ": " + e.getMessage());
			} else {
				try {
					dto.setResponseCode(connection.getResponseCode());
					dto.setResponseMessage("HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
				} catch (IOException e1) {
					dto.setResponseCode(-1);
					dto.setResponseMessage(e.getClass().getName() + ": " + e.getMessage());
				}
			}
			App.logger.error("request失敗", e);
			return dto;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
				is = null;
			}
			if (connection != null) {
				connection.disconnect();
				connection = null;
			}
		}
		if (actualLength <= 0) {
			return dto;
		}
		String readHtml = new String(data, bbsObject.getCharset());

		// readcgi.js
		String readcgijs = Files.readString(FileUtil.realCapitalPath(App.scriptFolder.resolve("readcgi.js")), Charset.forName("UTF-8"));

		try {
			org.mozilla.javascript.Context cx = org.mozilla.javascript.Context.enter();
			Scriptable scope = cx.initStandardObjects();
			cx.evaluateString(scope, readcgijs, "readcgi.js", 1, null);
			Object htmlToDat = scope.get("htmlToDat", scope);
			if (!(htmlToDat instanceof Function)) {
				throw new IOException("readcgi.jsが異常");
			}
			Object result = ((Function) htmlToDat).call(cx, scope, scope, new Object[]{urlStr, readHtml, res.size()});
			dto.setData((result == null) ? null : Context.toString(result));
			return dto;
		} finally {
			Context.exit();
		}
	}
}
