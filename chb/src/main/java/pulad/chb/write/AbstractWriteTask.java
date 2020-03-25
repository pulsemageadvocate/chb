package pulad.chb.write;

import javafx.concurrent.Task;

public abstract class AbstractWriteTask extends Task<Boolean> {
	private String url;

	public AbstractWriteTask(String url) {
		this.url = url;
	}

	@Override
	protected Boolean call() {
		return Boolean.TRUE;
	}
}
