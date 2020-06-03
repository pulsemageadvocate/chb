package pulad.chb.read.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;
import pulad.chb.util.NumberUtil;

/**
 * 本文中の"-XXXX"と"ID:XXXXXXXX"にリンクとNGメニューを設定する。
 * @author pulad
 *
 */
public class BodyNGAnchorResProcessor implements ResProcessor {
	private static final String anchor1 = "<a chain=\"";
	private static final String anchor2 = "\" aboneable=\"";
	private static final String anchor3 = "\">";
	private static final String anchor4 = "</a> (";
	private static final String anchor5 = ") ";

	// immutable
	private final Pattern regWacchoi = Pattern.compile("-[0-9A-Za-z+/]{4}(?![0-9A-Za-z+/\\-])",
			Pattern.CASE_INSENSITIVE);
	private final Pattern regId = Pattern.compile("(?<=ID:)[0-9A-Za-z+/]{8,}(?![0-9A-Za-z+/\\-])",
			Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now) {
		process0(res, regWacchoi, ResDto::getWacchoiLower, "wacchoiLower");
		process0(res, regId, ResDto::getId, "id");
	}

	private void process0(TreeMap<Integer, ResDto> res, Pattern reg, Function<ResDto, String> targetGetter,
			String aboneable) {
		// res.values().parallelStream().forEach(dto -> {
		for (ResDto dto : res.values()) {
			Matcher matcherBody = reg.matcher(dto.getBody());
			StringBuilder sb = new StringBuilder();
			while (matcherBody.find()) {
				String word = matcherBody.group();
				List<Integer> indexList = new ArrayList<Integer>();
				for (ResDto indexDto : res.values()) {
					String target = targetGetter.apply(indexDto);
					if (!word.equals(target)) {
						continue;
					}
					indexList.add(NumberUtil.integerCache(indexDto.getNumber()));
				}
				if (indexList.isEmpty()) {
					continue;
				}

				matcherBody.appendReplacement(sb, anchor1 + createLinkString(indexList) + anchor2 + aboneable + anchor3
						+ matcherBody.group() + anchor4 + indexList.size() + anchor5);
			}
			matcherBody.appendTail(sb);
			dto.setBody(sb.toString());
		} // );
	}

	/**
	 * ポップアップ表示対象をカンマ区切りで結合する。
	 * 
	 * @param list
	 * @return
	 */
	private String createLinkString(List<Integer> list) {
		if (list == null || list.size() <= 0) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Integer i : list) {
			sb.append(i);
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}
}
