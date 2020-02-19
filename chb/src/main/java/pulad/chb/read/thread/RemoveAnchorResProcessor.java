package pulad.chb.read.thread;

import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pulad.chb.dto.ResDto;

/**
 * &lt;br&gt;以外のタグ削除
 * @author pulad
 *
 */
public class RemoveAnchorResProcessor implements ResProcessor {

	// immutable
	private final Pattern regBody = Pattern.compile("</?(?!br)[^<>]*?>", Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now) {
		for (ResDto dto : res.values()) {
			// <br>以外のタグ削除
			dto.setBody(regBody.splitAsStream(dto.getBody()).collect(Collectors.joining()));
		}
	}

}
