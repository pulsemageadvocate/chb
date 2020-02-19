package pulad.chb;

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

import pulad.chb.dto.DownloadDto;
import pulad.chb.util.DateTimeUtil;

public class DownloadProcessor {

	public static boolean download(String urlStr, Path filePath) {
		return download(urlStr, filePath, 1048576);
	}

	public static boolean download(String urlStr, Path filePath, int maxLength) {
		DownloadDto dto = downloadBytes(urlStr, maxLength);
		if (dto == null) {
			return false;
		}
		byte[] data = dto.getData();
		if (data == null) {
			return false;
		}
		try {
			Files.write(filePath, data, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			App.logger.error("download失敗", e);
			return false;
		}
		return true;
	}

	/**
	 * ファイルをダウンロードする。
	 * @param urlStr URL
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr) {
		return downloadBytes(urlStr, 1048576, null);
	}

	/**
	 * ファイルをダウンロードする。
	 * @param urlStr URL
	 * @param maxLength ファイルサイズが指定バイトを超える場合はダウンロードしない。
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr, int maxLength) {
		return downloadBytes(urlStr, maxLength, null);
	}

	/**
	 * ファイルをダウンロードする。
	 * @param urlStr URL
	 * @param maxLength ファイルサイズが指定バイトを超える場合はダウンロードしない。
	 * @param filter falseを返した場合はダウンロードしない。
	 * @return DownloadDto
	 */
	public static DownloadDto downloadBytes(String urlStr, int maxLength, Predicate<HttpURLConnection> filter) {
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
			connection.setReadTimeout(30000);
			connection.setInstanceFollowRedirects(true);
			connection.connect();

			dto.setResponseCode(connection.getResponseCode());
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
			// コンソールに出てくるとうざいだけなので出さない
		} catch (IOException e) {
			if (connection == null) {
				dto.setResponseCode(-1);
			} else {
				try {
					dto.setResponseCode(connection.getResponseCode());
				} catch (IOException e1) {
					dto.setResponseCode(-1);
				}
			}
			App.logger.error("downloadBytes失敗", e);
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
