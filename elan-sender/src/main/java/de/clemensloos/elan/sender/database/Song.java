package de.clemensloos.elan.sender.database;

/**
 * Created by Loos on 10.06.2016.
 */
public class Song {

    private int _number;
    private String _title;
    private String _artist;

    public Song(int number, String title, String artist) {
        this._number = number;
        this._title = title;
        this._artist = artist;
    }

    public void setNumber(int number) {
        this._number = number;
    }

    public int getNumber() {
        return _number;
    }

    public void setTitle(String title) {
        this._title = title;
    }

    public String getTitle() {
        return _title;
    }

    public void setArtist(String artist) {
        this._artist = artist;
    }

    public String getArtist() {
        return _artist;
    }

}
