package ch.luklanis.esscan.history;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ch.luklanis.esscanlite.R;
import com.actionbarsherlock.view.Menu;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class HistoryFragment extends ListFragment {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private HistoryCallbacks historyCallbacks;
	private int activatedPosition = ListView.INVALID_POSITION;

	public interface HistoryCallbacks {
		public void selectTopInTwoPane();
		public void onItemSelected(int oldPosition, int newPosition);
		public int getPositionToActivate();
		public void setOptionalOkAlert(int id);
	}

	private HistoryManager historyManager;
	private HistoryItemAdapter adapter;

	private HistoryFragment self;

	private boolean listIsEmpty;

	private static HistoryCallbacks sDummyCallbacks = new HistoryCallbacks(){
		@Override
		public void onItemSelected(int oldPosition, int newPosition) {
		}
		@Override
		public void setOptionalOkAlert(int id) {
		}
		@Override
		public int getPositionToActivate() {
			return -1;
		}
		@Override
		public void selectTopInTwoPane() {
		}
	};

	protected static final Comparator<HistoryItem> historyComparator = new Comparator<HistoryItem>() {

		@Override
		public int compare(HistoryItem lhs, HistoryItem rhs) {
			if (rhs.getResult() == null) {
				if (lhs.getResult() == null) {
					return 0;
				}

				return 1;
			}

			if (lhs.getResult() == null) {
				return -1;
			}

			return (Long.valueOf(rhs.getResult().getTimestamp()))
					.compareTo(lhs.getResult().getTimestamp());
		}
	};

	public HistoryFragment() {
		listIsEmpty = true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		historyManager = new HistoryManager(getActivity());
		self = this;

		adapter = null;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {

		if (listIsEmpty) {
			return;
		}

		super.onListItemClick(listView, view, position, id);

		int oldPosition = activatedPosition;
		activatedPosition = position;

		historyCallbacks.onItemSelected(oldPosition, position);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		if (savedInstanceState != null && savedInstanceState
				.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
		}

		ListView listview = getListView();
		registerForContextMenu(listview);
		listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view,
					int position, long id) {

				activatedPosition = position;

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.msg_confirm_delete_title)
				.setMessage(R.string.msg_confirm_delete_message)
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						historyManager.deleteHistoryItem(activatedPosition);
						historyCallbacks.selectTopInTwoPane();

						GetHistoryAsyncTask async = new GetHistoryAsyncTask(self, historyManager);
						async.execute();
					}
				})
				.setNegativeButton(R.string.button_cancel, null);

				builder.show();
				return true;
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		GetHistoryAsyncTask async = new GetHistoryAsyncTask(this, historyManager);
		async.execute();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof HistoryCallbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		historyCallbacks = (HistoryCallbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		historyCallbacks = sDummyCallbacks ;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (activatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
		}
	}

	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(activateOnItemClick
				? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	public void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(activatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		activatedPosition = position;
	}

	public void updatePosition(int position, HistoryItem item) {

		if (adapter != null && adapter.getCount() > position) {
			HistoryItem listItem = adapter.getItem(position);
			listItem.update(item);
		}

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}

	public void setHistoryItemAdapter(HistoryItemAdapter adapter) {

		if (adapter.isEmpty()) {
			adapter.add(new HistoryItem(null));
			listIsEmpty = true;
		} else {
			listIsEmpty = false;
		}

		this.adapter = adapter;
		setListAdapter(this.adapter);
		setListShown(true);

		this.adapter.notifyDataSetInvalidated();

		int position = historyCallbacks.getPositionToActivate();
		setActivatedPosition(position);
	}

	public HistoryItemAdapter getHistoryItemAdapter() {
		return adapter;
	}

	public int getActivatedPosition() {
		return activatedPosition;
	}
}
