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
	// &lrm;
	// &rlm;
	private final Pattern reg = Pattern.compile("&(#820[4-7]|#x200[c-f]|zwnj|zwj|lrm|rlm);", Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, long now) {
		res.values().parallelStream().forEach(dto -> {
			StringBuilder sb = new StringBuilder();
			String body = dto.getBody();

			Matcher matcher = reg.matcher(body);
			while (matcher.find()) {
				matcher.appendReplacement(sb, "");
			}
			matcher.appendTail(sb);
			dto.setBody(sb.toString());
		});
	}
}
