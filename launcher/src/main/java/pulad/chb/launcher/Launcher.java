package pulad.chb.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

/**
 * jarを検索してクラスパスを動的に構成する。
 *
 */
public class Launcher {
	protected static LinkedList<URL> jarList = new LinkedList<URL>();

	public static void main(String[] args){
		addJarDir(".");
		addJarDir("chb_lib");

		run(args);
	}

	protected static void addJarDir(String path) {
		try {
			Files.list(Paths.get(path))
					.filter(x -> x.getFileName().toString().endsWith(".jar"))
					.map(x -> {
						try {
							return x.toAbsolutePath().toFile().toURI().toURL();
						} catch (MalformedURLException e) {
							e.printStackTrace();
							return null;
						}
					})
					.filter(x -> x != null)
					.forEachOrdered(x -> jarList.add(x));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static void addClassDir(String path) {
		try {
			jarList.add(new File(path).toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	protected static void run(String[] args) {
		for (URL url : jarList) {
			System.out.println(url);
		}

		ClassLoader cl = new URLClassLoader(jarList.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
		Thread.currentThread().setContextClassLoader(cl);

		Class<?> cls;
		try {
			cls = Class.forName("pulad.chb.App", true, cl);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		Method main;
		try {
			main = cls.getMethod("main", new Class[]{String[].class});
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			return;
		}
		try {
			main.invoke(null, new Object[]{args});
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
