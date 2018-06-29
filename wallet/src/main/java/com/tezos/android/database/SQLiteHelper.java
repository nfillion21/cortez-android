package com.tezos.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tezos.core.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SQLiteHelper extends SQLiteOpenHelper implements EnglishWordsDatabaseConstants
{
	public static final int DB_VERSION = 1;
	private final Context mHelperContext;

	public SQLiteHelper(Context context)
	{
		super(context, DB_NAME, null, DB_VERSION);

		mHelperContext = context;
		getWritableDatabase();
	}

	private static final String FTS_TABLE_CREATE =
			"CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
					" USING fts3 (" +
					COL_WORD + ")";

	private static final String STANDARD_TABLE_CREATE =
			"CREATE TABLE " + TABLE_WORD + "(" + COL_ID
					+ " INTEGER PRIMARY KEY NOT NULL, " + " " + COL_WORD
					+ " VARCHAR(50) NOT NULL);";

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE " + TABLE_WORD + "(" + COL_ID
				+ " INTEGER PRIMARY KEY NOT NULL, " + " " + COL_WORD
				+ " VARCHAR(50) NOT NULL);");
		loadDictionary(db);
	}

	private void loadDictionary(SQLiteDatabase db)
	{
		new Thread(() ->
		{
			try
			{
				loadWords(db);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}).start();
	}

	private void loadWords(SQLiteDatabase db) throws IOException
	{
		final Resources resources = mHelperContext.getResources();
		InputStream inputStream = resources.openRawResource(R.raw.english);
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		try
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				long id = addWord(db, line.trim());
				if (id < 0)
				{
					//Log.e(TAG, "unable to add word: " + line.trim());
				}
			}
		}
		finally
		{
			reader.close();
		}
		//Log.d(TAG, "DONE loading words.");
	}

	public long addWord(SQLiteDatabase db, String word)
	{
		ContentValues initialValues = new ContentValues();
		initialValues.put(COL_WORD, word);

		//db.execSQL("INSERT INTO " + TABLE_WORD + " (" + COL_WORD
				//+ ") VALUES ('" + word + "');");

				//TODO handle FTS virtual table
		//return db.insert(FTS_VIRTUAL_TABLE, null, initialValues);
		return db.insert(TABLE_WORD, null, initialValues);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
	    /*
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
		onCreate(db);
		*/
	}
}
