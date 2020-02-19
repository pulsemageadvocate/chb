package pulad.chb.read.thread;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import pulad.chb.dto.ResDto;
import pulad.chb.util.NotShitarabaIdPredicate;

public class CountResProcessor implements ResProcessor {
	private final Predicate<ResDto> notShitarabaId = new NotShitarabaIdPredicate();

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now) {
		HashMap<String, AtomicInteger> nameMap = new HashMap<>(64);
		HashMap<String, AtomicInteger> wacchoiMap = new HashMap<>(64);
		HashMap<String, AtomicInteger> wacchoiLowerMap = new HashMap<>(64);
		HashMap<String, AtomicInteger> ipMap = new HashMap<>(64);
		HashMap<String, AtomicInteger> idMap = new HashMap<>(64);

		for (ResDto dto : res.values()) {
			// 名無しはカウントしない
			if (!dto.isAnonymous()) {
				dto.setNameIndex(count(nameMap, dto.getName()));
			}
			dto.setWacchoiIndex(count(wacchoiMap, dto.getWacchoi()));
			dto.setWacchoiLowerIndex(count(wacchoiLowerMap, dto.getWacchoiLower()));
			dto.setIpIndex(count(ipMap, dto.getIp()));
			// ID:???0はカウントしない
			if (notShitarabaId.test(dto)) {
				dto.setIdIndex(count(idMap, dto.getId()));
			}
		}
		AtomicInteger notFound = new AtomicInteger(0);
		for (ResDto dto : res.values()) {
			dto.setNameCount(dto.isAnonymous() ? 0 : nameMap.getOrDefault(dto.getName(), notFound).intValue());
			dto.setWacchoiCount(wacchoiMap.getOrDefault(dto.getWacchoi(), notFound).intValue());
			dto.setWacchoiLowerCount(wacchoiLowerMap.getOrDefault(dto.getWacchoiLower(), notFound).intValue());
			dto.setIpCount(ipMap.getOrDefault(dto.getIp(), notFound).intValue());
			dto.setIdCount(notShitarabaId.test(dto) ? idMap.getOrDefault(dto.getId(), notFound).intValue() : 0);
		}
	}

	/**
	 * mapに出現数を追加する。現在のインデックスを返す。キーが空の場合は処理せず0を返す。
	 * @param map
	 * @param key
	 * @return
	 */
	private int count(HashMap<String, AtomicInteger> map, String key) {
		if (key == null || key.isEmpty()) {
			return 0;
		}
		AtomicInteger value = map.get(key);
		if (value == null) {
			map.put(key, new AtomicInteger(1));
			return 1;
		} else {
			return value.incrementAndGet();
		}
	}
}
