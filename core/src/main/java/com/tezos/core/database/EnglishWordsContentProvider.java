package com.tezos.core.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Provides access to the dictionary database.
 */
public class EnglishWordsContentProvider extends ContentProvider
{
    public static String AUTHORITY = "com.tezos.android.database.EnglishWordsProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + EnglishWordsDatabaseConstants.TABLE_WORD);
    private SQLiteHelper dbHelper;

    @Override
    public boolean onCreate()
    {
        dbHelper = new SQLiteHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(EnglishWordsDatabaseConstants.TABLE_WORD);
        String orderBy = EnglishWordsDatabaseConstants.COL_WORD + " asc";
        Cursor cursor = qb.query(dbHelper.getReadableDatabase(),
                new String[] { EnglishWordsDatabaseConstants.COL_ID,
                        EnglishWordsDatabaseConstants.COL_WORD }, null,
                null, null, null, orderBy);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.com.tezos.core.database.provider.LanguageContentProvider";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}

