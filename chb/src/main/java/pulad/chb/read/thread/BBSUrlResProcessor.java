package pulad.chb.read.thread;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.bbs.BBSManager;
import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.BBS;
import pulad.chb.interfaces.ResProcessor;

/**
 * 表示可能な掲示板URLのリンクを設定する。
 * @author pulad
 *
 */
public class BBSUrlResProcessor implements ResProcessor {
	private static final String anchor1 = "<a bbsurl=\"";
	private static final String anchor2 = "\">";
	private static final String anchor3 = "</a>";

	private static final Pattern regUrl = Pattern.compile("(?<![\\\"\\>])h?ttps?://[0-9A-Za-z\\-_./]*");

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, long now) {
		StringBuilder sb = new StringBuilder();
		for (ResDto dto : res.values()) {
			Matcher matcher = regUrl.matcher(dto.getBody());
			while (matcher.find()) {
				BBS bbsObject = BBSManager.getBBSFromUrl(matcher.group());
				if (bbsObject == null) {
					matcher.appendReplacement(sb, "$0");
					continue;
				}
				matcher.appendReplacement(sb, anchor1 + "$0" + anchor2 + "$0" + anchor3);
			}
			matcher.appendTail(sb);
			dto.setBody(sb.toString());
			sb.setLength(0);
		}
	}
}
