package pulad.chb.read.thread;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.thymeleaf.util.StringUtils;

import pulad.chb.config.Config;
import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;

/**
 * ReplaceStr.txtの処理。DATを変更しない。あぼーん処理より後。
 * dateにはidとauxが含まれる。{@link ResProcessor}の一番最初に実行すること。
 * ReplaceStr.txtは最初の1回だけ読み込まれる。変更した場合は再起動が必要。
 * @author pulad
 *
 */
public class ReplaceStrResProcessor implements ResProcessor {
	private static final List<Replace> replaceList;

	static {
		final Pattern regBody = Pattern.compile("^(?![;'])(?!//)\\<(?<type>[er]x2?)?\\>(?<target>[^\\t]+)\\t(?<replace>[^\\t]*)(\\t(?<category>[^\\t]*)(\\t(\\<(?<n>[0-5])\\>)?(?<url>[^\\t]*))?)?");
		List<Replace> replaceList0 = new ArrayList<>();

		Path path = Config.getRootFolder().resolve("ReplaceStr.txt");
		if (Files.exists(path)) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(path.toString(), Charset.forName("MS932")));
				String str = null;
				while ((str = br.readLine()) != null) {
					Matcher matcher = regBody.matcher(str);
					while (matcher.find()) {
						String n = matcher.group("n");
						Replace replace = new Replace(
								matcher.group("type"),
								matcher.group("target"),
								matcher.group("replace"),
								(n == null) ? 0 : Integer.parseInt(n),
								matcher.group("url"));

						String s = matcher.group("category");
						if (s == null) {
							// 指定しない場合はall
							replace.category = Category.all;
						} else switch (s.toLowerCase()) {
						case "name":
							replace.category = Category.name;
							break;
						case "mail":
							replace.category = Category.mail;
							break;
						case "date":
							replace.category = Category.date;
							break;
						case "msg":
							replace.category = Category.msg;
							break;
						default:
							// 指定しない場合はall
							replace.category = Category.all;
						}

						replaceList0.add(replace);
					}
				}
			} catch (IOException e) {
				throw new RuntimeException("ReplaceStr.txt読み込みエラー", e);
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
					}
					br = null;
				}
			}
		}
		replaceList = Collections.unmodifiableList(replaceList0);
	}

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, long now) {
		res.values().parallelStream().forEach(x -> {
			for (Replace replace : replaceList) {
				replace.process(url, x);
			}
		});
	}

	/**
	 * ReplaceStr.txtの1行分の処理を表す。
	 * @author pulad
	 *
	 */
	private static class Replace {
		protected Category category;
		private Predicate<String> urlPredicate = null;
		private Function<String, String> replaceFunction;

		private Replace(String type, String target, String replacement, int ngexType, String url) {
			if (!StringUtils.isEmpty(url)) {
				switch (ngexType) {
				case 0:
					this.urlPredicate = new N0Predicate(url);
					break;
				case 1:
					this.urlPredicate = new N1Predicate(url);
					break;
				case 2:
					this.urlPredicate = new N2Predicate(url);
					break;
				case 3:
					this.urlPredicate = new N3Predicate(url);
					break;
				case 4:
					this.urlPredicate = new N4Predicate(url);
					break;
				case 5:
					this.urlPredicate = new N5Predicate(url);
					break;
				}
			}

			if (type == null) {
				// 指定しない場合は<ex>
				replaceFunction = new RegexReplaceFunction(true, Pattern.quote(target), replacement);
			} else switch (type.toLowerCase()) {
			case "ex2":
				replaceFunction = x -> x.replace(target, replacement);
				break;
			case "rx":
				replaceFunction = new RegexReplaceFunction(true, target, replacement);
				break;
			case "rx2":
				replaceFunction = new RegexReplaceFunction(false, target, replacement);
				break;
			default:
				// 指定しない場合は<ex>
				replaceFunction = new RegexReplaceFunction(true, Pattern.quote(target), replacement);
			}
		}

		public void process(String url, ResDto dto) {
			// TODO:何100回も同じテストするのはおいしくない
			if (urlPredicate != null) {
				if (!urlPredicate.test(url)) {
					return;
				}
			}

			switch (category) {
			case name:
				dto.setName(replaceFunction.apply(dto.getName()));
				break;
			case mail:
				dto.setMail(replaceFunction.apply(dto.getMail()));
				break;
			case date:
				dto.setTimeIdAux(replaceFunction.apply(dto.getTimeIdAux()));
				break;
			case msg:
				dto.setBody(replaceFunction.apply(dto.getBody()));
				break;
			default: //all
				dto.setName(replaceFunction.apply(dto.getName()));
				dto.setMail(replaceFunction.apply(dto.getMail()));
				dto.setTimeIdAux(replaceFunction.apply(dto.getTimeIdAux()));
				dto.setBody(replaceFunction.apply(dto.getBody()));
				break;
			}
		}
	}

	private static class RegexReplaceFunction implements Function<String, String> {
		private Pattern reg;
		private String replacement;

		private RegexReplaceFunction(boolean ignoreCase, String target, String replacement) {
			this.replacement = replacement;
			this.reg = Pattern.compile(target, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
		}

		@Override
		public String apply(String source) {
			return reg.matcher(source).replaceAll(replacement);
		}
	}

	/**
	 * 対象URL/タイトルに0:含む
	 * @author pulad
	 *
	 */
	private static class N0Predicate implements Predicate<String> {
		private String url;

		private N0Predicate(String url) {
			this.url = url;
		}

		@Override
		public boolean test(String t) {
			return t.indexOf(url) >= 0;
		}
	}

	/**
	 * 対象URL/タイトルに1:含まない
	 * @author pulad
	 *
	 */
	private static class N1Predicate implements Predicate<String> {
		private String url;

		private N1Predicate(String url) {
			this.url = url;
		}

		@Override
		public boolean test(String t) {
			return t.indexOf(url) < 0;
		}
	}

	/**
	 * 対象URL/タイトルと2:一致
	 * @author pulad
	 *
	 */
	private static class N2Predicate implements Predicate<String> {
		private String url;

		private N2Predicate(String url) {
			this.url = url;
		}

		@Override
		public boolean test(String t) {
			return t.equals(url);
		}
	}

	/**
	 * 対象URL/タイトルと3:一致しない
	 * @author pulad
	 *
	 */
	private static class N3Predicate implements Predicate<String> {
		private String url;

		private N3Predicate(String url) {
			this.url = url;
		}

		@Override
		public boolean test(String t) {
			return !t.equals(url);
		}
	}

	/**
	 * 対象URL/タイトルに4:含む（正規表現）
	 * @author pulad
	 *
	 */
	private static class N4Predicate implements Predicate<String> {
		private Pattern reg;

		private N4Predicate(String url) {
			this.reg = Pattern.compile(url);
		}

		@Override
		public boolean test(String t) {
			return reg.matcher(t).matches();
		}
	}

	/**
	 * 対象URL/タイトルに5:含まない（正規表現）
	 * @author pulad
	 *
	 */
	private static class N5Predicate implements Predicate<String> {
		private Pattern reg;

		private N5Predicate(String url) {
			this.reg = Pattern.compile(url);
		}

		@Override
		public boolean test(String t) {
			return !reg.matcher(t).matches();
		}
	}

	private enum Category {
		name,
		mail,
		date,
		msg,
		all
	}
}
