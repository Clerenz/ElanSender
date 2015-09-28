package de.clemensloos.elan.sender;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by Clemens.Loos on 28.09.2015.
 */
public class ImportListActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_list);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {

            Uri uri2 = intent.getData();
            String uri = uri2.getEncodedPath() + "  complete: " + uri2.toString();
            TextView textView = (TextView)findViewById(R.id.text_imported);
            textView.setText(uri);

        }


    }
}