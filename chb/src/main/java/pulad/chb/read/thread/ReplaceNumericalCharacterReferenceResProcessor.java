package pulad.chb.read.thread;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;

/**
 * 表示できない数値文字参照を変換する。
 * @author pulad
 *
 */
public class ReplaceNumericalCharacterReferenceResProcessor implements ResProcessor {
	private Logger logger = LoggerFactory.getLogger(ReplaceNumericalCharacterReferenceResProcessor.class);
	private final Pattern reg10 = Pattern.compile("&(?=#(?<i>[0-9]+);)", Pattern.CASE_INSENSITIVE);
	private final Pattern reg16 = Pattern.compile("&(?=#x(?<i>[0-9a-f]+);)", Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now) {
		res.values().parallelStream().forEach(dto -> {
			StringBuilder sb = new StringBuilder();
			String body = dto.getBody();
			Matcher matcher = reg10.matcher(body);
			while (matcher.find()) {
				int i = Integer.parseInt(matcher.group("i"), 10);
				//if (i >= 65536) {
					matcher.appendReplacement(sb, "&amp;");
					logger.debug("{} -> {}", new String(new int[]{i}, 0, 1), "&#" + i + ";");
				//}
			}
			matcher.appendTail(sb);
			body = sb.toString();

			sb.setLength(0);
			matcher = reg16.matcher(body);
			while (matcher.find()) {
				int i = Integer.parseInt(matcher.group("i"), 16);
				//if (i >= 65536) {
					matcher.appendReplacement(sb, "&amp;");
					logger.debug("{} -> {}", new String(new int[]{i}, 0, 1), "&#x" + Integer.toHexString(i) + ";");
				//}
			}
			matcher.appendTail(sb);
			dto.setBody(sb.toString());
		});
	}
}
