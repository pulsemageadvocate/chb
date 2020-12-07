package pulad.chb.read.thread;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.thymeleaf.util.ObjectUtils;
import org.thymeleaf.util.StringUtils;

import pulad.chb.App;
import pulad.chb.config.Config;
import pulad.chb.dto.DownloadDto;
import pulad.chb.util.ImageUtil;

public class LinkHistManager {
	private static ConcurrentHashMap<String, LinkHistDto> linkhist = null;

	/**
	 * ローカルキャッシュのファイル名を取得する。キャッシュが無い場合はnull。
	 * @param url
	 * @return ファイル名。キャッシュが無い場合はnull。
	 */
	public static String getCacheFileName(String url) {
		// URL,CHECKTIME,ACCESSTIME,RESPONSECODE,CONTENTTYPE,CONTENTLENGTH,EXPIRATION,DATE,LASTMODIFIED,LOCATION,IMAGECACHE
		LinkHistDto dto = get(url);
		if (dto == null || dto.lock != null) {
			return null;
		}

		// キャッシュ無し（失敗履歴有り）
		if (dto.record[10].length() <= 0) {
			return "classpath:image/notfound.gif";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(Config.getImageFolder().resolve(dto.record[10].substring(0, 1)).resolve(dto.record[10]).toString());
		sb.setLength(sb.length() - 1);
		sb.append(ImageUtil.getFileExt(dto.record[4]));
		return sb.toString();
	}

	/**
	 * linkhist.txtのエントリを取得する。
	 * @param url
	 * @return
	 */
	private static LinkHistDto get(String url) {
		if (url == null || !url.startsWith("http")) {
			return null;
		}

		if (linkhist == null) {
			synchronized (LinkHistManager.class) {
				if (linkhist == null) {
					readLinkHistFile();
				}
			}
		}
		return linkhist.get(url);
	}

	private static void readLinkHistFile() {
		ConcurrentHashMap<String, LinkHistDto> linkhist0;
		BufferedReader br = null;
		try {
			File linkhistfile = Config.getLinkhistFile().toFile();
			long fileSize = linkhistfile.length();
			linkhist0 = new ConcurrentHashMap<>((int)(fileSize / 150L));

			br = new BufferedReader(new FileReader(linkhistfile, Charset.forName("Shift-jis")));
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] token = str.split(",", 11);
				if (token.length != 11) {
					continue;
				}
				LinkHistDto dto = new LinkHistDto();
				dto.record = token;
				linkhist0.put(token[0], dto);
			}
		} catch (IOException e) {
			App.logger.error("readLinkHistFile失敗", e);
			linkhist0 = new ConcurrentHashMap<>();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					App.logger.error("readLinkHistFile close失敗", e);
				}
				br = null;
			}
		}
		linkhist = linkhist0;
	}

	/**
	 * 指定したurlに対するロックを取得する。
	 * @param url
	 * @return ロックが成功した場合はtrue、他でロックされているかダウンロード済みの場合はロック解放後にfalse。
	 * @throws InterruptedException ロック待ちが中断された場合
	 */
	public static boolean lock(String url) throws InterruptedException {
		if (url == null || !url.startsWith("http")) {
			return false;
		}

		LinkHistDto dto = new LinkHistDto();
		dto.record = new String[11];
		dto.record[0] = url;
		dto.lock = new CountDownLatch(1);
		synchronized (LinkHistManager.class) {
			LinkHistDto old = linkhist.putIfAbsent(url, dto);
			if (old != null) {
				if (old.lock != null) {
					old.lock.await();
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * ファイルを追加する。ロックを開放する。
	 * @param downloadDto
	 */
	public static void addFile(DownloadDto downloadDto) {
		String url = downloadDto.getUrl();
		if (StringUtils.isEmpty(url)) {
			return;
		}

		LinkHistDto dto = get(url);
		if (dto == null || dto.lock == null) {
			return;
		}
		CountDownLatch lock = dto.lock;
		dto.record[1] = Long.toString(downloadDto.getCheckTime());
		dto.record[2] = Long.toString(downloadDto.getAccessTime());
		dto.record[3] = Integer.toString(downloadDto.getResponseCode());
		dto.record[4] = ObjectUtils.nullSafe(downloadDto.getContentType(), "");
		dto.record[5] = Long.toString(downloadDto.getContentLength());
		dto.record[6] = Long.toString(downloadDto.getExpiration());
		dto.record[7] = Long.toString(downloadDto.getDate());
		dto.record[8] = Long.toString(downloadDto.getLastModified());
		dto.record[9] = ObjectUtils.nullSafe(downloadDto.getLocation(), "");
		dto.record[10] = ObjectUtils.nullSafe(downloadDto.getImageCache(), "") + ObjectUtils.nullSafe(downloadDto.getImageExt(), "");
		dto.lock = null;
		lock.countDown();

		synchronized (LinkHistManager.class) {
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(
						Config.getLinkhistFile().toFile(),
						Charset.forName("Shift-jis"),
						true));
				bw.write(String.join(",", dto.record));
				bw.write("\r\n");
				bw.flush();
			} catch (IOException e) {
				App.logger.error("addFile失敗", e);
			} finally {
				if (bw != null) {
					try {
						bw.close();
					} catch (IOException e) {
						App.logger.error("addFile close失敗", e);
					}
					bw = null;
				}
			}
		}
	}

	/**
	 * 指定したurlに対するロックがあれば、解放してエントリを削除する。異常時用。
	 * @param url
	 */
	public static void release(String url) {
		LinkHistDto dto = get(url);
		if (dto == null || dto.lock == null) {
			return;
		}
		linkhist.remove(url);
		dto.lock.countDown();
	}

	/**
	 * エントリを削除する。再読み込み用。
	 * @param url
	 */
	public static void delete(String url) {
		if (url == null || !url.startsWith("http")) {
			return;
		}

		if (linkhist == null) {
			synchronized (LinkHistManager.class) {
				if (linkhist == null) {
					readLinkHistFile();
				}
			}
		}

		LinkHistDto dto = get(url);
		if (dto == null || dto.lock != null) {
			return;
		}
		linkhist.remove(url);

		if (dto.record[10].length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(Config.getImageFolder().resolve(dto.record[10].substring(0, 1)).resolve(dto.record[10]).toString());
			sb.setLength(sb.length() - 1);
			sb.append(ImageUtil.getFileExt(dto.record[4]));
			try {
				Files.delete(Paths.get(sb.toString()));
			} catch (IOException e) {
			}
		}
	}

	private static class LinkHistDto {
		private volatile CountDownLatch lock = null;
		private volatile String[] record = null;
	}
}
