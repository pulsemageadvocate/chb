package pulad.chb.read.thread;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pulad.chb.dto.ImageDto;

/**
 * ローカルのファイルに対するURLConnectionの実装。
 * @author pulad
 *
 */
public class LocalHttpURLConnection extends HttpURLConnection {
	private String contentType;
	private long expiration;
	private long date;
	private long lastModified;
	private String location;
	private ArrayList<String> headerKeyList;
	private LinkedHashMap<String, List<String>> headerFields;

	public LocalHttpURLConnection(
			URL u,
			int responseCode,
			String contentType,
			long contentLength,
			long expiration,
			long date,
			long lastModified,
			String location,
			String fileName) {
		super(u);
		super.setFollowRedirects(false);
		super.setInstanceFollowRedirects(false);
		// 3xxに余計な処理があるので410Goneにしておく
		super.responseCode = (300 <= responseCode && responseCode < 400) ? 410 : responseCode;
		
		headerKeyList = new ArrayList<>();
		headerFields = new LinkedHashMap<>();
		
		this.contentType = contentType;
		headerKeyList.add("content-type");
		headerFields.put("content-type", List.of(contentType));
		
		super.fixedContentLength = (int) contentLength;
		super.fixedContentLengthLong = contentLength;
		headerKeyList.add("content-length");
		headerFields.put("content-length", List.of(Long.toString(contentLength)));
		
		this.expiration = expiration;
		headerKeyList.add("expires");
		headerFields.put("expires", List.of(Long.toString(expiration)));
		
		this.date = date;
		headerKeyList.add("date");
		headerFields.put("date", List.of(Long.toString(date)));
		
		this.lastModified = lastModified;
		headerKeyList.add("last-modified");
		headerFields.put("last-modified", List.of(Long.toString(lastModified)));

		this.location = location;
		headerKeyList.add("Location");
		headerFields.put("Location", List.of(location));
	}

	@Override
	public void setAuthenticator(Authenticator auth) {
	}

	@Override
	public String getHeaderFieldKey(int n) {
		return headerKeyList.get(n);
	}

	@Override
	public void setFixedLengthStreamingMode(int contentLength) {
	}

	@Override
	public void setFixedLengthStreamingMode(long contentLength) {
	}

	@Override
	public void setChunkedStreamingMode(int chunklen) {
	}

	@Override
	public String getHeaderField(int n) {
		return headerFields.get(headerKeyList.get(n)).get(0);
	}

	@Override
	public void setRequestMethod(String method) throws ProtocolException {
	}

	@Override
	public String getRequestMethod() {
		return method;
	}

	@Override
	public int getResponseCode() throws IOException {
		return responseCode;
	}

	@Override
	public String getResponseMessage() throws IOException {
		return "CACHE LocalHttpHRLConnection";
	}

	@Override
	public Permission getPermission() throws IOException {
		throw new UnsupportedOperationException("実装されていません。");
	}

	@Override
	public void setConnectTimeout(int timeout) {
	}

	@Override
	public int getConnectTimeout() {
		return 0;
	}

	@Override
	public void setReadTimeout(int timeout) {
	}

	@Override
	public int getReadTimeout() {
		return 0;
	}

	@Override
	public URL getURL() {
		return url;
	}

	@Override
	public int getContentLength() {
		return super.fixedContentLength;
	}

	@Override
	public long getContentLengthLong() {
		return super.fixedContentLengthLong;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public String getContentEncoding() {
		return null;
	}

	@Override
	public long getExpiration() {
		return this.expiration;
	}

	@Override
	public long getDate() {
		return this.date;
	}

	@Override
	public long getLastModified() {
		return this.lastModified;
	}

	@Override
	public String getHeaderField(String name) {
		return headerFields.get(name).get(0);
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		return Collections.unmodifiableMap(headerFields);
	}

	@Override
	public Object getContent() throws IOException {
		throw new UnsupportedOperationException("実装されていません。");
	}

	@Override
	public Object getContent(Class<?>[] classes) throws IOException {
		throw new UnsupportedOperationException("実装されていません。");
	}

	@Override
	public InputStream getInputStream() throws IOException {
		String urlStr = super.url.toString();
		ImageDto cache = LinkHistManager.getCache(urlStr);
		if (cache == null) {
			return getClass().getClassLoader().getResourceAsStream("image/notfound.gif");
		}
		String cachePath = cache.getFileName();
		if (cachePath.startsWith("classpath:")) {
			return getClass().getClassLoader().getResourceAsStream(cachePath.substring(10));
		}
		return new File(cachePath).toURI().toURL().openStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("実装されていません。");
	}

	@Override
	public InputStream getErrorStream() {
		return null;
	}

	@Override
	public void setDoInput(boolean doinput) {
	}

	@Override
	public boolean getDoInput() {
		return true;
	}

	@Override
	public void setDoOutput(boolean dooutput) {
	}

	@Override
	public boolean getDoOutput() {
		return false;
	}

	@Override
	public void setAllowUserInteraction(boolean allowuserinteraction) {
	}

	@Override
	public boolean getAllowUserInteraction() {
		return false;
	}

	@Override
	public void setUseCaches(boolean usecaches) {
	}

	@Override
	public boolean getUseCaches() {
		return true;
	}

	@Override
	public void setIfModifiedSince(long ifmodifiedsince) {
	}

	@Override
	public long getIfModifiedSince() {
		return 0;
	}

	@Override
	public boolean getDefaultUseCaches() {
		return true;
	}

	@Override
	public void setDefaultUseCaches(boolean defaultusecaches) {
	}

	@Override
	public void setRequestProperty(String key, String value) {
	}

	@Override
	public void addRequestProperty(String key, String value) {
	}

	@Override
	public String getRequestProperty(String key) {
		return null;
	}

	@Override
	public Map<String, List<String>> getRequestProperties() {
		throw new UnsupportedOperationException("実装されていません。");
	}

	@Override
	public void disconnect() {
	}

	@Override
	public boolean usingProxy() {
		return false;
	}

	@Override
	public void connect() throws IOException {
	}

}
