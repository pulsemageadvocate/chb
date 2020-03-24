package pulad.chb.read.thread;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import pulad.chb.App;
import pulad.chb.DownloadProcessor;
import pulad.chb.dto.DownloadDto;
import pulad.chb.util.ImageUtil;
import pulad.chb.util.V2CSHA1Value;

public class LocalURLStreamHandler extends URLStreamHandler {

	// TODO:変更可能にする
	public static final String PROTOCOL = "imgcache";
	public static final String PROTOCOL_SECURE = PROTOCOL + "s";
	public static final String PROTOCOL_LOCAL = "imglocal";
	public static final String PROTOCOL_LOCAL_SECURE = PROTOCOL_LOCAL + "s";

	public static class Factory implements URLStreamHandlerFactory {
		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			return (PROTOCOL.equals(protocol) ||
					PROTOCOL_SECURE.equals(protocol) ||
					PROTOCOL_LOCAL.equals(protocol) ||
					PROTOCOL_LOCAL_SECURE.equals(protocol)) ? new LocalURLStreamHandler() : null;
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
		if (App.offline || urlStr.startsWith(PROTOCOL_LOCAL)) {
			return new LocalURLConnection(u);
		}
		urlStr = urlStr.replaceFirst(PROTOCOL, "http");
		String cachePath = LinkHistManager.getCacheFileName(urlStr);
		if (cachePath == null) {
			try {
				if (LinkHistManager.lock(urlStr)) {
					DownloadDto downloadDto = DownloadProcessor.downloadBytes(
							urlStr,
							1048576 * 4,
							x -> (ImageUtil.getFileExt(x.getContentType()) != null));
					byte[] data = downloadDto.getData();
					String contentType = downloadDto.getContentType();
					if (data != null) {
						String imageExt = ImageUtil.getImageExt(contentType);
						String fileExt = ImageUtil.getFileExt(contentType);

						V2CSHA1Value sha1 = V2CSHA1Value.createInstance();
						sha1.update(data, 0, data.length);
						sha1.digest();
						String sha1FileName = sha1.toString();
						downloadDto.setImageCache(sha1FileName);
						downloadDto.setImageExt(imageExt);
						sha1FileName = sha1FileName + fileExt;

						try {
							Files.write(App.imageFolder.resolve(sha1FileName.substring(0, 1)).resolve(sha1FileName), data, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
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
				return new LocalURLConnection(u);
				//return new URL(u.toExternalForm().replaceFirst(PROTOCOL, "http")).openConnection();
			} catch (InterruptedException e) {
				throw new IOException(e);
			} finally {
				LinkHistManager.release(urlStr);
			}
		} else {
			return new LocalURLConnection(u);
		}
	}

	/**
	 * ローカルのファイルに対するURLConnectionの実装。
	 * @author pulad
	 *
	 */
	private static class LocalURLConnection extends URLConnection {
		private String url;

		public LocalURLConnection(URL u) {
			super(u);
			this.url = LocalURLStreamHandler.getSourceUrl(u.toExternalForm());
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			String cachePath = LinkHistManager.getCacheFileName(url);
			if (cachePath == null) {
				throw new IOException("みつかりません");
			}
			if (cachePath.startsWith("classpath:")) {
				return getClass().getClassLoader().getResourceAsStream(cachePath.substring(10));
			}
			return new File(cachePath).toURI().toURL().openStream();
		}

		@Override
		public String getContentType() {
			// TODO Auto-generated method stub
			return super.getContentType();
		}
	}
}
