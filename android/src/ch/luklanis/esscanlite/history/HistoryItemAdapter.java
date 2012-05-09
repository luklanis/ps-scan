/*
 * Copyright 2012 ZXing authors
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

package ch.luklanis.esscanlite.history;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import ch.luklanis.esscanlite.R;
import ch.luklanis.esscan.paymentslip.EsrResult;

import java.util.ArrayList;

final class HistoryItemAdapter extends ArrayAdapter<HistoryItem> {

  private final Activity activity;

  HistoryItemAdapter(Activity activity) {
    super(activity, R.layout.history_list_item, new ArrayList<HistoryItem>());
    this.activity = activity;
  }

  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    LinearLayout layout;
    if (view instanceof LinearLayout) {
      layout = (LinearLayout) view;
    } else {
      LayoutInflater factory = LayoutInflater.from(activity);
      layout = (LinearLayout) factory.inflate(R.layout.history_list_item, viewGroup, false);
    }

    HistoryItem item = getItem(position);
    EsrResult result = item.getResult();

    String title;
    String address;
    if (result != null) {
      title = result.toString() + " " + item.getAmount();
      // TODO get address
      address = item.getAddress().replaceAll("[\\r\\n]+", ", ");      
    } else {
      Resources resources = getContext().getResources();
      title = resources.getString(R.string.history_empty);
      address = resources.getString(R.string.history_empty_detail);
    }

    ((TextView) layout.findViewById(R.id.history_title)).setText(title);    
    ((TextView) layout.findViewById(R.id.history_detail)).setText(address);

    return layout;
  }

}
