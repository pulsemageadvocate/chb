package pulad.chb;

import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import pulad.chb.config.Config;

/**
 * 画像ポップアップ。
 * ドラッグ可能。クリックすると閉じる。
 * @author pulad
 *
 */
public class ImageViewProcessor {

	public static Popup view(Window owner, String url) {
		Image image = new Image(url);

		// 画像が画面よりも大きい場合にサイズを調整する
		double imageWidth = image.getWidth();
		double imageHeight = image.getHeight();
		double minX = 0;
		double minY = 0;
		double maxX = 0;
		double maxY = 0;
		for (Screen s : Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight())) {
			Rectangle2D r = s.getBounds();
			if (minX > r.getMinX()) {
				minX = r.getMinX();
			}
			if (minY > r.getMinY()) {
				minY = r.getMinY();
			}
			if (maxX < r.getMaxX()) {
				maxX = r.getMaxX();
			}
			if (maxY < r.getMaxY()) {
				maxY = r.getMaxY();
			}
		}
		double screenWidth = maxX - minX;
		double screenHeight = maxY - minY;
		double scale = 1d;
		int scaleNumerator = 1;
		int scaleDenominator = 1;
		if (screenWidth >= 1d || screenHeight >= 1d) {
			while (imageWidth * scale > screenWidth ||
					imageHeight * scale > screenHeight) {
				scale /= 2d;
				scaleDenominator <<= 1;
			}
		}

		Popup popup = new Popup();
		Scene scene = popup.getScene();
		scene.getStylesheets().add(Config.styleCss);

		ImageView imageView = new ImageView(image);
		imageView.setFitWidth(image.getWidth());
		imageView.setFitHeight(image.getHeight());

		AnchorPane anchorPane = new AnchorPane(imageView);
		anchorPane.setStyle("-fx-background-color: black;");
		AnchorPane.setLeftAnchor(imageView, 0d);
		AnchorPane.setTopAnchor(imageView, 0d);
		AnchorPane.setRightAnchor(imageView, 0d);
		AnchorPane.setBottomAnchor(imageView, 0d);

		ScaleTextPane scaleTextPane = new ScaleTextPane();
		scaleTextPane.setStyle("-fx-background-color: white;");
		scaleTextPane.setScaleText(scaleNumerator, scaleDenominator);
		scaleTextPane.setOpacity(0.75d);

		Pane rootPane = new Pane(anchorPane, scaleTextPane);
		scene.setRoot(rootPane);

		scene.setOnMouseClicked(new MouseClickEventHandler(popup));
		MouseDragOverEventHandler dragOverEventHandler = new MouseDragOverEventHandler(popup);
		rootPane.setOnMouseDragOver(dragOverEventHandler);
		rootPane.setOnDragDetected(new DragDetectedEventHandler(popup, imageView, dragOverEventHandler));
		rootPane.setOnScroll(new MouseScrollEventHandler(imageView, scaleTextPane, scaleNumerator, scaleDenominator));
		// 端で止まらないようにする
		popup.setAutoFix(false);
		popup.setAutoHide(true);

		popup.show(owner);
		return popup;
	}

	private static class ScaleTextPane extends Label {
		private StringBuilder sb = new StringBuilder();

		private void setScaleText(int scaleNumerator, int scaleDenominator) {
			sb.setLength(0);
			sb.append("x");
			sb.append(scaleNumerator);
			if (scaleDenominator > 1) {
				sb.append("/");
				sb.append(scaleDenominator);
			}
			this.setText(sb.toString());
		}
	}

	private static class MouseClickEventHandler implements EventHandler<MouseEvent> {
		private Popup popup;

		public MouseClickEventHandler(Popup popup) {
			this.popup = popup;
		}

		@Override
		public void handle(MouseEvent event) {
			// ドラッグ（の時間後）の場合は反応しないようにする
			if (event.isStillSincePress()) {
				App.logger.debug("ImageViewProcessor.MouseClickEventHandler.handle();");
				popup.hide();
				event.consume();
			}
		}
	}

	private static class DragDetectedEventHandler implements EventHandler<MouseEvent> {
		private Popup popup;
		private Node node;
		private MouseDragOverEventHandler dragOverEventHandler;

		public DragDetectedEventHandler(Popup popup, Node node, MouseDragOverEventHandler dragOverEventHandler) {
			this.popup = popup;
			this.node = node;
			this.dragOverEventHandler = dragOverEventHandler;
		}

		@Override
		public void handle(MouseEvent event) {
			dragOverEventHandler.fromMouseX = event.getScreenX();
			dragOverEventHandler.fromMouseY = event.getScreenY();
			dragOverEventHandler.fromWindowX = popup.getX();
			dragOverEventHandler.fromWindowY = popup.getY();
			node.startFullDrag();
			event.consume();
		}
	}

	private static class MouseDragOverEventHandler implements EventHandler<MouseDragEvent> {
		private Popup popup;
		public volatile double fromMouseX;
		public volatile double fromMouseY;
		public volatile double fromWindowX;
		public volatile double fromWindowY;

		public MouseDragOverEventHandler(Popup popup) {
			this.popup = popup;
		}

		@Override
		public void handle(MouseDragEvent event) {
			double toMouseX = event.getScreenX();
			double toMouseY = event.getScreenY();
			double toWindowX = fromWindowX + toMouseX - fromMouseX;
			double toWindowY = fromWindowY + toMouseY - fromMouseY;
			popup.setX(toWindowX);
			popup.setY(toWindowY);
			event.consume();
		}
	}

	private static class MouseScrollEventHandler implements EventHandler<ScrollEvent> {
		private ImageView image;
		private ScaleTextPane scaleTextPane;
		private int scaleNumerator;
		private int scaleDenominator;
		private double imageWidth;
		private double imageHeight;

		public MouseScrollEventHandler(ImageView imageView, ScaleTextPane scaleTextPane, int scaleNumerator,  int scaleDenominator) {
			this.image = imageView;
			this.scaleTextPane = scaleTextPane;
			this.scaleNumerator = scaleNumerator;
			this.scaleDenominator = scaleDenominator;
			Image i = image.getImage();
			this.imageWidth = i.getWidth();
			this.imageHeight = i.getHeight();
		}

		@Override
		public void handle(ScrollEvent event) {
			double scrollY = event.getDeltaY();
			if (scrollY < 0d) {
				if (scaleNumerator <= 1) {
					scaleDenominator <<= 1;
				} else {
					scaleNumerator >>= 1;
				}
				image.setFitWidth(imageWidth * scaleNumerator / scaleDenominator);
				image.setFitHeight(imageHeight * scaleNumerator / scaleDenominator);
				scaleTextPane.setScaleText(scaleNumerator, scaleDenominator);
			} else if (scrollY > 0d) {
				if (scaleDenominator <= 1) {
					scaleNumerator <<= 1;
				} else {
					scaleDenominator >>= 1;
				}
				image.setFitWidth(imageWidth * scaleNumerator / scaleDenominator);
				image.setFitHeight(imageHeight * scaleNumerator / scaleDenominator);
				scaleTextPane.setScaleText(scaleNumerator, scaleDenominator);
			}
			event.consume();
		}
		
	}

	private ImageViewProcessor() {}
}
