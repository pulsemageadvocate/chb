package pulad.chb.util;

public class ImageUtil {

	public static String getImageExt(String contentType) {
		if (contentType == null) {
			return null;
		}
		switch (contentType.toLowerCase()) {
		case "image/gif":
			return "2";
		case "image/jpeg":
		case "image/pjpeg":
			return "3";
		case "image/png":
		case "image/x-png":
			return "4";
		case "image/tiff":
			return "5";
		case "image/bmp":
			return "6";
		case "image/webp":
			return "7";
		case "video/webm":
			return "a";
		}
		return null;
	}

	public static String getFileExt(String contentType) {
		if (contentType == null) {
			return null;
		}
		switch (contentType.toLowerCase()) {
		case "image/gif":
			return ".gif";
		case "image/jpeg":
		case "image/pjpeg":
			return ".jpg";
		case "image/png":
		case "image/x-png":
			return ".png";
		case "image/tiff":
			return ".tif";
		case "image/bmp":
			return ".bmp";
		case "image/webp":
			return ".webp";
		case "video/webm":
			return ".webm";
		}
		return null;
	}

	private ImageUtil() {}
}
