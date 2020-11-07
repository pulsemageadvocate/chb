package pulad.chb.config;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pulad.chb.dto.ConfigFileDto;

/**
 * chb_config.txtファイルを読み書きする。
 * @author pulad
 *
 */
public class Config {
	private static Logger logger = LoggerFactory.getLogger(Config.class);
	private static Path rootFolder;
	private static Path bbsFolder;
	private static Path logFolder;
	private static Path scriptFolder;
	private static Path imageFolder;
	private static Path styleFolder;
	private static Path linkhistFile;
	private static Path configFile;
	private static File file;
	private static ObjectMapper mapper;
	public static String ua = "chb/0.0.1-SNAPSHOT";
	public static String editorCommand = "C:\\Programs\\sakura\\sakura.exe $LINK";
	public static String styleCss;

	public static void init(String baseDir, Class<?> resourceClass) {
		rootFolder = Paths.get(baseDir);
		bbsFolder = Paths.get(baseDir, "BBS");
		logFolder = Paths.get(baseDir, "log");
		scriptFolder = Paths.get(baseDir, "script");
		imageFolder = Paths.get(baseDir, "image");
		styleFolder = Paths.get(baseDir, "style");
		linkhistFile = Paths.get(baseDir, "linkhist.txt");
		configFile = Paths.get(baseDir, "chb_config.txt");

		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		file = configFile.toFile();

		try {
			Path cssPath = Config.getStyleFolder().resolve("style.css");
			if (Files.exists(cssPath)) {
				styleCss = cssPath.toUri().toURL().toExternalForm();
			} else {
				// デフォルトのcssを用意する。
				styleCss = resourceClass.getResource("/style/style.css").toExternalForm();
			}
			logger.debug("styleCss: {}", styleCss);
		} catch (MalformedURLException e) {
			logger.error("style.css失敗", e);
			throw new RuntimeException("style.css失敗", e);
		}
	}

	public static Path getRootFolder() {
		return rootFolder;
	}

	public static Path getBBSFolder() {
		return bbsFolder;
	}

	public static Path getLogFolder() {
		return logFolder;
	}

	public static Path getScriptFolder() {
		return scriptFolder;
	}

	public static Path getImageFolder() {
		return imageFolder;
	}

	public static Path getStyleFolder() {
		return styleFolder;
	}

	public static Path getLinkhistFile() {
		return linkhistFile;
	}

	public static Path getConfigFile() {
		return configFile;
	}

	public static ConfigFileDto read() {
		ConfigFileDto configFileDto = null;

		synchronized(file) {
			try {
				configFileDto = mapper.readValue(file, ConfigFileDto.class);
			} catch (Exception e) {
			}
		}

		if (configFileDto == null) {
			configFileDto = new ConfigFileDto();
		}
		editorCommand = configFileDto.getEditor();
		if (editorCommand == null) {
			editorCommand = "C:\\Programs\\sakura\\sakura.exe $LINK";
		}
		return configFileDto;
	}

	public static void write(ConfigFileDto dto) {
		dto.setEditor(editorCommand);
		synchronized(file) {
			try {
				mapper.writeValue(file, dto);
			} catch (Exception e) {
				logger.error("Config write失敗", e);
			}
		}
	}

	private Config() {}
}
