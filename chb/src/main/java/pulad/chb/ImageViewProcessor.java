package pulad.chb;

import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
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
		if (screenWidth >= 1d || screenHeight >= 1d) {
			while (imageWidth * scale > screenWidth ||
					imageHeight * scale > screenHeight) {
				scale /= 2d;
			}
		}

		Popup popup = new Popup();
		Scene scene = popup.getScene();
		scene.getStylesheets().add(Config.styleCss);

		ImageView imageView = new ImageView(image);
		// 縮小時は中央に縮小、拡大時は右下へ伸びるのを右下へ統一する。
		imageView.setFitWidth(image.getWidth() / 16d);
		imageView.setFitHeight(image.getHeight() / 16d);
		imageView.setScaleX(imageView.getScaleX() * scale * 16d);
		imageView.setScaleY(imageView.getScaleY() * scale * 16d);
//		AnchorPane rootPane = new AnchorPane(imageView);
//		rootPane.setStyle("-fx-background-color: black;");
//		rootPane.setPadding(new Insets(1d));
//		AnchorPane.setLeftAnchor(imageView, 100d);
//		AnchorPane.setTopAnchor(imageView, 100d);
//		AnchorPane.setRightAnchor(imageView, 100d);
//		AnchorPane.setBottomAnchor(imageView, 100d);
//		scene.setRoot(rootPane);

//		Pane rootPane = new Pane(imageView);
		AnchorPane rootPane = new AnchorPane(imageView);
		rootPane.setStyle("-fx-background-color: black;");
		AnchorPane.setLeftAnchor(imageView, 0d);
		AnchorPane.setTopAnchor(imageView, 0d);
		AnchorPane.setRightAnchor(imageView, 0d);
		AnchorPane.setBottomAnchor(imageView, 0d);
		scene.setRoot(rootPane);

		scene.setOnMouseClicked(new MouseClickEventHandler(popup));
		MouseDragOverEventHandler dragOverEventHandler = new MouseDragOverEventHandler(popup);
		imageView.setOnMouseDragOver(dragOverEventHandler);
		imageView.setOnDragDetected(new DragDetectedEventHandler(popup, imageView, dragOverEventHandler));
		imageView.setOnScroll(new MouseScrollEventHandler(imageView));
		// 端で止まらないようにする
		popup.setAutoFix(false);
		popup.setAutoHide(true);

		popup.show(owner);
		return popup;
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
		private Node image;

		public MouseScrollEventHandler(Node image) {
			this.image = image;
		}

		@Override
		public void handle(ScrollEvent event) {
			double scrollY = event.getDeltaY();
			if (scrollY < 0d) {
				image.setScaleX(image.getScaleX() * 0.5d);
				image.setScaleY(image.getScaleY() * 0.5d);
			} else if (scrollY > 0d) {
				image.setScaleX(image.getScaleX() * 2d);
				image.setScaleY(image.getScaleY() * 2d);
			}
			event.consume();
		}
		
	}

	private ImageViewProcessor() {}
}
