package pulad.chb.util;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * DTOの{@link String}型プロパティの値が指定した正規表現にマッチするか検索する{@link Predicate}。
 * @author pulad
 *
 * @param <T>
 */
public class RegexPredicate<T> implements Predicate<T> {
	private Pattern pattern;
	private Function<T, String> targetGetter;

	/**
	 * コンストラクタ。
	 * @param regex 正規表現
	 * @param targetGetter 検索対象の文字列を取得する{@link Function}
	 */
	public RegexPredicate(String regex, Function<T, String> targetGetter) {
		this(regex, 0, targetGetter);
	}

	/**
	 * コンストラクタ。
	 * @param regex 正規表現
	 * @param flag 正規表現のフラグ
	 * @param targetGetter 検索対象の文字列を取得する{@link Function}
	 */
	public RegexPredicate(String regex, int flags, Function<T, String> targetGetter) {
		this.pattern = Pattern.compile(regex, flags);
		this.targetGetter = targetGetter;
	}

	@Override
	public boolean test(T t) {
		if (t == null) {
			return false;
		}
		String target = targetGetter.apply(t);
		return (target != null) && pattern.matcher(target).find();
	}
}
