package pulad.chb.util;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

	/**
	 * 大文字小文字が違うファイルが存在する場合、存在するファイルに合わせる。
	 * @param path
	 * @return String
	 */
	public static String realCapital(String path) {
		return realCapital(Paths.get(path));
	}

	/**
	 * 大文字小文字が違うファイルが存在する場合、存在するファイルに合わせる。
	 * @param path
	 * @return String
	 */
	public static String realCapital(Path path) {
		File f = realCapital0(path);
		return (f == null) ? path.toString() : f.toString();
	}

	/**
	 * 大文字小文字が違うファイルが存在する場合、存在するファイルに合わせる。
	 * @param path
	 * @return File
	 */
	public static File realCapitalFile(String path) {
		return realCapitalFile(Paths.get(path));
	}

	/**
	 * 大文字小文字が違うファイルが存在する場合、存在するファイルに合わせる。
	 * @param file
	 * @return File
	 */
	public static File realCapitalFile(File file) {
		File f = realCapital0(file.toPath());
		return (f == null) ? file : f;
	}

	/**
	 * 大文字小文字が違うファイルが存在する場合、存在するファイルに合わせる。
	 * @param path
	 * @return File
	 */
	public static File realCapitalFile(Path path) {
		File f = realCapital0(path);
		return (f == null) ? path.toFile() : f;
	}

	/**
	 * 大文字小文字が違うファイルが存在する場合、存在するファイルに合わせる。
	 * @param path
	 * @return Path
	 */
	public static Path realCapitalPath(String path) {
		return realCapitalPath(Paths.get(path));
	}

	/**
	 * 大文字小文字が違うファイルが存在する場合、存在するファイルに合わせる。
	 * @param path
	 * @return Path
	 */
	public static Path realCapitalPath(Path path) {
		File f = realCapital0(path);
		return (f == null) ? path : f.toPath();
	}

	private static File realCapital0(Path path) {
		File parent = path.getParent().toFile();
		File[] list = parent.listFiles(new IgnoreCaseFilter(path.getFileName().toString()));
		if (list != null && list.length > 0) {
			return list[0];
		}
		return null;
	}

	private FileUtil() {}

	private static class IgnoreCaseFilter implements FilenameFilter {
		private String fileName;

		private IgnoreCaseFilter(String fileName) {
			this.fileName = fileName.toLowerCase();
		}

		@Override
		public boolean accept(File dir, String name) {
			return (this.fileName.equals(name.toLowerCase()));
		}
	}
}
