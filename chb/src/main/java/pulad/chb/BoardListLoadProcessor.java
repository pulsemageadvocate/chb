package pulad.chb;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import pulad.chb.bbs.BBSManager;
import pulad.chb.interfaces.BBS;
import pulad.chb.read.thread.BoardListLoadTask;

/**
 * 板一覧を更新する処理。
 * 5chのみ対応。
 * @author pulad
 *
 */
public class BoardListLoadProcessor {

	public static void request(Tab tab, App app) {
		BoardListLoadService service = new BoardListLoadService();
		service.setOnSucceeded(new BoardListLoadSucceededEventHandler(service, tab, app));
		service.start();
	}

	private static class BoardListLoadService extends Service<Boolean> {

		@Override
		protected Task<Boolean> createTask() {
			BBS bbsObject = BBSManager.getBBSFromLogDirectoryName("2ch_");
			if (bbsObject == null) {
				return null;
			}
			Task<Boolean> task = new BoardListLoadTask(bbsObject);
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent event) {
				}
			});
			return task;
		}
	}

	private static class BoardListLoadSucceededEventHandler implements EventHandler<WorkerStateEvent> {
		private BoardListLoadService service;
		private Tab tab;
		private App app;

		public BoardListLoadSucceededEventHandler(BoardListLoadService service, Tab tab, App app) {
			this.service = service;
			this.tab = tab;
			this.app = app;
		}

		@Override
		public void handle(WorkerStateEvent event) {
			if (service.getValue().booleanValue()) {
				BBSMenuTreeProcessor.reload(tab, app);
			}
		}
	}
}
