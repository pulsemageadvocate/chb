package pulad.chb.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pulad.chb.config.Config;
import pulad.chb.dto.DownloadDto;

public class DownloadProcessor {
	private static Logger logger = LoggerFactory.getLogger(DownloadProcessor.class);

	/**
	 * ファイルをダウンロードして保存する。ダウンロードの詳細を返す。
	 * @param urlStr
	 * @param filePath
	 * @return
	 * @throws IOException 書き込みに失敗した場合
	 */
	public static DownloadDto download(String urlStr, Path filePath) throws IOException {
		return download(urlStr, filePath, 1048576, 30000);
	}

	/**
	 * ファイルをダウンロードして保存する。ダウンロードの詳細を返す。
	 * @param urlStr
	 * @param filePath
	 * @param maxLength
	 * @return
	 * @throws IOException 書き込みに失敗した場合
	 */
	public static DownloadDto download(String urlStr, Path filePath, int maxLength) throws IOException {
		return download(urlStr, filePath, maxLength, 30000);
	}

	/**
	 * ファイルをダウンロードして保存する。ダウンロードの詳細を返す。
	 * @param urlStr
	 * @param filePath
	 * @param maxLength
	 * @param timeout タイムアウト（ミリ秒）
	 * @return
	 * @throws IOException 書き込みに失敗した場合
	 */
	public static DownloadDto download(String urlStr, Path filePath, int maxLength, int timeout) throws IOException {
		DownloadDto dto = downloadBytes(urlStr, maxLength, timeout);
		if (dto == null) {
			return null;
		}
		byte[] data = dto.getData();
		if (data == null) {
			return dto;
		}
		try {
			Files.write(filePath, data, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.error("download失敗", e);
			throw e;
		}
		return dto;
	}

	/**
	 * ファイルをダウンロードする。ダウンロードの詳細を返す。
	 * @param urlStr URL
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr) {
		return downloadBytes(urlStr, 1048576, 30000, null);
	}

	/**
	 * ファイルをダウンロードする。ダウンロードの詳細を返す。
	 * @param urlStr URL
	 * @param maxLength ファイルサイズが指定バイトを超える場合はダウンロードしない。
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr, int maxLength) {
		return downloadBytes(urlStr, maxLength, 30000, null);
	}

	/**
	 * ファイルをダウンロードする。ダウンロードの詳細を返す。
	 * @param urlStr URL
	 * @param maxLength ファイルサイズが指定バイトを超える場合はダウンロードしない。
	 * @param timeout タイムアウト（ミリ秒）
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr, int maxLength, int timeout) {
		return downloadBytes(urlStr, maxLength, timeout, null);
	}

	/**
	 * ファイルをダウンロードする。ダウンロードの詳細を返す。
	 * @param urlStr URL
	 * @param maxLength ファイルサイズが指定バイトを超える場合はダウンロードしない。
	 * @param filter falseを返した場合はダウンロードしない。
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr, int maxLength, Predicate<HttpURLConnection> filter) {
		return downloadBytes(urlStr, maxLength, 30000, filter);
	}

	/**
	 * ファイルをダウンロードする。ダウンロードの詳細を返す。
	 * @param urlStr URL
	 * @param maxLength ファイルサイズが指定バイトを超える場合はダウンロードしない。
	 * @param timeout タイムアウト（ミリ秒）
	 * @param filter falseを返した場合はダウンロードしない。
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr, int maxLength, int timeout, Predicate<HttpURLConnection> filter) {
		DownloadDto dto = new DownloadDto();
		dto.setUrl(urlStr);
		long now = DateTimeUtil.localDateTimeToHttpLong(LocalDateTime.now());
		dto.setCheckTime(now);
		dto.setAccessTime(now);

		HttpURLConnection connection = null;
		InputStream is = null;
		byte[] data = null;
		int actualLength = 0;
		try {
			URL url = new URL(urlStr);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", Config.ua);
			connection.setConnectTimeout(timeout);
			connection.setReadTimeout(timeout);
			connection.setInstanceFollowRedirects(true);
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

			if (filter != null) {
				if (!filter.test(connection)) {
					return dto;
				}
			}

			if (lengthLong < 0 || lengthLong > (long) maxLength) {
				//throw new IOException("ダウンロードサイズ制限: " + lengthLong + "bytes " + url);
				return dto;
			}
			int length = (int) lengthLong;
			is = connection.getInputStream();
			data = new byte[(int) length];
			int readLength = 0;
			while ((length > actualLength) && ((readLength = is.readNBytes(data, actualLength, length - actualLength)) > 0)) {
				actualLength += readLength;
			}
			dto.setData(data);
		} catch (UnknownHostException e) {
			dto.setResponseCode(410); // 410 Goneにして再度要求しないようにする
			dto.setResponseMessage(e.getClass().getName() + ": " + e.getMessage());
			// コンソールに出てくるとうざいだけなので出さない
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
			logger.error("downloadBytes失敗", e);
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
		return dto;
	}
}
