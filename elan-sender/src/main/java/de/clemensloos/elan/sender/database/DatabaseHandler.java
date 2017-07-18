package de.clemensloos.elan.sender.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Loos on 11.06.2016.
 */
public class DatabaseHandler extends SQLiteOpenHelper {


    public static final String DATABASE_NAME = "elanSender.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_SONGS = "table_songs";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_ARTIST = "artist";


    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSongsTable(db);
    }

    private void createSongsTable(SQLiteDatabase db) {

        String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_TITLE
                + " TEXT," + COLUMN_ARTIST + " TEXT" + ")";
        db.execSQL(CREATE_SONGS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion,
                          int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        onCreate(db);
    }


    public void clearSongs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        createSongsTable(db);
        db.close();
    }

    public void addSongs(List<Song> songList) {
        for (Song song : songList) {
            addSong(song);
        }
    }

    public void addSong(Song song) {

        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, song.getNumber());
        values.put(COLUMN_TITLE, song.getTitle());
        values.put(COLUMN_ARTIST, song.getArtist());

        SQLiteDatabase db = this.getWritableDatabase();

        db.insert(TABLE_SONGS, null, values);
        db.close();
    }

    public Song getSongById(int id) {

        String query = "Select * FROM " + TABLE_SONGS + " WHERE " + COLUMN_ID + " =  \"" + id + "\"";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        String title = "", artist = "";
        if (cursor.moveToFirst()) {
            title = cursor.getString(1);
            artist = cursor.getString(2);
        }
        cursor.close();
        db.close();
        return new Song(id, title, artist);
    }

    public List<Song> getAllSongs() {

        List<Song> all = new LinkedList<Song>();
        String query = "Select * FROM " + TABLE_SONGS;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        while (cursor.moveToNext()) {
            all.add(new Song(cursor.getInt(0), cursor.getString(1), cursor.getString(2)));
        }
        cursor.close();
        db.close();

        return all;
    }



}
