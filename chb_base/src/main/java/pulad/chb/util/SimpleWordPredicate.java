package pulad.chb.util;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * DTOの{@link String}型プロパティの値に指定した文字列が含まれるか検索する{@link Predicate}。
 * @author pulad
 *
 * @param <T>
 */
public class SimpleWordPredicate<T> implements Predicate<T> {
	private String word;
	private Function<T, String> targetGetter;

	/**
	 * コンストラクタ。
	 * @param word 検索文字列
	 * @param targetGetter 検索対象の文字列を取得する{@link Function}
	 */
	public SimpleWordPredicate(String word, Function<T, String> targetGetter) {
		this.word = word;
		this.targetGetter = targetGetter;
	}

	@Override
	public boolean test(T t) {
		if (t == null) {
			return false;
		}
		String target = targetGetter.apply(t);
		return (target != null) && (target.indexOf(word) >= 0);
	}
}
