package pulad.chb.read.thread;

import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;

/**
 * 表示するとアプリが落ちる文字を変換する。
 * @author pulad
 *
 */
public class ReplaceErrorCharacterResProcessor implements ResProcessor {
	// 絵文字結合
//	private final Pattern regToNumerical10 = Pattern.compile("&(?=#(?<i>820[45]);)", Pattern.CASE_INSENSITIVE);
//	private final Pattern regToNumerical16 = Pattern.compile("&(?=#x(?<i>200[cd]);)", Pattern.CASE_INSENSITIVE);
	// &lrm;
//	private final Pattern regToLRM = Pattern.compile("&(?=(#x200e|#8206|lrm);)", Pattern.CASE_INSENSITIVE);
	// &rlm;
//	private final Pattern regToRLM = Pattern.compile("&(?=(#x200f|#8207|rlm);)", Pattern.CASE_INSENSITIVE);
	private final Pattern reg = Pattern.compile("&(?=(#820[4-7]|#x200[c-f]|lrm|rlm);)", Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now) {
		res.values().parallelStream().forEach(dto -> {
			StringBuilder sb = new StringBuilder();
			String body = dto.getBody();

			Matcher matcher = reg.matcher(body);
			while (matcher.find()) {
				matcher.appendReplacement(sb, "&amp;");
			}
			matcher.appendTail(sb);
			dto.setBody(sb.toString());
		});
	}
}
