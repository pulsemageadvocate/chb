package pulad.chb.dto;

import java.io.Serializable;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.LinkedList;

public class DomainCookieDto implements Serializable {
	private String domainToken;
	private String requestURI;
	private HashMap<String, HttpCookie> cookie;
	private LinkedList<DomainCookieDto> child;
	public String getDomainToken() {
		return domainToken;
	}
	public void setDomainToken(String domainToken) {
		this.domainToken = domainToken;
	}
	public String getRequestURI() {
		return requestURI;
	}
	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}
	public HashMap<String, HttpCookie> getCookie() {
		return cookie;
	}
	public void setCookie(HashMap<String, HttpCookie> cookie) {
		this.cookie = cookie;
	}
	public LinkedList<DomainCookieDto> getChild() {
		return child;
	}
	public void setChild(LinkedList<DomainCookieDto> child) {
		this.child = child;
	}
}
