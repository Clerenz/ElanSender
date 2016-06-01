package de.clemensloos.elan.sender;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;


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

            Uri uri = intent.getData();

            try{
                FileInputStream file = new FileInputStream(new File(uri.getPath()));
                Iterator<Row> rowIterator;

                HSSFWorkbook workbook = new HSSFWorkbook(file);
                HSSFSheet sheet = workbook.getSheetAt(0);
                rowIterator = sheet.iterator();


                LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                TableLayout table = (TableLayout) findViewById(R.id.song_table);
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    double nr = row.getCell(0).getNumericCellValue();
                    String title = row.getCell(1).getStringCellValue();
                    String inter = row.getCell(2).getStringCellValue();
                    TableRow rowView = (TableRow)inflater.inflate(R.layout.import_table_row, null);
                    ((TextView)rowView.findViewById(R.id.number)).setText("" + nr);
                    ((TextView)rowView.findViewById(R.id.title)).setText(title);
                    ((TextView)rowView.findViewById(R.id.interpret)).setText(inter);
                    table.addView(rowView);
                }
                //table.addView(rowView);


            }
            catch(IOException e) {

            }



        }


    }
}