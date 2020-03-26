package pulad.chb.util;

import java.util.function.Predicate;

import pulad.chb.dto.ResDto;

/**
 * したらばのID:???の場合はfalseを返す。
 * @author pulad
 *
 */
public class NotShitarabaIdPredicate implements Predicate<ResDto> {

	@Override
	public boolean test(ResDto t) {
		String id = t.getId();
		return (id != null) && (id.indexOf("???") < 0);
	}
}
