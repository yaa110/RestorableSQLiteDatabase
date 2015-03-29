package com.github.yaa110.db;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.update.Update;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;

/**
 * A wrapper to replicate android's SQLiteDatabase class to manage a SQLite database with restoring capability.
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

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#update(String, android.content.ContentValues, String, String[]) update} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs, String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        return updateWithOnConflict(table, values, whereClause, whereArgs, SQLiteDatabase.CONFLICT_NONE, tag);
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#updateWithOnConflict(String, android.content.ContentValues, String, String[], int) updateWithOnConflict} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public int updateWithOnConflict(String table, ContentValues values,
                                    String whereClause, String[] whereArgs, int conflictAlgorithm, String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        generateRestoringUpdate(
                table,
                whereClause,
                whereArgs,
                tag
        );

        return mSQLiteDatabase.updateWithOnConflict(
                table,
                values,
                whereClause,
                whereArgs,
                conflictAlgorithm
        );
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#delete(String, String, String[]) delete} method.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public int delete(String table, String whereClause, String[] whereArgs, String tag) {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        generateRestoringDelete(
                table,
                whereClause,
                whereArgs,
                tag
        );

        return mSQLiteDatabase.delete(
                table,
                whereClause,
                whereArgs
        );
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#rawQuery(String, String[]) rawQuery} method.
     * This method uses {@link net.sf.jsqlparser.parser.CCJSqlParserUtil#parse(String) parser} method to parse the SQL query.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    public Cursor rawQuery(String sql, String[] selectionArgs, String tag)
            throws JSQLParserException, ClassCastException {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        Statement statement = CCJSqlParserUtil.parse(sql);

        generateRawUpdateDeleteQuery(statement, sql, selectionArgs, tag);

        Cursor cursor =  mSQLiteDatabase.rawQuery(sql, selectionArgs);

        if (sql.toLowerCase(Locale.getDefault()).contains("insert into")) {
            Insert insertStatement = (Insert) statement;
            String table = insertStatement.getTable().getName();
            generateInsertRawQuery(cursor, table, tag);
        }

        return cursor;
    }

    /**
     * Use the {@link android.database.sqlite.SQLiteDatabase#rawQuery(String, String[]) rawQuery} method.
     * This method uses {@link net.sf.jsqlparser.parser.CCJSqlParserUtil#parse(String) parser} method to parse the SQL query.
     * @param tag The tag to be mapped to the restoring query.
     * @throws IllegalArgumentException if the tag is null.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public Cursor rawQuery(String sql, String[] selectionArgs,
                           CancellationSignal cancellationSignal, String tag)
            throws JSQLParserException, ClassCastException {
        if (tag == null)
            throw new IllegalArgumentException("The tag must not be null.");

        Statement statement = CCJSqlParserUtil.parse(sql);

        generateRawUpdateDeleteQuery(statement, sql, selectionArgs, tag);

        Cursor cursor = mSQLiteDatabase.rawQuery(sql, selectionArgs, cancellationSignal);

        if (sql.toLowerCase(Locale.getDefault()).contains("insert into")) {
            Insert insertStatement = (Insert) statement;
            String table = insertStatement.getTable().getName();
            generateInsertRawQuery(cursor, table, tag);
        }

        return cursor;
    }

    /**
     * Generates the restoring query of rawQuery methods.
     * @param sql the SQL query.
     * @param selectionArgs arguments to be replaced with ? in the SQL query.
     * @throws JSQLParserException
     */
    private void generateRawUpdateDeleteQuery(Statement statement, String sql, String[] selectionArgs, String tag)
            throws JSQLParserException, ClassCastException {

        String table = null;
        String where = null;

        if (sql.toLowerCase(Locale.getDefault()).contains("update")) {

            Update updateStatement = (Update) statement;
            table = updateStatement.getTables().get(0).getName();
            where = updateStatement.getWhere().toString();
            generateRestoringUpdate(
                    table,
                    where,
                    selectionArgs,
                    tag
            );

        } else if (sql.toLowerCase(Locale.getDefault()).contains("delete")) {

            Delete deleteStatement = (Delete) statement;
            table = deleteStatement.getTable().getName();
            where = deleteStatement.getWhere().toString();
            generateRestoringDelete(
                    table,
                    where,
                    selectionArgs,
                    tag
            );
        }
    }

    /**
     * Generates the restoring query of rawQuery insertion.
     * @param cursor returned cursor over the result of rawQuery.
     * @param table the table name.
     * @param tag the tag mapped to restoring queries.
     * @throws JSQLParserException
     * @throws ClassCastException
     */
    private void generateInsertRawQuery(Cursor cursor, String table, String tag)
            throws JSQLParserException, ClassCastException {
        ArrayList<String> queries = new ArrayList<>();
        ArrayList<String[]> queriesParameters = new ArrayList<>();

        queries.add("DELETE FROM " + table + " WHERE " + ROWID + " = ?");
        queriesParameters.add(new String[] {cursor.getString(cursor.getColumnIndex(ROWID))});

        mTagQueryTable.put(tag, queries);
        mTagQueryParameters.put(tag, queriesParameters);
    }

    /**
     * Generates the restoring query of rawQuery deletion.
     * @param table the table name.
     * @param whereClause the optional WHERE clause to apply when deleting.
     * @param whereArgs arguments to be replaced with ? in the SQL query.
     * @param tag the tag mapped to restoring queries.
     */
    private void generateRestoringDelete(String table,
                                         String whereClause,
                                         String[] whereArgs,
                                         String tag) {
        // Gets all affected_rows
        Cursor restoring_cursor = mSQLiteDatabase.query(
                table,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        ArrayList<String> queries = new ArrayList<>();
        ArrayList<String[]> queriesParameters = new ArrayList<>();

        // Generates restoring queries
        while (restoring_cursor.moveToNext()) {
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT OR REPLACE INTO ");
            sql.append(table);

            int i = 0;
            String[] parameters = new String[restoring_cursor.getColumnCount()];

            StringBuilder sql_columns = new StringBuilder();
            StringBuilder sql_values = new StringBuilder();

            for (String columnName : restoring_cursor.getColumnNames()) {
                if (i > 0) {
                    sql_columns.append(", ");
                    sql_values.append(", ");
                } else {
                    sql_columns.append(" (");
                    sql_values.append(" (");
                }

                sql_columns.append(columnName);
                sql_values.append("?");
                parameters[i] = restoring_cursor.getString(restoring_cursor.getColumnIndex(columnName));

                i++;
            }

            sql_columns.append(")");
            sql_values.append(")");

            sql.append(sql_columns.toString());
            sql.append(" VALUES ");
            sql.append(sql_values.toString());

            queries.add(sql.toString());
            queriesParameters.add(parameters);
        }

        restoring_cursor.close();

        mTagQueryTable.put(tag, queries);
        mTagQueryParameters.put(tag, queriesParameters);
    }

    /**
     * Generates the restoring query of rawQuery updating.
     * @param table the table name.
     * @param whereClause the optional WHERE clause to apply when updating.
     * @param whereArgs arguments to be replaced with ? in the SQL query.
     * @param tag the tag mapped to restoring queries.
     */
    private void generateRestoringUpdate(String table,
                                         String whereClause,
                                         String[] whereArgs,
                                         String tag) {
        // Gets all affected_rows
        Cursor restoring_cursor = mSQLiteDatabase.query(
                table,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        ArrayList<String> queries = new ArrayList<>();
        ArrayList<String[]> queriesParameters = new ArrayList<>();

        // Generates restoring queries
        while (restoring_cursor.moveToNext()) {
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ");
            sql.append(table);
            sql.append(" SET ");

            int i = 0;
            String[] parameters = new String[restoring_cursor.getColumnCount() + 1];

            for (String columnName : restoring_cursor.getColumnNames()) {
                if (i > 0) sql.append(", ");

                sql.append(columnName);
                sql.append(" = ?");
                parameters[i] = restoring_cursor.getString(restoring_cursor.getColumnIndex(columnName));

                i++;
            }

            sql.append(" WHERE ");
            sql.append(ROWID);
            sql.append(" = ?");
            parameters[i] = restoring_cursor.getString(restoring_cursor.getColumnIndex(ROWID));

            queries.add(sql.toString());
            queriesParameters.add(parameters);
        }

        restoring_cursor.close();

        mTagQueryTable.put(tag, queries);
        mTagQueryParameters.put(tag, queriesParameters);
    }

    /**
     * Restores all restoring queries.
     * @return possible number of restored queries to which tag is mapped.
     */
    public int restoreAll() {
        return restore(mTagQueryTable.keySet());
    }

    /**
     * Restores the queries to which each tag is mapped.
     * @param tags an array of tags mapped to restoring queries.
     * @return possible number of restored queries to which tag is mapped.
     */
    public int restore(String[] tags) {
        int restored_queries = 0;

        for (String tag : tags) {
            restored_queries += restore(tag);
        }

        return restored_queries;
    }

    /**
     * Restores the queries to which each tag is mapped.
     * @param tags a set of tags mapped to restoring queries.
     * @return possible number of restored queries to which tag is mapped.
     */
    public int restore(Set<String> tags) {
        int restored_queries = 0;

        for (String tag : tags) {
            restored_queries += restore(tag);
        }

        return restored_queries;
    }

    /**
     * Restores the queries to which the tag is mapped.
     * @param tag the tag mapped to restoring queries.
     * @return possible number of restored queries to which tag is mapped.
     */
    public int restore(String tag) {
        ArrayList<String> queries = mTagQueryTable.get(tag);
        ArrayList<String[]> parameters = mTagQueryParameters.get(tag);

        if (queries != null && parameters != null) {
            int restored_queries = queries.size();

            for (int i = 0; i < restored_queries; i++) {
                mSQLiteDatabase.rawQuery(
                        queries.get(i),
                        parameters.get(i)
                );
            }

            mTagQueryTable.remove(tag);
            mTagQueryParameters.remove(tag);

            return restored_queries;
        }

        return 0;
    }

}
