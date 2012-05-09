package ch.luklanis.esscanlite.history;

import android.os.AsyncTask;

public final class HistoryExportUpdateAsyncTask extends
AsyncTask<HistoryItem, Void, Void> {

	private final HistoryManager historyManager;
	private final String fileName;

	public HistoryExportUpdateAsyncTask(HistoryManager historyManager, String fileName) {
		this.historyManager = historyManager;
		this.fileName = fileName;
	}

	@Override
	protected Void doInBackground(HistoryItem... params) {
		if(params.length > 0){
			for (int i = 0; i < params.length; i++) {
				HistoryItem item = params[i];

				if(item.getExported()){
					historyManager.updateHistoryItemFileName(item.getResult().getCompleteCode(), fileName);
				}
			}
		}

		return null;
	}
}
