package de.clemensloos.elan.sender;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.clemensloos.elan.sender.database.DatabaseHandler;
import de.clemensloos.elan.sender.database.Song;


/**
 * Created by Clemens.Loos on 18.07.2017.
 */
public class ViewListActivity extends Activity {

    List<Song> songList;
    Button okay;
    Button cancel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        songList = null;

        setContentView(R.layout.import_list);
        okay = ((Button)findViewById(R.id.but_okay_import));
        okay.setEnabled(true);
        cancel = ((Button)findViewById(R.id.but_cancel_import));
        cancel.setText(R.string.label_delete);
        cancel.setEnabled(true);

        final DatabaseHandler dbh = new DatabaseHandler(ViewListActivity.this);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        TableLayout table = (TableLayout) findViewById(R.id.song_table);
        songList = new ArrayList<>();
        TableRow rowView = (TableRow)inflater.inflate(R.layout.import_table_row, null);
        ((TextView)rowView.findViewById(R.id.number)).setText("#");
        ((TextView)rowView.findViewById(R.id.title)).setText("Title");
        ((TextView)rowView.findViewById(R.id.interpret)).setText("Artist");
        table.addView(rowView);

        for (Song song : dbh.getAllSongs()) {
            rowView = (TableRow)inflater.inflate(R.layout.import_table_row, null);
            ((TextView)rowView.findViewById(R.id.number)).setText("" + song.getNumber());
            ((TextView)rowView.findViewById(R.id.title)).setText(song.getTitle());
            ((TextView)rowView.findViewById(R.id.interpret)).setText(song.getArtist());
            table.addView(rowView);
        }

        okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(ViewListActivity.this);
                builder.setMessage("Delete song list?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dbh.clearSongs();
                                finish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // nothing
                            }
                        }).show();
            }
        });





    }
}