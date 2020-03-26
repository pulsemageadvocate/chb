package pulad.chb.read.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pulad.chb.dto.ResDto;
import pulad.chb.interfaces.ResProcessor;

/**
 * 画像を抽出する
 * @author pulad
 *
 */
public class ImageResProcessor implements ResProcessor {

	// immutable
	private final Pattern regBody = Pattern.compile("h?ttp(s?://[\\w/:%#\\$&\\?\\(\\)~\\.=\\+\\-]+((?<=\\.jp(e|g|eg))|(?<=\\.png)|(?<=\\.gif)))", Pattern.CASE_INSENSITIVE);

	@Override
	public void process(String url, TreeMap<Integer, ResDto> res, boolean remote, long now) {
		for (ResDto dto : res.values()) {
			List<String> images = dto.getImages();
			if (images == null) {
				images = new ArrayList<>();
				dto.setImages(images);
			} else {
				images.clear();
			}
			
			Matcher matcher = regBody.matcher(dto.getBody());
			while (matcher.find()) {
				// remoteでなくてもレスポップアップの場合に読み込みしたいので
				//images.add((remote ? LocalURLStreamHandler.PROTOCOL : LocalURLStreamHandler.PROTOCOL_LOCAL) + matcher.group(1));
				images.add(LocalURLStreamHandler.PROTOCOL + matcher.group(1));
			}
		}
	}
}
