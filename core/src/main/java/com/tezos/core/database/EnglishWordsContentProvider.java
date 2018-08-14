package com.tezos.core.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.tezos.core.BuildConfig;
import com.tezos.core.database.EnglishWordsDatabaseConstants;
import com.tezos.core.database.SQLiteHelper;

/**
 * Provides access to the dictionary database.
 */
public class EnglishWordsContentProvider extends ContentProvider
{
    /*
    public static String AUTHORITY = "com.tezos.core.database.EnglishWordsProvider";
    //public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".EnglishWordsProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + EnglishWordsDatabaseConstants.TABLE_WORD);
    */
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

        selection = EnglishWordsDatabaseConstants.COL_WORD + " LIKE ?";
        if (selectionArgs != null && selectionArgs.length > 0)
        {
            selectionArgs = new String[] { selectionArgs[0] + "%"};
        }

        Cursor cursor = qb.query(dbHelper.getReadableDatabase(),
                new String[] { EnglishWordsDatabaseConstants.COL_ID,
                        EnglishWordsDatabaseConstants.COL_WORD }, selection,
                selectionArgs, null, null, null);
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

