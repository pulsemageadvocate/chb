package pulad.chb;

import java.io.File;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import pulad.chb.config.Config;
import pulad.chb.dto.SearchConditionDto;

/**
 * 検索タブを開く処理。
 * @author pulad
 *
 */
public class SearchViewProcessor {

	/**
	 * 検索タブを開く。
	 * @param tab
	 */
	public static void open(Tab tab) {
		GridPane gridpane = new GridPane();
		gridpane.getStyleClass().add("searchView_gridpane");
		ColumnConstraints cc = new ColumnConstraints();
		cc.setHgrow(Priority.ALWAYS);
		gridpane.getColumnConstraints().addAll(new ColumnConstraints(), cc, new ColumnConstraints(), new ColumnConstraints());

		gridpane.add(new Label("検索"), 0, 0);
		TextField textField = new TextField();
		gridpane.add(textField, 1, 0);
		CheckBox textAaCheck = new CheckBox("A/a");
		textAaCheck.setSelected(true);
		gridpane.add(textAaCheck, 2, 0);
		CheckBox textReCheck = new CheckBox("Re");
		gridpane.add(textReCheck, 3, 0);

		gridpane.add(new Label("対象"), 0, 1);
		TextField directoryField = new TextField();
		directoryField.setEditable(false);
		directoryField.setText(Config.getLogFolder().toFile().getAbsolutePath());
		directoryField.setOnAction(event -> {
			openDirectoryDialog(tab, directoryField);
		});
		directoryField.setOnMouseClicked(event -> {
			openDirectoryDialog(tab, directoryField);
		});
		gridpane.add(directoryField, 1, 1);

		gridpane.add(new Label("タイトル"), 0, 2);
		TextField titleField = new TextField();
		gridpane.add(titleField, 1, 2);
		CheckBox titleAaCheck = new CheckBox("A/a");
		titleAaCheck.setSelected(true);
		gridpane.add(titleAaCheck, 2, 2);
		CheckBox titleReCheck = new CheckBox("Re");
		gridpane.add(titleReCheck, 3, 2);

		Button searchButton = new Button("検索");
		searchButton.setOnAction(event -> {
			SearchConditionDto dto = new SearchConditionDto();
			dto.setText(textField.getText());
			dto.setTextAa(textAaCheck.isSelected());
			dto.setTextRe(textReCheck.isSelected());
			dto.setDirectory(directoryField.getText());
			dto.setTitle(titleField.getText());
			dto.setTitleAa(titleAaCheck.isSelected());
			dto.setTitleRe(titleReCheck.isSelected());
			App.getInstance().openSearchResult(dto);
		});
		gridpane.add(searchButton, 1, 3);

		tab.setContent(gridpane);
	}

	private static void openDirectoryDialog(Tab tab, TextField directoryField) {
		DirectoryChooser dc = new DirectoryChooser();
		dc.setInitialDirectory(Config.getLogFolder().toFile());
		dc.setTitle("検索対象");
		File f = dc.showDialog(tab.getTabPane().getScene().getWindow());
		if (f == null) {
			return;
		}
		directoryField.setText(f.getAbsolutePath());
	}
}
