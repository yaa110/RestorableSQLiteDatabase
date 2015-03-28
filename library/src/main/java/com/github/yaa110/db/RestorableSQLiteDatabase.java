package com.github.yaa110.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
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
     * The hash table to map a tag to its restoring queries.
     */
    public Hashtable<String, ArrayList<String>> mTagQueryTable;

    /**
     * The hash table to map a tag to the parameters should be used in the queries.
     */
    public Hashtable<String, ArrayList<String[]>> mTagQueryParameters;

    /**
     * Singleton pattern constructor
     * @param mSQLiteDatabase the instance of the SQLiteDatabase to be wrapped.
     * @return an instance of this class.
     */
    public static RestorableSQLiteDatabase getInstance(SQLiteDatabase mSQLiteDatabase){
        if(mInstance == null) {
            mInstance = new RestorableSQLiteDatabase(mSQLiteDatabase);
        }
        return mInstance;
    }

    /**
     * Singleton pattern constructor.
     * @param helper the instance of the SQLiteOpenHelper to open a database using {@link android.database.sqlite.SQLiteOpenHelper#getWritableDatabase() getWritableDatabase} method.
     * @return an instance of this class.
     */
    public static RestorableSQLiteDatabase getInstance(SQLiteOpenHelper helper){
        if(mInstance == null) {
            mInstance = new RestorableSQLiteDatabase(helper);
        }
        return mInstance;
    }

    /**
     * Singleton pattern constructor to force this to renew the instance. Do not use this constructor, if you do not want to change the database.
     * @param mSQLiteDatabase the instance of the SQLiteDatabase to be wrapped.
     * @return an instance of this class.
     */
    public static RestorableSQLiteDatabase getNewInstance(SQLiteDatabase mSQLiteDatabase){
        mInstance = new RestorableSQLiteDatabase(mSQLiteDatabase);
        return mInstance;
    }

    /**
     * Singleton pattern constructor to force this to renew the instance. Do not use this constructor, if you do not want to change the database.
     * @param helper the instance of the SQLiteOpenHelper to open a database using {@link android.database.sqlite.SQLiteOpenHelper#getWritableDatabase() getWritableDatabase} method.
     * @return an instance of this class.
     */
    public static RestorableSQLiteDatabase getNewInstance(SQLiteOpenHelper helper){
        mInstance = new RestorableSQLiteDatabase(helper);
        return mInstance;
    }

    /**
     * Private constructor of singleton pattern.
     * @param mSQLiteDatabase the instance of the SQLiteDatabase to be wrapped.
     */
    private RestorableSQLiteDatabase(SQLiteDatabase mSQLiteDatabase) {
        mTagQueryTable = new Hashtable<>();
        mTagQueryParameters = new Hashtable<>();
        this.mSQLiteDatabase = mSQLiteDatabase;
    }

    /**
     * Private constructor of singleton pattern.
     * @param helper the instance of the SQLiteOpenHelper to open a database using {@link android.database.sqlite.SQLiteOpenHelper#getWritableDatabase() getWritableDatabase} method.
     */
    private RestorableSQLiteDatabase(SQLiteOpenHelper helper) {
        mTagQueryTable = new Hashtable<>();
        mTagQueryParameters = new Hashtable<>();
        this.mSQLiteDatabase = helper.getWritableDatabase();
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
     * @param tag Possible tag of restoring queries.
     * @throws IllegalArgumentException if the tag is null.
     * @return the queries to which the tag is mapped, or null if the hash table contains no mapping for the tag.
     */
    public ArrayList<String> getQueries(String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        return mTagQueryTable.get(tag);
    }

    /**
     * Returns the hash table.
     * @return the hash table.
     */
    public Hashtable<String, ArrayList<String>> getTagQueryTable() {
        return mTagQueryTable;
    }

    /**
     * Returns the parameters hash table.
     * @return the parameters hash table.
     */
    public Hashtable<String, ArrayList<String[]>> getTagQueryParameters() {
        return mTagQueryParameters;
    }

    /**
     * Changes the hash table.
     * @param tagQueryTable the substitute hash table.
     */
    public void setTagQueryTable(Hashtable<String, ArrayList<String>> tagQueryTable) {
        this.mTagQueryTable = tagQueryTable;
    }

    /**
     * Changes the parameters hash table.
     * @param tagQueryParameters the substitute hash table.
     */
    public void setTagQueryParameters(Hashtable<String, ArrayList<String[]>> tagQueryParameters) {
        this.mTagQueryParameters = tagQueryParameters;
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
     * @param rowIdAlias The alias used in the initialValues to set the value of ROWID. Passing null will cause using the default value of rowid.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long replace(String table, String nullColumnHack, ContentValues initialValues, String tag, String rowIdAlias) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        try {
            return insertWithOnConflict(table, nullColumnHack, initialValues,
                    SQLiteDatabase.CONFLICT_REPLACE, tag, rowIdAlias);
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting " + initialValues, e);
            return -1;
        }
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replaceOrThrow(String, String, android.content.ContentValues) replaceOrThrow} method.
     * @param tag The tag to be mapped to the restoring query.
     * @param rowIdAlias The alias used in the initialValues to set the value of ROWID. Passing null will cause using the default value of rowid.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long replaceOrThrow(String table, String nullColumnHack,
                               ContentValues initialValues, String tag, String rowIdAlias) throws SQLException {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        return insertWithOnConflict(table, nullColumnHack, initialValues,
                SQLiteDatabase.CONFLICT_REPLACE, tag, rowIdAlias);
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replaceOrThrow(String, String, android.content.ContentValues) insertWithOnConflict} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long insertWithOnConflict(String table, String nullColumnHack,
                                     ContentValues initialValues, int conflictAlgorithm, String tag) {
        return insertWithOnConflict(
                table,
                nullColumnHack,
                initialValues,
                conflictAlgorithm,
                tag,
                null
        );
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#replaceOrThrow(String, String, android.content.ContentValues) insertWithOnConflict} method.
     * @param tag The tag to be mapped to the restoring query.
     * @param rowIdAlias The alias used in the initialValues to set the value of ROWID. Passing null will cause using the default value of rowid.
     * @throws IllegalArgumentException if the tag is null.
     */
    public long insertWithOnConflict(String table, String nullColumnHack,
                                     ContentValues initialValues, int conflictAlgorithm, String tag, String rowIdAlias) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        ArrayList<String> queries = new ArrayList<>();
        ArrayList<String[]> queriesParameters = new ArrayList<>();

        // Determines if restoring query of replacement is generated
        boolean restore_status = false;

        // Generates replacement restoring query
        if (conflictAlgorithm == SQLiteDatabase.CONFLICT_REPLACE) {
            if (rowIdAlias == null) rowIdAlias = ROWID;

            Cursor restoring_cursor = mSQLiteDatabase.query(
                    table,
                    null,
                    rowIdAlias + " = ?",
                    new String[] {(String) initialValues.get(rowIdAlias)},
                    null,
                    null,
                    null,
                    null
            );

            if (restoring_cursor.moveToFirst()) {
                StringBuilder sql = new StringBuilder();
                sql.append("UPDATE ");
                sql.append(table);
                sql.append(" SET ");

                int i = 0;
                String[] parameters = new String[restoring_cursor.getColumnCount()];

                for (String columnName : restoring_cursor.getColumnNames()) {
                    if (columnName.equals(rowIdAlias))
                        continue;

                    if (i > 0) sql.append(", ");

                    sql.append(columnName);
                    sql.append(" = ?");
                    parameters[i] = restoring_cursor.getString(restoring_cursor.getColumnIndex(columnName));

                    i++;
                }

                sql.append(" WHERE ");
                sql.append(rowIdAlias);
                sql.append(" = ?");
                parameters[i] = (String) initialValues.get(rowIdAlias);

                queries.add(sql.toString());
                queriesParameters.add(parameters);

                restore_status = true;
            }

            restoring_cursor.close();
        }

        // Executes query
        long id = mSQLiteDatabase.insertWithOnConflict(
                table,
                nullColumnHack,
                initialValues,
                conflictAlgorithm
        );

        // Generates query to restore insertion
        if (!restore_status) {
            queries.add("DELETE FROM " + table + " WHERE " + ROWID + " = ?");
            queriesParameters.add(new String[] {id + ""});
        }

        // Add queries and their parameters if no error has occurred
        if (id != -1) {
            mTagQueryTable.put(tag, queries);
            mTagQueryParameters.put(tag, queriesParameters);
        }

        return id;
    }



    // TODO rwaQueries, executions, update and delete restoring: http://grepcode.com/file/repo1.maven.org/maven2/org.robolectric/android-all/5.0.0_r2-robolectric-0/android/database/sqlite/SQLiteDatabase.java

}
