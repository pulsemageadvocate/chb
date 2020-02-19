package pulad.chb.util;

import java.util.Comparator;

/**
 * 未読が多い順に並べる{@link Comparator}。ログが無いスレッドはその後。
 * @author pulad
 *
 */
public class ResCountComparator implements Comparator<Integer> {

	@Override
	public int compare(Integer o1, Integer o2) {
		if (o1 == null) {
			return (o2 == null) ? 0 : 1;
		}
		if (o2 == null) {
			return -1;
		}
		return (o2.intValue() - o1.intValue());
	}

}
