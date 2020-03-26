package pulad.chb.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeUtil {
	private static final Clock clock = Clock.system(ZoneId.of("Asia/Tokyo"));
	private static final ZoneOffset zoneOffset = clock.getZone().getRules().getOffset(clock.instant());
	private static final Pattern regResTime = Pattern.compile("^(?<year>[0-9]{4})/(?<month>[0-9]{2})/(?<day>[0-9]{2})\\(.\\) (?<hour>[0-9]{2}):(?<minute>[0-9]{2}):(?<second>[0-9]{2})(\\.(?<fraction>[0-9]{0,9}))?");

	public static LocalDateTime now() {
		return LocalDateTime.now(clock);
	}

	public static LocalDateTime httpLongToLocalDateTime(long l) {
		if (l == 0L) {
			return null;
		}
		return LocalDateTime.ofEpochSecond(l / 1000L, (int)(l % 1000L) * 1000000, zoneOffset);
	}

	public static long localDateTimeToHttpLong(LocalDateTime dt) {
		if (dt == null) {
			return 0L;
		}
		return dt.toEpochSecond(zoneOffset) * 1000L + (long)(dt.getNano() / 1000000);
	}

	public static LocalDateTime parseResTime(String resTime) {
		Matcher matcher = regResTime.matcher(resTime);
		if (!matcher.find()) {
			return null;
		}

		String nanoSecondsStr = matcher.group("fraction");
		int nanoSeconds;
		if (nanoSecondsStr == null) {
			nanoSeconds = 0;
		} else {
			// 0.9 ⇒ 900000000
			// 0.91 ⇒ 910000000
			nanoSeconds = NumberUtil.parseInt(nanoSecondsStr, 0);
			for (int i = nanoSecondsStr.length(); i < 9; i++) {
				nanoSeconds *= 10;
			}
		}
		return LocalDateTime.of(
				NumberUtil.parseInt(matcher.group("year"), 0),
				NumberUtil.parseInt(matcher.group("month"), 0),
				NumberUtil.parseInt(matcher.group("day"), 0),
				NumberUtil.parseInt(matcher.group("hour"), 0),
				NumberUtil.parseInt(matcher.group("minute"), 0),
				NumberUtil.parseInt(matcher.group("second"), 0),
				nanoSeconds);
	}

	private DateTimeUtil() {}
}
