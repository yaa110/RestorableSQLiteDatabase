package com.github.yaa110.db;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.HashMap;
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
    private static final String ROWID = "rowid";

    /**
     * The hash table to map a tag to its restoring query.
     */
    public Hashtable<String, String> mTagQueryTable;

    /**
     * Maps a tag to the parameters should be used in the query string.
     */
    public HashMap<String, String[]> mTagQueryParameters;

    public static RestorableSQLiteDatabase getInstance(SQLiteDatabase mSQLiteDatabase){
        if(mInstance == null) {
            mInstance = new RestorableSQLiteDatabase(mSQLiteDatabase);
        }
        return mInstance;
    }

    /**
     * Private constructor of singleton pattern.
     * @param mSQLiteDatabase the instance of the SQLiteDatabase to be wrapped.
     */
    private RestorableSQLiteDatabase(SQLiteDatabase mSQLiteDatabase) {
        mTagQueryTable = new Hashtable<>();
        mTagQueryParameters = new HashMap<>();
        this.mSQLiteDatabase = mSQLiteDatabase;
    }

    /**
     * Provides the instance of wrapped SQLiteDatabase.
     * @return the instance of wrapped SQLiteDatabase.
     */
    public SQLiteDatabase getSQLiteDatabase() {
        return mSQLiteDatabase;
    }

    /**
     * Checks if the hash table contains the tag.
     * @param tag Possible tag of restoring query.
     * @return true if the hash table contains the tag; false otherwise.
     * @throws IllegalArgumentException if the tag is null.
     */
    public boolean containsTag(String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        return mTagQueryTable.containsKey(tag);
    }

    /**
     * Provides a Set view of the tags contained in the hash table.
     * @return a Set view of the tags contained in the hash table.
     */
    public Set<String> tagSet() {
        return mTagQueryTable.keySet();
    }

    /**
     * Provides the query to which the tag is mapped.
     * @param tag Possible tag of restoring query.
     * @throws IllegalArgumentException if the tag is null.
     * @return the query to which the tag is mapped, or null if the hash table contains no mapping for the tag.
     */
    public String getQuery(String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        return mTagQueryTable.get(tag);
    }

    /**
     * Returns the hash table.
     * @return the hash table.
     */
    public Hashtable<String, String> getTagQueryTable() {
        return mTagQueryTable;
    }

    /**
     * Returns the parameters map.
     * @return the parameters map.
     */
    public HashMap<String, String[]> getTagQueryParameters() {
        return mTagQueryParameters;
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#insert(String, String, android.content.ContentValues) insert} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long insert(String table, String nullColumnHack, ContentValues values, String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        try {
            return insertWithOnConflict(table, nullColumnHack, values, SQLiteDatabase.CONFLICT_NONE, tag);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + values, e);
            return -1;
        }
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#insertOrThrow(String, String, android.content.ContentValues) insertOrThrow} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values, String tag)
            throws SQLException {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        return insertWithOnConflict(table, nullColumnHack, values, SQLiteDatabase.CONFLICT_NONE, tag);
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replace(String, String, android.content.ContentValues) replace} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long replace(String table, String nullColumnHack, ContentValues initialValues, String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues,
                    SQLiteDatabase.CONFLICT_REPLACE, tag);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + initialValues, e);
            return -1;
        }
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replaceOrThrow(String, String, android.content.ContentValues) replaceOrThrow} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long replaceOrThrow(String table, String nullColumnHack,
                               ContentValues initialValues, String tag) throws SQLException {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        return insertWithOnConflict(table, nullColumnHack, initialValues,
                SQLiteDatabase.CONFLICT_REPLACE, tag);
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replaceOrThrow(String, String, android.content.ContentValues) insertWithOnConflict} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long insertWithOnConflict(String table, String nullColumnHack,
                                     ContentValues initialValues, int conflictAlgorithm, String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        long id = mSQLiteDatabase.insertWithOnConflict(
                table,
                nullColumnHack,
                initialValues,
                conflictAlgorithm
        );

        if (id != -1) {
            StringBuilder restoring_query = new StringBuilder();

            restoring_query.append("DELETE FROM ");
            restoring_query.append(table);
            restoring_query.append(" WHERE ");
            restoring_query.append(ROWID);
            restoring_query.append(" = ?");

            mTagQueryTable.put(tag, restoring_query.toString());
            mTagQueryParameters.put(tag, new String[] {id + ""});
        }

        return id;
    }

}
