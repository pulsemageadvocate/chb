package pulad.chb.read.thread;

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;
import pulad.chb.util.NumberUtil;

/**
 * &gt;&gt;1等をリンクにする。
 * 参照元ポップアップのためにリンク元をまとめる。
 * 連鎖あぼ～んのためにリンク先をまとめる。
 * @author pulad
 *
 */
public class AnchorLinkResProcessor implements ResProcessor {
	private static final String anchor1 = "<a href=\"#R";
	private static final String anchor2 = "\" chain=\"";
	private static final String anchor3 = "\">";
	private static final String anchor4 = "</a>";

	// immutable
	private final Pattern regBody = Pattern.compile("(＞|&gt;)+(?<a>[1-9][0-9]*(-[1-9][0-9]*)?)(?<b>(,[1-9][0-9]*(-[1-9][0-9]*)?)*)", Pattern.CASE_INSENSITIVE);
	private final Pattern regIndividual = Pattern.compile(",?(?<from>[1-9][0-9]*)(-(?<to>[1-9][0-9]*))?", Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, long now) {
		res.values().parallelStream().forEach(dto -> {
			Matcher matcherBody = regBody.matcher(dto.getBody());
			StringBuilder sb = new StringBuilder();
			while (matcherBody.find()) {
				// 参照先
				Set<Integer> referSet = dto.getReferSet();
				if (referSet == null) {
					referSet = new TreeSet<Integer>();
					dto.setReferSet(referSet);
				}

				// このリンクの参照先
				Set<Integer> linkReferSet = new TreeSet<Integer>();

				//?<a>
				Matcher matcherIndividual = regIndividual.matcher(matcherBody.group("a"));
				matcherIndividual.find();
				int from = Integer.parseInt(matcherIndividual.group("from"));
				int to = NumberUtil.parseInt(matcherIndividual.group("to"), -1);
				addLinkRefer(linkReferSet, from, to);
				//?<b>
				matcherIndividual = regIndividual.matcher(matcherBody.group("b"));
				while (matcherIndividual.find()) {
					from = Integer.parseInt(matcherIndividual.group("from"));
					to = NumberUtil.parseInt(matcherIndividual.group("to"), -1);
					addLinkRefer(linkReferSet, from, to);
				}

				// ポップアップ用にカンマ区切り
				String chain = createLinkString(linkReferSet);
				matcherBody.appendReplacement(sb, anchor1 + from + anchor2 + chain + anchor3 + matcherBody.group() + anchor4);
				// このレス全体の参照先
				referSet.addAll(linkReferSet);

				// 参照元
				for (Integer referIndex : referSet) {
					ResDto referDto = res.get(referIndex);
					if (referDto == null) {
						continue;
					}

					Set<Integer> referredSet = referDto.getReferredSet();
					if (referredSet == null) {
						referredSet = new TreeSet<Integer>();
						referDto.setReferredSet(referredSet);
					}
					referredSet.add(NumberUtil.integerCache(dto.getNumber()));
				}
			}
			matcherBody.appendTail(sb);
			dto.setBody(sb.toString());
		});
		// 参照元ポップアップ用にカンマ区切り
		res.values().parallelStream().forEach(dto -> {
			dto.setReferredLink(createLinkString(NumberUtil.integerCache(dto.getNumber()), dto.getReferredSet()));
		});
	}

	private void addLinkRefer(Set<Integer> linkReferSet, int from, int to) {
		if (to <= 0) {
			linkReferSet.add(NumberUtil.integerCache(from));
		} else {
			for (int i = from; i <= to; i++) {
				linkReferSet.add(NumberUtil.integerCache(i));
			}
		}
	}

	/**
	 * ポップアップ表示対象をカンマ区切りで結合する。
	 * @param list
	 * @return
	 */
	private String createLinkString(Set<Integer> set) {
		if (set == null || set.size() <= 0) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Integer i : set) {
			sb.append(i);
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	/**
	 * ポップアップ表示対象をカンマ区切りで結合する。
	 * @param first
	 * @param list
	 * @return
	 */
	private String createLinkString(Integer first, Set<Integer> set) {
		StringBuilder sb = new StringBuilder();
		sb.append(first);
		if (set != null) {
			for (Integer i : set) {
				sb.append(",");
				sb.append(i);
			}
		}
		return sb.toString();
	}

}
