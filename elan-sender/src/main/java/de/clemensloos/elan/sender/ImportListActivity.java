package de.clemensloos.elan.sender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.clemensloos.elan.sender.database.DatabaseHandler;
import de.clemensloos.elan.sender.database.Song;


/**
 * Created by Clemens.Loos on 28.09.2015.
 */
public class ImportListActivity extends Activity {

    List<Song> songList;
    Button okay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        songList = null;

        setContentView(R.layout.import_list);
        okay = ((Button)findViewById(R.id.but_okay_import));
        okay.setEnabled(false);

        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {

            Uri uri = intent.getData();

            try{
                FileInputStream file = new FileInputStream(new File(uri.getPath()));
                Iterator<Row> rowIterator;

                HSSFWorkbook workbook = new HSSFWorkbook(file);
                HSSFSheet sheet = workbook.getSheetAt(0);
                rowIterator = sheet.iterator();


                LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                TableLayout table = (TableLayout) findViewById(R.id.song_table);
                songList = new ArrayList<>();
                TableRow rowView = (TableRow)inflater.inflate(R.layout.import_table_row, null);
                ((TextView)rowView.findViewById(R.id.number)).setText("#");
                ((TextView)rowView.findViewById(R.id.title)).setText("Title");
                ((TextView)rowView.findViewById(R.id.interpret)).setText("Artist");
                table.addView(rowView);
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    if (row.getCell(0) == null || row.getCell(0).getCellType() != Cell.CELL_TYPE_NUMERIC) {
                        continue;
                    }
                    int nr = (int)Math.rint(row.getCell(0).getNumericCellValue());
                    String title = "";
                    if (row.getCell(1) != null && row.getCell(1).getCellType() == Cell.CELL_TYPE_STRING) {
                        title = row.getCell(1).getStringCellValue();
                    }
                    String artist = "";
                    if (row.getCell(2) != null && row.getCell(2).getCellType() == Cell.CELL_TYPE_STRING) {
                        artist = row.getCell(2).getStringCellValue();
                    }
                    songList.add(new Song(nr, title, artist));
                    rowView = (TableRow)inflater.inflate(R.layout.import_table_row, null);
                    ((TextView)rowView.findViewById(R.id.number)).setText("" + nr);
                    ((TextView)rowView.findViewById(R.id.title)).setText(title);
                    ((TextView)rowView.findViewById(R.id.interpret)).setText(artist);
                    table.addView(rowView);
                    okay.setEnabled(true);
                }
                //table.addView(rowView);

            }
            catch(IOException e) {
                finish();
            }

            ((Button)findViewById(R.id.but_cancel_import)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            okay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (songList != null && songList.size() > 0) {
                        DatabaseHandler dbh = new DatabaseHandler(ImportListActivity.this);
                        dbh.clearSongs();
                        dbh.addSongs(songList);
                    }
                    finish();
                }
            });


        }


    }
}