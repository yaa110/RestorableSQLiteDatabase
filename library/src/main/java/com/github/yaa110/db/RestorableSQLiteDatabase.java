package com.github.yaa110.db;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Hashtable;
import java.util.Set;

/**
 * A wrapper to replicate the `SQLiteDatabase` class to manage a SQLite database with restoring capability.
 * This wrapper makes it possible to undo changes made after execution of SQL commands.
 */
@SuppressWarnings("UnusedDeclaration")
public class RestorableSQLiteDatabase {

    private static RestorableSQLiteDatabase mInstance = null;
    private SQLiteDatabase mSQLiteDatabase;
    private static final String TAG = "SQLiteDatabase";

    /**
     * The hash table to map a tag to its restoring query
     */
    public Hashtable<String, String> mTagQueryTable;

    public static RestorableSQLiteDatabase getInstance(SQLiteDatabase mSQLiteDatabase){
        if(mInstance == null) {
            mInstance = new RestorableSQLiteDatabase(mSQLiteDatabase);
        }
        return mInstance;
    }

    /**
     * Private constructor of singleton pattern
     * @param mSQLiteDatabase the instance of the SQLiteDatabase to be wrapped
     */
    private RestorableSQLiteDatabase(SQLiteDatabase mSQLiteDatabase) {
        mTagQueryTable = new Hashtable<>();
        this.mSQLiteDatabase = mSQLiteDatabase;
    }

    /**
     * Provides the instance of wrapped SQLiteDatabase
     * @return the instance of wrapped SQLiteDatabase
     */
    public SQLiteDatabase getSQLiteDatabase() {
        return mSQLiteDatabase;
    }

    /**
     * Checks if the hash table contains the tag
     * @param tag Possible tag of restoring query
     * @return true if the hash table contains the tag; false otherwise
     */
    public boolean containsTag(String tag) {
        return mTagQueryTable.containsKey(tag);
    }

    /**
     * Provides a Set view of the tags contained in the hash table
     * @return a Set view of the tags contained in the hash table
     */
    public Set<String> tagSet() {
        return mTagQueryTable.keySet();
    }

    /**
     * Provides the query to which the tag is mapped
     * @param tag Possible tag of restoring query
     * @return the query to which the tag is mapped, or null if the hash table contains no mapping for the tag
     */
    public String getQuery(String tag) {
        return mTagQueryTable.get(tag);
    }

    /**
     * Returns the hash table
     * @return the hash table
     */
    public Hashtable<String, String> getTagQueryTable() {
        return mTagQueryTable;
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#insert(String, String, android.content.ContentValues) insert} method.
     */
    public long insert(String table, String nullColumnHack, ContentValues values) {
        try {
            return insertWithOnConflict(table, nullColumnHack, values, SQLiteDatabase.CONFLICT_NONE);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + values, e);
            return -1;
        }
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#insertOrThrow(String, String, android.content.ContentValues) insertOrThrow} method.
     */
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values)
            throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, values, SQLiteDatabase.CONFLICT_NONE);
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replace(String, String, android.content.ContentValues) replace} method.
     */
    public long replace(String table, String nullColumnHack, ContentValues initialValues) {
        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + initialValues, e);
            return -1;
        }
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replaceOrThrow(String, String, android.content.ContentValues) replaceOrThrow} method.
     */
    public long replaceOrThrow(String table, String nullColumnHack,
                               ContentValues initialValues) throws SQLException {
        return insertWithOnConflict(table, nullColumnHack, initialValues,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replaceOrThrow(String, String, android.content.ContentValues) insertWithOnConflict} method.
     */
    public long insertWithOnConflict(String table, String nullColumnHack,
                                     ContentValues initialValues, int conflictAlgorithm) {
        return mSQLiteDatabase.insertWithOnConflict(
                table,
                nullColumnHack,
                initialValues,
                conflictAlgorithm
        );
    }

}
