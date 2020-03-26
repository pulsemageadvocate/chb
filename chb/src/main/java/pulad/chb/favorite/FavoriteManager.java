package pulad.chb.favorite;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.thymeleaf.util.StringUtils;

import pulad.chb.App;
import pulad.chb.bbs.BBSManager;
import pulad.chb.config.Config;
import pulad.chb.interfaces.BBS;

public class FavoriteManager {

	public static void addFavorite(String url) {
		synchronized (FavoriteManager.class) {
			Path path = Config.getRootFolder().resolve("favorite.txt");
			List<String> lines;

			try {
				lines = Files.readAllLines(path, Charset.forName("UTF-8"));
			} catch (IOException e) {
				App.logger.error("addFavorite失敗", e);
				lines = Collections.singletonList("0,C,Jane");
			}

			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter(path.toFile(), Charset.forName("UTF-8")));

				for (String s : lines) {
					if (!StringUtils.isEmpty(s)) {
						bw.write(s);
						bw.newLine();
					}
				}

				BBS bbsObject = BBSManager.getBBSFromUrl(url);
				String boardUrl = bbsObject.getBoardUrlFromThreadUrl(url);
				String datFileName = bbsObject.getDatFileNameFromThreadUrl(url);
				if (!StringUtils.isEmpty(boardUrl) && !StringUtils.isEmpty(datFileName)) {
					bw.write("1,");
					bw.write(boardUrl);
					bw.write(",");
					bw.write(datFileName);
					bw.write(",");
				} else {
					bw.write("1,");
					bw.write(url);
				}
				bw.newLine();
				bw.newLine();
				bw.flush();
			} catch (IOException e) {
				App.logger.error("addFavorite失敗", e);
			} finally {
				if (bw != null) {
					try {
						bw.close();
					} catch (IOException e) {
					}
					bw = null;
				}
			}

			App.getInstance().reloadFavorite();
		}
	}

	private FavoriteManager() {}
}
