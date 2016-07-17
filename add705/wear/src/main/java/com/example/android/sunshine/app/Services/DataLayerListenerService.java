package com.example.android.sunshine.app.Services;

import android.content.Intent;
import android.util.Log;

import com.example.android.sunshine.app.R;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by User on 17/7/2016.
 */
public class DataLayerListenerService extends WearableListenerService {
    private String TAG = "DataLayerListenerService";
    private String KEY = "key";
    private String TEXT_KEY = "showText_key";

    public DataLayerListenerService() {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);

        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/num") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String receivedString = dataMap.getString(KEY);
                    Log.d(TAG, receivedString);
//                    updateTV(dataMap.getString(KEY));
                    Intent intent = new Intent();
                    intent.putExtra(getString(R.string.BRODCAST_INTENT_KEY), receivedString);
                    intent.setAction(getString(R.string.Text_RECEIVER_ACTION));
                    sendBroadcast(intent);
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }

        }

    }
}