package pulad.chb.read.thread;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.App;
import pulad.chb.config.Config;
import pulad.chb.dto.DownloadDto;
import pulad.chb.dto.ImageDto;
import pulad.chb.util.DownloadProcessor;
import pulad.chb.util.ImageUtil;

/**
 * imgcache://、imgcaches://、imglocal://、imglocals://、image://で始まるURLを処理する。
 * @author pulad
 *
 */
public class LocalURLStreamHandler extends URLStreamHandler {
	private static final Pattern regImage = Pattern.compile("^image:[A-Za-z0-9._\\-]+$");

	// TODO:変更可能にする
	public static final String PROTOCOL = "imgcache";
	public static final String PROTOCOL_SECURE = PROTOCOL + "s";
	public static final String PROTOCOL_LOCAL = "imglocal";
	public static final String PROTOCOL_LOCAL_SECURE = PROTOCOL_LOCAL + "s";
	public static final String PROTOCOL_IMAGE = "image";

	public static class Factory implements URLStreamHandlerFactory {
		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			return (PROTOCOL.equals(protocol) ||
					PROTOCOL_SECURE.equals(protocol) ||
					PROTOCOL_LOCAL.equals(protocol) ||
					PROTOCOL_LOCAL_SECURE.equals(protocol) ||
					PROTOCOL_IMAGE.equals(protocol)) ? new LocalURLStreamHandler() : null;
		}
	}

	/**
	 * imgcache:やimglocal:をhttp:に戻す。
	 * @param url
	 * @return
	 */
	public static String getSourceUrl(String url) {
		return url.replaceFirst(PROTOCOL, "http").replaceFirst(PROTOCOL_LOCAL, "http");
	}

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		String urlStr = u.toExternalForm();
		String source = getSourceUrl(urlStr);

		if (urlStr.startsWith(PROTOCOL_IMAGE)) {
			return new LocalURLConnection(u);
		}

		ImageDto cache = LinkHistManager.getCache(source);
		if (cache != null) {
			return new LocalHttpURLConnection(
					new URL(source),
					cache.getResponseCode(),
					cache.getContentType(),
					cache.getContentLength(),
					cache.getExpiration(),
					cache.getDate(),
					cache.getLastModified(),
					cache.getLocation(),
					cache.getFileName());
		}

		if (App.offline || urlStr.startsWith(PROTOCOL_LOCAL)) {
			return new LocalURLConnection(u);
		}

		try {
			if (LinkHistManager.lock(source)) {
				DownloadDto downloadDto = DownloadProcessor.downloadBytes(
						source,
						1048576 * 4,
						x -> (ImageUtil.getFileExt(x.getContentType()) != null));
				byte[] data = downloadDto.getData();
				String contentType = downloadDto.getContentType();
				if (data != null) {
					String imageExt = ImageUtil.getImageExt(contentType);
					String fileExt = ImageUtil.getFileExt(contentType);

					MessageDigest md = MessageDigest.getInstance("SHA-1");
					md.update(data, 0, data.length);
					byte[] digest = md.digest();
					String sha1FileName = String.format("%040x", new BigInteger(1, digest));

					downloadDto.setImageCache(sha1FileName);
					downloadDto.setImageExt(imageExt);
					sha1FileName = sha1FileName + fileExt;

					try {
						Files.write(Config.getImageFolder().resolve(sha1FileName.substring(0, 1)).resolve(sha1FileName), data, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
					} catch (FileAlreadyExistsException e) {
						// URLが別でも内容のハッシュが同じならありうる
					} catch (IOException e) {
						App.logger.error("download失敗", e);
						return new LocalURLConnection(u);
					}
				}

				if (downloadDto.getResponseCode() != -1) {
					LinkHistManager.addFile(downloadDto);
				}
			}

			cache = LinkHistManager.getCache(source);
			if (cache != null) {
				return new LocalHttpURLConnection(
						new URL(source),
						cache.getResponseCode(),
						cache.getContentType(),
						cache.getContentLength(),
						cache.getExpiration(),
						cache.getDate(),
						cache.getLastModified(),
						cache.getLocation(),
						cache.getFileName());
			}

			return new LocalURLConnection(u);
			//return new URL(u.toExternalForm().replaceFirst(PROTOCOL, "http")).openConnection();
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e);
		} finally {
			LinkHistManager.release(source);
		}
	}

	/**
	 * ローカルのファイルに対するURLConnectionの実装。
	 * LocalHttpURLConnectionの実装により不要になるはず。
	 * @author pulad
	 *
	 */
	private static class LocalURLConnection extends URLConnection {
		private String url;

		public LocalURLConnection(URL u) {
			super(u);
			this.url = LocalURLStreamHandler.getSourceUrl(u.toExternalForm());
			App.logger.warn("new LocalURLConnection: {}", this.url);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (url.startsWith("image:")) {
				// パストラバーサル脆弱性を検証する
				Matcher matcher = regImage.matcher(url);
				if (!matcher.matches()) {
					throw new IOException("みつかりません");
				}
				return getClass().getClassLoader().getResourceAsStream("image/" + url.substring(6));
			}
			throw new IOException("みつかりません");
		}

		@Override
		public String getContentType() {
			// TODO Auto-generated method stub
			return super.getContentType();
		}
	}
}
