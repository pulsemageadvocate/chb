package pulad.chb.read.thread;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;

public class BodyWacchoiNGAnchorResProcessor implements ResProcessor {
	private static final String anchor1 = "<a aboneable=\"wacchoiLower\">";
	private static final String anchor2 = "</a>";

	// immutable
	private final Pattern regBody = Pattern.compile("-[0-9A-Za-z+/]{4}(?![0-9A-Za-z+/\\-])", Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now) {
		res.values().parallelStream().forEach(dto -> {
			Matcher matcherBody = regBody.matcher(dto.getBody());
			StringBuilder sb = new StringBuilder();
			while (matcherBody.find()) {
				matcherBody.appendReplacement(sb, anchor1 + matcherBody.group() + anchor2);
			}
			matcherBody.appendTail(sb);
			dto.setBody(sb.toString());
		});
	}
}
