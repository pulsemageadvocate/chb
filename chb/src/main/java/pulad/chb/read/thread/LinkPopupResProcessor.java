package pulad.chb.read.thread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.thymeleaf.util.StringUtils;

import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;
import pulad.chb.util.NumberUtil;

/**
 * 同一ﾜｯﾁｮｲ、ﾜｯﾁｮｲ下4桁、IP、IDのポップアップ内容を設定する。
 * @author pulad
 *
 */
public class LinkPopupResProcessor implements ResProcessor {

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, long now) {
		// 名前
		process0(res, ResDto::getName, ((Predicate<ResDto>) ResDto::isAnonymous).negate(), ResDto::setNameLink);
		// ﾜｯﾁｮｲ
		process0(res, ResDto::getWacchoi, x -> true, ResDto::setWacchoiLink);
		// ﾜｯﾁｮｲ下4桁
		process0(res, ResDto::getWacchoiLower, x -> true, ResDto::setWacchoiLowerLink);
		// IP
		process0(res, ResDto::getIp, x -> true, ResDto::setIpLink);
		// ID
		process0(res, ResDto::getId, x -> true, ResDto::setIdLink);
	}

	private void process0(TreeMap<Integer, ResDto> res, Function<ResDto, String> targetGetter, Predicate<ResDto> linkable, BiConsumer<ResDto, String> linkSetter) {
		HashMap<String, List<Integer>> chainList = new HashMap<>();
		for (ResDto dto : res.values()) {
			String target = targetGetter.apply(dto);
			// ﾜｯﾁｮｲやIDが無い場合はリンクしない
			if (StringUtils.isEmpty(target)) {
				continue;
			}
			// 名無しはリンクしない
			if (!linkable.test(dto)) {
				continue;
			}

			List<Integer> indexList = chainList.get(target);
			if (indexList == null) {
				indexList = new ArrayList<Integer>();
				chainList.put(target, indexList);
			}
			indexList.add(NumberUtil.integerCache(dto.getNumber()));
		}

		HashMap<String, String> stringChainList = new HashMap<>();
		chainList.forEach((x, y) -> stringChainList.put(x, createLinkString(y)));

		for (ResDto dto : res.values()) {
			String target = targetGetter.apply(dto);
			// ﾜｯﾁｮｲやIDが無い場合はリンクしない
			if (StringUtils.isEmpty(target)) {
				continue;
			}

			linkSetter.accept(dto, stringChainList.get(target));
		}
	}

	/**
	 * ポップアップ表示対象をカンマ区切りで結合する。
	 * 1つのみの場合もNGはポップアップするので返す。
	 * @param list
	 * @return
	 */
	private String createLinkString(List<Integer> list) {
		if (list.size() <= 0) {
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
