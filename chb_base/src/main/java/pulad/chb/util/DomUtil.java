package pulad.chb.util;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class DomUtil {

	/**
	 * ElementからAttributeの値を取得する。
	 * 存在しない場合はnullを返す。
	 * @param target Element
	 * @param name Attribute名
	 * @return Attributeの値
	 */
	public static String getAttribute(Node target, String name) {
		NamedNodeMap attributes = target.getAttributes();
		if (attributes == null) {
			return null;
		}
		Node attribute = attributes.getNamedItem(name);
		if (attribute == null) {
			return null;
		}
		return attribute.getNodeValue();
	}

	/**
	 * Elementとその先祖から最初に見つかったAttributeの値を取得する。
	 * 見つからない場合はnullを返す。
	 * @param target Element
	 * @param name Attribute名
	 * @return Attributeの値
	 */
	public static String searchAttribute(Node target, String name) {
		Node n = target;
		while (n != null) {
			NamedNodeMap attributes = n.getAttributes();
			if (attributes != null) {
				Node attribute = attributes.getNamedItem(name);
				if (attribute != null) {
					return attribute.getNodeValue();
				}
			}
			n = n.getParentNode();
		}
		return null;
	}

	private DomUtil() {}
}
