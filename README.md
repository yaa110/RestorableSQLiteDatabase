Restorable SQLiteDatabase
=========================
RestorableSQLiteDatabase is a wrapper to replicate android's [SQLiteDatabase](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html) class to manage a SQLite database with restoring capability. This wrapper makes it possible to undo changes made after execution of SQL queries.

## How to use

**Use Gradle**

Reference library using this dependency in your module's `build.gradle` file:

```Gradle
repositories {
    maven {
        url  "http://dl.bintray.com/yaa110/maven"
    }
}

dependencies {
    compile 'github.yaa110.db:restorablesqlitedatabase:0.1.0'
}
```

**or Use Maven**

```xml
<dependency>
	<groupId>github.yaa110.db</groupId>
	<artifactId>restorablesqlitedatabase</artifactId>
	<version>0.1.0</version>
	<type>aar</type>
</dependency>
```

[ ![Download](https://img.shields.io/badge/Download-0.1.0-green.svg) ](https://bintray.com/yaa110/maven/restorablesqlitedatabase/view)

## Example: Undoing deleted rows

First, create a subclass of `SQLiteOpenHelper`:

```java
public class DbHelper extends SQLiteOpenHelper {

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        COLUMN_ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_TITLE + " TEXT" +
                        ");"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int old_version, int new_version) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
```

Then, use `RestorableSQLiteDatabase`:
```java
HashMap<String, String> tableRowid = new HashMap<>();
tableRowid.put(TABLE_NAME, COLUMN_ROWID);

DbHelper helper = new DbHelper(this);

RestorableSQLiteDatabase db = new RestorableSQLiteDatabase(helper, tableRowid);

// Delete some rows
db.delete(
        TABLE_NAME,
        COLUMN_TITLE + " = ?",
        new String[] {"demo"},
        "DELETION_TAG"
);

// Undoing deletion
db.restore("DELETION_TAG");
```

## Documentation
```java
public static RestorableSQLiteDatabase getInstance(SQLiteDatabase mSQLiteDatabase, HashMap<String, String> tableRowid)
```

Constructs a new instance of the `RestorableSQLiteDatabase` only if no instance is constructed.

**Parameters**
- *mSQLiteDatabase* the instance of the `SQLiteDatabase` to be wrapped.
- *tableRowid* maps the table name to its ROWID column name.

```java
public static <T extends SQLiteOpenHelper> RestorableSQLiteDatabase getInstance(T helper, HashMap<String, String> tableRowid)
```

Constructs a new instance of the `RestorableSQLiteDatabase` only if no instance is constructed.

**Parameters**
- *helper* the instance of the `SQLiteOpenHelper` to open a database using its [getWritableDatabase](http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html#getWritableDatabase()) method.
- *tableRowid* maps the table name to its ROWID column name.

```java
public static RestorableSQLiteDatabase getNewInstance(SQLiteDatabase mSQLiteDatabase, HashMap<String, String> tableRowid)
```

Constructs a new instance of the `RestorableSQLiteDatabase`.

**Parameters**
- *mSQLiteDatabase* the instance of the `SQLiteDatabase` to be wrapped.
- *tableRowid* maps the table name to its ROWID column name.

```java
public static <T extends SQLiteOpenHelper> RestorableSQLiteDatabase getNewInstance(T helper, HashMap<String, String> tableRowid)
```

Constructs a new instance of the `RestorableSQLiteDatabase`.

**Parameters**
- *helper* the instance of the `SQLiteOpenHelper` to open a database using its [getWritableDatabase](http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html#getWritableDatabase()) method.
- *tableRowid* maps the table name to its ROWID column name.

```java
public void close()
```

Closes the SQLite database. Use the `reopen` methods to reopen the SQLite database.

```java
public boolean containsTag(String tag)
```

Checks if the hash table contains the tag.

**Parameters**
- *tag* possible tag of restoring query.

**Returns**

True if the hash table contains the tag; false otherwise.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public int delete(String table, String whereClause, String[] whereArgs, String tag)
```

Replicates the [delete](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#delete(java.lang.String, java.lang.String, java.lang.String[])) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public ArrayList<String> getQueries(String tag)
```

Provides the query to which the tag is mapped.

**Parameters**
- *tag* possible tag of restoring query.

**Returns**

The queries to which the tag is mapped, or null if the hash table contains no mapping for the tag.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public SQLiteDatabase getSQLiteDatabase()
```

Provides the instance of wrapped `SQLiteDatabase`.

**Returns**

The instance of wrapped `SQLiteDatabase`.

```java
public Hashtable<String, ArrayList<String[]>> getTagQueryParameters()
```

Provides the parameters hash table.

**Returns**

The parameters hash table.

```java
public Hashtable<String, ArrayList<String>> getTagQueryTable()
```

Provides the hash table.

**Returns**

The hash table.

```java
public long insert(String table, String nullColumnHack, ContentValues values, String tag)
```

Replicates the [insert](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#insert(java.lang.String, java.lang.String, android.content.ContentValues)) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public long insertOrThrow(String table, String nullColumnHack, ContentValues values, String tag) throws SQLException
```

Replicates the [insertOrThrow](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#insertOrThrow(java.lang.String, java.lang.String, android.content.ContentValues)) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm, String tag)
```

Replicates the [insertWithOnConflict](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#insertWithOnConflict(java.lang.String, java.lang.String, android.content.ContentValues, int)) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public Cursor rawQuery(String sql, String[] selectionArgs, String tag) throws JSQLParserException, ClassCastException
```

Replicates the [rawQuery](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#rawQuery(java.lang.String, java.lang.String[])) method of the `SQLiteDatabase`.

Unlike the `rawQuery` of the `SQLiteDatabase`, there is no need to call the `moveToFirst` method of the returned `Cursor` to apply SQL query.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public Cursor rawQuery(String sql, String[] selectionArgs, CancellationSignal cancellationSignal, String tag) throws JSQLParserException, ClassCastException
```

Replicates the [rawQuery](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#rawQuery(java.lang.String, java.lang.String[], android.os.CancellationSignal)) method of the `SQLiteDatabase`.

Unlike the `rawQuery` of the `SQLiteDatabase`, there is no need to call the `moveToFirst` method of the returned `Cursor` to apply SQL query.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public void reopen(SQLiteDatabase mSqLiteDatabase)
```

Reopens the SQLite database.

**Parameters**
- *mSqLiteDatabase* the instance of the `SQLiteDatabase` to be wrapped.

```java
public <T extends SQLiteOpenHelper> void reopen(T helper)
```

Reopens the SQLite database.

**Parameters**
- *helper* the instance of the `SQLiteOpenHelper` to open a database using its [getWritableDatabase](http://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html#getWritableDatabase()) method.

```java
public long replace(String table, String nullColumnHack, ContentValues initialValues, String tag)
```

Replicates the [replace](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#replace(java.lang.String, java.lang.String, android.content.ContentValues)) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public long replaceOrThrow(String table, String nullColumnHack, ContentValues initialValues, String tag) throws SQLException
```

Replicates the [replaceOrThrow](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#replaceOrThrow(java.lang.String, java.lang.String, android.content.ContentValues)) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public int restore(String tag)
```

Restores the SQL queries to which the tag is mapped.

**Parameters**
- *tag* the tag mapped to restoring queries.

**Returns**

Possible number of restored queries to which tag is mapped.

```java
public int restore(String[] tags)
```

Restores the queries to which each tag is mapped.

**Parameters**
- *tags* an array of tags mapped to restoring SQL queries.

**Returns**

Possible number of restored queries to which tag is mapped.

```java
public int restore(Set<String> tags)
```

Restores the queries to which each tag is mapped.

**Parameters**
- *tags* a set of tags mapped to restoring SQL queries.

**Returns**

Possible number of restored queries to which tag is mapped.

```java
public int restoreAll()
```

Restores all restoring SQL queries.

**Returns**

Possible number of restored queries to which tag is mapped.

```java
public void setTagQueryParameters(Hashtable<String, ArrayList<String[]>> tagQueryParameters)
```

Changes the parameters hash table.

**Parameters**
- *tagQueryParameters* the substitute hash table.

```java
public void setTagQueryTable(Hashtable<String, ArrayList<String>> tagQueryTable)
```

Changes the hash table.

**Parameters**
- *tagQueryTable* the substitute hash table.

```java
public Set<String> tagSet()
```

Provides a `Set` view of the tags contained in the hash table.

**Returns**

a `Set` view of the tags contained in the hash table.

```java
public int update(String table, ContentValues values, String whereClause, String[] whereArgs, String tag)
```

Replicates the [update](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#update(java.lang.String, android.content.ContentValues, java.lang.String, java.lang.String[])) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

```java
public int updateWithOnConflict(String table, ContentValues values, String whereClause, String[] whereArgs, int conflictAlgorithm, String tag)
```

Replicates the [updateWithOnConflict](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html#updateWithOnConflict(java.lang.String, android.content.ContentValues, java.lang.String, java.lang.String[], int)) method of the `SQLiteDatabase`.

**Parameters**
- *tag* the tag to be mapped to the restoring query.

**Throws**
- *IllegalArgumentException* if the tag is null.

## Dependencies

**[JSqlParser](https://github.com/JSQLParser/JSqlParser)** parses an SQL statement and translate it into a hierarchy of Java classes. JSqlParser is licensed under the LGPL V2.1.

## License
RestorableSQLiteDatabase is licensed under the MIT License.
