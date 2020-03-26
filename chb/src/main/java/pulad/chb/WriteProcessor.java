package pulad.chb;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import pulad.chb.bbs.BBSManager;
import pulad.chb.interfaces.BBS;

public class WriteProcessor {

	public static void write(String url, String name, String mail, String body) {
		WriteService service = new WriteService(url, name, mail, body);
		service.setOnSucceeded(new WriteSucceededEventHandler());
		service.start();
	}

	private static class WriteService extends Service<Boolean> {
		private String url;
		private String name;
		private String mail;
		private String body;

		public WriteService(String url, String name, String mail, String body) {
			this.url = url;
			this.name = name;
			this.mail = mail;
			this.body = body;
		}

		@Override
		protected Task<Boolean> createTask() {
			BBS bbsObject = BBSManager.getBBSFromUrl(url);
			return bbsObject.createWriteTask(url, name, mail, body);
		}
	}

	private static class WriteSucceededEventHandler implements EventHandler<WorkerStateEvent> {

		@Override
		public void handle(WorkerStateEvent event) {
			// TODO Auto-generated method stub
			
		}
	}
}
