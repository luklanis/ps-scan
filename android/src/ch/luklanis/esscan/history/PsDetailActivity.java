package ch.luklanis.esscan.history;

import ch.luklanis.esscan.R;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;

public class PsDetailActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
        setContentView(R.layout.activity_ps_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (icicle == null) {
            Bundle arguments = new Bundle();
            arguments.putInt(PsDetailFragment.ARG_POSITION,
                    getIntent().getIntExtra(PsDetailFragment.ARG_POSITION, 0));
            PsDetailFragment fragment = new PsDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.ps_detail_container, fragment)
                    .commit();
        }
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!savePaymentSlip(this)) {
            	return true;
            }
            
            NavUtils.navigateUpTo(this, new Intent(this, HistoryActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!savePaymentSlip(this)) {
            	return true;
            }
		}
		
		return super.onKeyDown(keyCode, event);
	}
	
	public static boolean savePaymentSlip(SherlockFragmentActivity activity) {
		PsDetailFragment oldFragment = (PsDetailFragment)activity.getSupportFragmentManager()
				.findFragmentById(R.id.ps_detail_container);
		
		if (oldFragment != null && oldFragment.save()) {
			return true;
		}
		
		return false;
	}
}
