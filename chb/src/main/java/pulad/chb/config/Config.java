package pulad.chb.config;

import java.io.File;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pulad.chb.App;
import pulad.chb.dto.ConfigFileDto;

/**
 * chb_config.txtファイルを読み書きする。
 * @author pulad
 *
 */
public class Config {
	private static final File file;
	private static ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		file = App.configFile.toFile();
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
		return configFileDto;
	}

	public static void write(ConfigFileDto dto) {
		synchronized(file) {
			try {
				mapper.writeValue(file, dto);
			} catch (Exception e) {
				App.logger.error("Config write失敗", e);
			}
		}
	}

	private Config() {}
}
