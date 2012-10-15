package ch.luklanis.esscanlite.history;

import java.util.ArrayList;
import java.util.List;

import ch.luklanis.esscanlite.R;
import ch.luklanis.esscan.paymentslip.DTAFileCreator;
import ch.luklanis.esscan.paymentslip.EsrResult;
import ch.luklanis.esscan.paymentslip.PsResult;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PsDetailFragment extends Fragment {

	public static final String ARG_POSITION = "history_position";

	protected static final String TAG = PsDetailFragment.class.getSimpleName();

	private HistoryManager historyManager;

	private HistoryItem historyItem;

	private final Button.OnClickListener addressChangeListener = new Button.OnClickListener() {
		@Override
		public void onClick(View view) {
			showAddressDialog(view);
		}
	};

	private final Button.OnClickListener resultCopyListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			ClipboardManager clipboardManager = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
			clipboardManager.setText(historyItem.getResult().getCompleteCode());

			//        clipboardManager.setPrimaryClip(ClipData.newPlainText("ocrResult", ocrResultView.getText()));
			//      if (clipboardManager.hasPrimaryClip()) {
			if (clipboardManager.hasText()) {
				Toast toast = Toast.makeText(v.getContext(), R.string.msg_copied, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.BOTTOM, 0, 0);
				toast.show();
			}
		}
	};

	private final Button.OnClickListener exportAgainListener = new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
			builder.setMessage(R.string.msg_sure);
			builder.setNeutralButton(R.string.button_cancel, null);
			builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					historyManager.updateHistoryItemFileName(historyItem.getResult().getCompleteCode(), null);

					ImageButton reexportButton = (ImageButton) getView().findViewById(R.id.button_export_again);

					TextView dtaFilenameTextView = (TextView) getView().findViewById(R.id.result_dta_file);
					dtaFilenameTextView.setText("");
					reexportButton.setVisibility(View.GONE);
				}
			});
			builder.show();
		}
	};

	public PsDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		historyManager = new HistoryManager(getActivity());

		if (getArguments().containsKey(ARG_POSITION)) {
			historyItem = historyManager.buildHistoryItem(getArguments().getInt(ARG_POSITION));
		} else {
			historyItem = null;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.fragment_ps_detail, container, false);

		if (historyItem == null) {
			return rootView;
		}

		PsResult psResult = historyItem.getResult();

		ImageView bitmapImageView = (ImageView) rootView.findViewById(R.id.image_view);
		bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
				R.drawable.ez_or));

		TextView accountTextView = (TextView) rootView.findViewById(R.id.result_account);
		accountTextView.setText(psResult.getAccount());

		if (psResult instanceof EsrResult) {
			EsrResult result = (EsrResult)psResult;

			EditText amountEditText = (EditText) rootView.findViewById(R.id.result_amount_edit);
			TextView amountTextView = (TextView) rootView.findViewById(R.id.result_amount);

			String amountFromCode = result.getAmount(); 
			if(amountFromCode != ""){
				amountEditText.setVisibility(View.GONE);
				amountTextView.setVisibility(View.VISIBLE);
				amountTextView.setText(amountFromCode);
			}
			else{
				amountTextView.setVisibility(View.GONE);
				amountEditText.setVisibility(View.VISIBLE);
				String amountManuel = historyItem.getAmount();
				if(amountManuel == null || amountManuel == "" || amountManuel.length() == 0){
					amountEditText.setText(R.string.result_amount_not_set);
					amountEditText.selectAll();
				}
				else{
					amountEditText.setText(amountManuel);
				}
			}

			TextView currencyTextView = (TextView) rootView.findViewById(R.id.result_currency);
			currencyTextView.setText(result.getCurrency());

			TextView referenceTextView = (TextView) rootView.findViewById(R.id.result_reference_number);
			referenceTextView.setText(result.getReference());
		}

		String dtaFilename = historyItem.getDTAFilename();

		ImageButton exportAgainButton = (ImageButton)rootView.findViewById(R.id.button_export_again);
		exportAgainButton.setOnClickListener(exportAgainListener);

		TextView dtaFilenameTextView = (TextView) rootView.findViewById(R.id.result_dta_file);
		TextView dtaFilenameTextTextView = (TextView) rootView.findViewById(R.id.result_dta_file_text);

		if(dtaFilename != null && dtaFilename != ""){
			dtaFilenameTextView.setText(historyItem.getDTAFilename());
			dtaFilenameTextTextView.setVisibility(View.VISIBLE);
			exportAgainButton.setVisibility(View.VISIBLE);
		}
		else{
			dtaFilenameTextTextView.setVisibility(View.GONE);
			exportAgainButton.setVisibility(View.GONE);
			dtaFilenameTextView.setText("");
		}

		ImageButton addressChangeButton = (ImageButton)rootView.findViewById(R.id.button_address_change);
		addressChangeButton.setOnClickListener(addressChangeListener);
		addressChangeButton.setVisibility(View.VISIBLE);

		EditText addressEditText = (EditText) rootView.findViewById(R.id.result_address);
		addressEditText.setText("");

		if(historyItem.getAddressNumber() != -1){
			addressEditText.setText(historyItem.getAddress());
		}
		else{
			showAddressDialog(rootView);
		}

		return rootView;
	}

	public int save() {

		String codeRow = historyItem.getResult().getCompleteCode();

		if(PsResult.getCoderowType(codeRow).equals(EsrResult.PS_TYPE_NAME) 
				&& TextUtils.isEmpty((new EsrResult(codeRow)).getAmount())) {
			EditText amountEditText = (EditText) getView().findViewById(R.id.result_amount_edit);
			String newAmount = amountEditText.getText().toString().replace(',', '.');
			try {
				float newAmountTemp = Float.parseFloat(newAmount);
				newAmountTemp *= 100;
				newAmountTemp -= (newAmountTemp % 5);
				newAmountTemp /= 100;

				newAmount = String.valueOf(newAmountTemp);

				if(newAmount.indexOf('.') == newAmount.length() - 2){
					newAmount += "0";
				}

				if(historyManager == null){
					Log.e(TAG, "onClick: historyManager is null!");
					return 0;
				}

				historyManager.updateHistoryItemAmount(historyItem.getResult().getCompleteCode(), 
						newAmount);
				amountEditText.setText(newAmount);

			} catch (NumberFormatException e) {
				return R.string.msg_amount_not_valid;
			}
		}

		int status = 0;
		EditText addressEditText = (EditText) getView().findViewById(R.id.result_address);
		String address = addressEditText.getText().toString();
		if(address.length() > 0){

			status = DTAFileCreator.validateAddress(address);

			if (historyItem.getAddressNumber() == -1) {
				historyManager.updateHistoryItemAddress(historyItem.getResult().getCompleteCode(), 
						historyManager.addAddress(historyItem.getResult().getAccount(), address));
			}
			else{
				historyManager.updateAddress(historyItem.getResult().getAccount(), 
						historyItem.getAddressNumber(), address);
			}
		}

		return status;
	}

	private void showAddressDialog(View view) {
		PsResult result = historyItem.getResult();
		List<String> addresses = new ArrayList<String>();
		addresses.addAll(historyManager.getAddresses(result.getAccount()));
		addresses.add(getResources().getString(R.string.address_new));

		if(addresses.size() <= 1){		
			ImageButton addressChangeButton = (ImageButton)view.findViewById(R.id.button_address_change);
			addressChangeButton.setVisibility(View.GONE);
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.address_dialog_title);
		builder.setItems(addresses.toArray(new String[addresses.size()]), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				EditText addressEditText = (EditText) getView().findViewById(R.id.result_address);
				String address = historyManager.getAddress(historyItem.getResult().getAccount(), which);

				if(address != ""){
					historyManager.updateHistoryItemAddress(historyItem.getResult().getCompleteCode(), which);

					historyItem.setAddress(address);
					historyItem.setAddressNumber(which);
					addressEditText.setText(historyItem.getAddress());

					Toast toast = Toast.makeText(getActivity(), R.string.msg_saved, Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM, 0, 0);
					toast.show();
				}
				else{
					historyItem.setAddress("");
					historyItem.setAddressNumber(-1);
					addressEditText.setText(historyItem.getAddress());
				}
			}
		});
		builder.setNeutralButton(R.string.button_cancel, null);
		builder.show();
	}
}
