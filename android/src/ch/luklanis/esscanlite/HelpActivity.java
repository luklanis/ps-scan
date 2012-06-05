/*
 * Copyright 2008 ZXing authors
 * Copyright 2011 Robert Theis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.luklanis.esscanlite;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import ch.luklanis.esscanlite.R;

import com.actionbarsherlock.app.SherlockActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * Activity to display informational pages to the user in a WebView.
 * 
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
public final class HelpActivity extends SherlockActivity {

	private static final String TAG = HelpActivity.class.getSimpleName();

	// Use this key and one of the values below when launching this activity via intent. If not
	// present, the default page will be loaded.
	public static final String REQUESTED_PAGE_KEY = "requested_page_key";
	public static final String DEFAULT_PAGE = "index.html";
	public static final String ABOUT_PAGE = "about.html";
	public static final String WHATS_NEW_PAGE = "whatsnew.html";

	private static final String WEBVIEW_STATE_PRESENT = "webview_state_present";

	private static final String DEFAULT_LANGUAGE = "en";

	private static final String LANGUAGE;
	static {
		Locale locale = Locale.getDefault();
		String language = locale == null ? DEFAULT_LANGUAGE : locale.getLanguage();
		LANGUAGE = language;
	}

	private static final Collection<String> TRANSLATED_HELP_ASSET_LANGUAGES =
			Arrays.asList("en", "de");

	private static final String BASE_URL = "file:///android_asset/html";

	private static final String BASE_HELP_URL = BASE_URL + "-"
			+ (TRANSLATED_HELP_ASSET_LANGUAGES.contains(LANGUAGE) ? LANGUAGE : DEFAULT_LANGUAGE) +"/";

//	private static final String BASE_HELP_URL = BASE_URL + "-de/";

	private WebView webView;

	private final Button.OnClickListener backListener = new Button.OnClickListener() {
		@Override
		public void onClick(View view) {
			webView.goBack();
		}
	};

	private final Button.OnClickListener doneListener = new Button.OnClickListener() {
		@Override
		public void onClick(View view) {
			finish();
		}
	};

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.help);
		
		// Hide Icon in ActionBar
		getSupportActionBar().setDisplayShowHomeEnabled(false);

		webView = (WebView)findViewById(R.id.help_contents);
		webView.setWebViewClient(new HelpClient((Activity)this));

		Intent intent = getIntent();

		// Show an OK button.
		View doneButton = findViewById(R.id.done_button);
		doneButton.setOnClickListener(doneListener);

		// Show an BACK button.
		View backButton = findViewById(R.id.back_button);
		backButton.setOnClickListener(backListener);

		// Froyo has a bug with calling onCreate() twice in a row, which causes the What's New page
		// that's auto-loaded on first run to appear blank. As a workaround we only call restoreState()
		// if a valid URL was loaded at the time the previous activity was torn down.
		if (icicle != null && icicle.getBoolean(WEBVIEW_STATE_PRESENT, false)) {
			webView.restoreState(icicle);
		} else if (intent != null){
			String page = intent.getStringExtra(REQUESTED_PAGE_KEY);

			if( page != null && page.length() > 0) {
				
				if(page.equals(DEFAULT_PAGE) || page.equals(ABOUT_PAGE)) {
					webView.loadUrl(BASE_HELP_URL + page);
				} else {
					webView.loadUrl(BASE_URL + "/" + page);
				}
				
				if(page.equals(WHATS_NEW_PAGE) || page.equals(ABOUT_PAGE) || page.equals(DEFAULT_PAGE)) {
//					backButton.setEnabled(false);
					backButton.setVisibility(View.GONE);
				}
				
			}else {
				webView.loadUrl(BASE_HELP_URL + DEFAULT_PAGE);
			}
		}else {
			webView.loadUrl(BASE_HELP_URL + DEFAULT_PAGE); 
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		String url = webView.getUrl();
		if (url != null && url.length() > 0) {
			webView.saveState(state);
			state.putBoolean(WEBVIEW_STATE_PRESENT, true);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView.canGoBack()) {
				webView.goBack();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private final class HelpClient extends WebViewClient {
		Activity context;
		public HelpClient(Activity context){
			this.context = context;
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			setTitle(view.getTitle());
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("file")) {
				// Keep local assets in this WebView.
				return false;
			} else if (url.startsWith("mailto:")) {
				try {
					MailTo mt = MailTo.parse(url);
					Intent i = new Intent(Intent.ACTION_SEND);
					i.setType("message/rfc822");
					i.putExtra(Intent.EXTRA_EMAIL, new String[]{mt.getTo()});
					i.putExtra(Intent.EXTRA_SUBJECT, mt.getSubject());
					context.startActivity(i);
					view.reload();
				}
				catch (ActivityNotFoundException e) {
					Log.w(TAG, "Problem with Intent.ACTION_SEND", e);
					new AlertDialog.Builder(context)
					.setTitle("Contact Info")
					.setMessage( "Please send your feedback to: app.ocr@gmail.com" )
					.setPositiveButton( "Done", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Log.d("AlertDialog", "Positive");
						}
					})
					.show();
				}
				return true;
			} else {
				// Open external URLs in Browser.
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				return true;
			}
		}
	}
}