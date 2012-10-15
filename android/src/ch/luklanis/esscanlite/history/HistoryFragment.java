package ch.luklanis.esscanlite.history;

import java.util.Comparator;
import java.util.List;

import ch.luklanis.esscanlite.R;
import com.actionbarsherlock.view.Menu;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;

public class HistoryFragment extends ListFragment {

	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private HistoryCallbacks historyCallbacks;
	private int mActivatedPosition = ListView.INVALID_POSITION;

	public interface HistoryCallbacks {

		public void onItemSelected(int oldPosition, int newPosition);
		public void setOptionalOkAlert(int id);
	}

	private HistoryManager historyManager;
	private HistoryItemAdapter adapter;

	private static HistoryCallbacks sDummyCallbacks = new HistoryCallbacks(){
		@Override
		public void onItemSelected(int oldPosition, int newPosition) {
		}
		@Override
		public void setOptionalOkAlert(int id) {
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

			return (new Long(rhs.getResult().getTimestamp())).compareTo(lhs.getResult().getTimestamp());
		}
	};

	public HistoryFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		historyManager = new HistoryManager(getActivity());

		adapter = null;

		loadAdapterFromDatabase();
		
		setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);
		
		int oldPosition = mActivatedPosition;
		mActivatedPosition = position;

		historyCallbacks.onItemSelected(oldPosition, position);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu,
			View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		menu.add(Menu.NONE, position, position, R.string.history_clear_one_history_text);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		int position = item.getItemId();
		historyManager.deleteHistoryItem(position);
		loadAdapterFromDatabase();
		return true;
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
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	public void setActivateOnItemClick(boolean activateOnItemClick) {
		getListView().setChoiceMode(activateOnItemClick
				? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

	public void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}

	public void showPaymentSlipDetail() {
		loadAdapterFromDatabase();

		setActivatedPosition(0);
		historyCallbacks.onItemSelected(ListView.INVALID_POSITION, 0);
	}

	public void updatePosition(int position, HistoryItem item) {
		if (adapter != null && adapter.getCount() > position) {
			adapter.remove(adapter.getItem(position));
			adapter.insert(item, position);
			adapter.notifyDataSetChanged();
		}
	}

	public void loadAdapterFromDatabase() {  

		List<HistoryItem> items = historyManager.buildAllHistoryItems();

		if (adapter == null) {
			adapter = new HistoryItemAdapter(getActivity());
		}

		adapter.clear();

		for (HistoryItem item : items) {
			adapter.add(item);
		}
		
		if (adapter.isEmpty()) {
			adapter.add(new HistoryItem(null));
		}

		adapter.notifyDataSetChanged();
	}

	public HistoryItemAdapter getAdapter() {
		return adapter;
	}
}
