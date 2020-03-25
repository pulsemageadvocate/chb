package pulad.chb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.thymeleaf.util.StringUtils;

import pulad.chb.dto.DomainCookieDto;

public class CookieStore implements java.net.CookieStore {
	private static CookieStore instance = null;

	private final File file;
	private DomainCookieDto domainRoot;
	private LinkedHashMap<URI, HashMap<String, HttpCookie>> uriMap;
	private volatile boolean modified = false;

	public static void initialize(File file) {
		if (instance == null) {
			instance = new CookieStore(file);
		}
	}

	public static CookieStore get() {
		return instance;
	}

	public CookieStore(File file) {
		this.file = file;
		domainRoot = new DomainCookieDto();
		domainRoot.setDomainToken("");
		domainRoot.setRequestURI("");
		domainRoot.setCookie(new HashMap<>());
		domainRoot.setChild(new LinkedList<>());
		uriMap = new LinkedHashMap<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")));
			String str = null;
			while ((str = br.readLine()) != null) {
				String[] token = str.split("\t");
				if (token.length != 2) {
					App.logger.debug("chb_cookie.txtの無効なエントリ: {}", str);
					continue;
				}
				try {
					URI uri = new URI(token[0]);
					List<HttpCookie> cookie = HttpCookie.parse(token[1]);
					for (HttpCookie c : cookie) {
						String domain = c.getDomain();
						if (StringUtils.isEmpty(domain)) {
							// domain無し /////////////////////////////////////////////////
							putUri(uri, c);
						} else {
							putDomain(domain, c);
							putUri(uri, c);
						}
					}
				} catch (URISyntaxException e) {
					App.logger.error("chb_cookie.txtでエラー", e);
					continue;
				}
			}
			App.logger.debug("CookieStore: {}", domainRoot);
		} catch (FileNotFoundException e) {
			try {
				file.createNewFile();
			} catch (IOException e2) {
				App.logger.error("chb_cookie.txtでエラー", e2);
			}
		} catch (IOException e) {
			App.logger.error("chb_cookie.txtでエラー", e);
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

	private void putDomain(String targetDomain, HttpCookie cookie) {
		String[] domainToken = targetDomain.split(".");
		DomainCookieDto leaf = domainRoot;
		for (int i = domainToken.length - 1; i >= 0; i--) {
			if (StringUtils.isEmpty(domainToken[i])) {
				continue;
			}
			DomainCookieDto c = find(leaf, domainToken[i]);
			if (c == null) {
				c = new DomainCookieDto();
				c.setDomainToken(domainToken[i]);
				c.setRequestURI("");
				c.setCookie(new HashMap<>());
				c.setChild(new LinkedList<>());
			}
			leaf = c;
		}
		leaf.getCookie().put(cookie.getName(), cookie);
	}

	private List<HttpCookie> getDomain(String targetDomain) {
		List<HttpCookie> list = new LinkedList<>();
		String[] domainToken = targetDomain.split(".");
		DomainCookieDto leaf = domainRoot;
		for (int i = domainToken.length - 1; i >= 0; i--) {
			DomainCookieDto c = find(leaf, domainToken[i]);
			if (c == null) {
				break;
			}
			list.addAll(c.getCookie().values());
			leaf = c;
		}
		return list;
	}

	private boolean removeDomain(HttpCookie cookie) {
		String[] domainToken = cookie.getDomain().split(".");
		DomainCookieDto leaf = domainRoot;
		for (int i = domainToken.length - 1; i >= 0; i--) {
			DomainCookieDto c = find(leaf, domainToken[i]);
			if (c == null) {
				return false;
			}
			leaf = c;
		}
		return (leaf.getCookie().remove(cookie.getName()) != null);
	}

	private DomainCookieDto find(DomainCookieDto parent, String domainToken) {
		for (DomainCookieDto c : parent.getChild()) {
			if (c.getDomainToken().equals(domainToken)) {
				return c;
			}
		}
		return null;
	}

	private void putUri(URI uri, HttpCookie cookie) {
		URI effectiveUri = getEffectiveURI(uri);
		HashMap<String, HttpCookie> map = uriMap.get(effectiveUri);
		if (map == null) {
			map = new HashMap<>();
			uriMap.put(effectiveUri, map);
		}
		map.put(cookie.getName(), cookie);
	}

	private boolean removeUri(URI uri, HttpCookie cookie) {
		URI effectiveUri = getEffectiveURI(uri);
		HashMap<String, HttpCookie> map = uriMap.get(effectiveUri);
		if (map == null) {
			return false;
		}
		return (map.remove(cookie.getName()) != null);
	}

	@Override
	public void add(URI uri, HttpCookie cookie) {
		synchronized (domainRoot) {
			String domain = cookie.getDomain();
			if (StringUtils.isEmpty(domain)) {
				// domain無し /////////////////////////////////////////////////
				putUri(uri, cookie);
			} else {
				putDomain(domain, cookie);
				putUri(uri, cookie);
			}
			modified = true;
		}
	}

	@Override
	public List<HttpCookie> get(URI uri) {
		List<HttpCookie> result = new LinkedList<HttpCookie>();

		synchronized (domainRoot) {
			// domain無し系の検索 /////////////////////////////////////////////////////
			result.addAll(getDomain(uri.getHost()));
		}

		App.logger.debug("CookieStore get: {}", result);
		return result;
	}

	@Override
	public List<HttpCookie> getCookies() {
		List<HttpCookie> list = new LinkedList<HttpCookie>();

		synchronized (domainRoot) {
			// domain無し系の検索 /////////////////////////////////////////////////////
			getCookies0(list, domainRoot);
		}
	
		App.logger.debug("CookieStore getCookies: {}", list);
		return list;
	}

	private void getCookies0(List<HttpCookie> list, DomainCookieDto dto) {
		list.addAll(dto.getCookie().values());
		for (DomainCookieDto c : dto.getChild()) {
			getCookies0(list, c);
		}
	}

	@Override
	public List<URI> getURIs() {
		List<URI> result = null;
		synchronized (domainRoot) {
			result = Collections.unmodifiableList(new LinkedList<URI>(uriMap.keySet()));
		}
		App.logger.debug("CookieStore getURIs: {}", result);
		return result;
	}

	@Override
	public boolean remove(URI uri, HttpCookie cookie) {
		App.logger.debug("CookieStore remove called: {}", cookie);
		synchronized (domainRoot) {
			boolean result1 = removeDomain(cookie);
			boolean result2 = removeUri(uri, cookie);
			boolean result = result1 || result2;
			if (result) {
				modified = true;
			}
			return result;
		}
	}

	@Override
	public boolean removeAll() {
		throw new UnsupportedOperationException("禁止");
	}

	public void save() {
		if (!modified) {
			return;
		}
		synchronized (domainRoot) {
			StringBuilder sb = new StringBuilder();
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(file, Charset.forName("UTF-8"), false));
				for (Entry<URI, HashMap<String, HttpCookie>> entry : uriMap.entrySet()) {
					for (HttpCookie cookie : entry.getValue().values()) {
						sb.append(cookie.toString());
						sb.append("; ");
					}
					sb.setLength(sb.length() - 2);
					bw.write(entry.getKey().toString());
					bw.write("\t");
					bw.write(sb.toString());
					bw.newLine();
					sb.setLength(0);
				}
				bw.flush();
			} catch (IOException e) {
				App.logger.error("chb_cookie.txtでエラー", e);
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
		App.logger.debug("CookieStore saved");
	}

	//
	// from java.net.InMemoryCookieStore
    // for cookie purpose, the effective uri should only be http://host
    // the path will be taken into account when path-match algorithm applied
    //
    private URI getEffectiveURI(URI uri) {
        URI effectiveURI = null;
        try {
            effectiveURI = new URI("http",
                                   uri.getHost(),
                                   null,  // path component
                                   null,  // query component
                                   null   // fragment component
                                  );
        } catch (URISyntaxException ignored) {
            effectiveURI = uri;
        }

        return effectiveURI;
    }
}
