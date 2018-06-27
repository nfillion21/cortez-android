package com.tezos.core.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import com.tezos.core.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class EnglishWordsDatabaseTable
{
    private static final String TAG = "EnglishWordsDatabase";

    //The columns we'll include in the dictionary table
    public static final String COL_WORD = "WORD";

    private static final String DATABASE_NAME = "ENGLISH_WORDS";
    public static final String FTS_VIRTUAL_TABLE = "FTS";
    private static final int DATABASE_VERSION = 1;

    private final DatabaseOpenHelper mDatabaseOpenHelper;

    public EnglishWordsDatabaseTable(Context context)
    {
        mDatabaseOpenHelper = new DatabaseOpenHelper(context);
    }

    private Cursor query(String selection, String[] selectionArgs, String[] columns)
    {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);

        Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null);

        if (cursor == null)
        {
            return null;
        }
        else if (!cursor.moveToFirst())
        {
            cursor.close();
            return null;
        }
        return cursor;
    }

    public Cursor getWord(String rowId, String[] columns) {
        String selection = "rowid = ?";
        String[] selectionArgs = new String[] {rowId};

        return query(selection, selectionArgs, columns);

        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE rowid = <rowId>
         */
    }

    public Cursor getWordMatches(String query, String[] columns)
    {
        String selection = COL_WORD + " MATCH ?";
        String[] selectionArgs = new String[] {query+"*"};

        return query(selection, selectionArgs, columns);

        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE <KEY_WORD> MATCH 'query*'
         * which is an FTS3 search for the query text (plus a wildcard) inside the word column.
         *
         * - "rowid" is the unique id for all rows but we need this value for the "_id" column in
         *    order for the Adapters to work, so the columns need to make "_id" an alias for "rowid"
         * - "rowid" also needs to be used by the SUGGEST_COLUMN_INTENT_DATA alias in order
         *   for suggestions to carry the proper intent data.
         *   These aliases are defined in the DictionaryProvider when queries are made.
         * - This can be revised to also search the definition text with FTS3 by changing
         *   the selection clause to use FTS_VIRTUAL_TABLE instead of KEY_WORD (to search across
         *   the entire table, but sorting the relevance could be difficult.
         */
    }

    private static class DatabaseOpenHelper extends SQLiteOpenHelper
    {
        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts3 (" +
                        COL_WORD + ")";

        DatabaseOpenHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
        }

        private void loadDictionary()
        {
            new Thread(() ->
            {
                try
                {
                    loadWords();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        private void loadWords() throws IOException {
            final Resources resources = mHelperContext.getResources();
            InputStream inputStream = resources.openRawResource(R.raw.english);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    long id = addWord(line.trim());
                    if (id < 0)
                    {
                        Log.e(TAG, "unable to add word: " + line.trim());
                    }
                }
            }
            finally
            {
                reader.close();
            }
            Log.d(TAG, "DONE loading words.");
        }

        public long addWord(String word)
        {
            ContentValues initialValues = new ContentValues();
            initialValues.put(COL_WORD, word);

            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }
    }
}
