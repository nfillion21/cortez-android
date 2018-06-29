package com.tezos.android;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;

import com.tezos.android.database.EnglishWordsContentProvider;
import com.tezos.android.database.EnglishWordsDatabaseConstants;

public class LanguageListActivity extends ListActivity implements
        LoaderCallbacks<Cursor> {
	private static final int LOADER_ID = 42;
	private CursorAdapter _adapter;
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.container_list);
		_adapter = new SimpleCursorAdapter(this,
				R.layout.container_list_item_view, null,
				new String[] {EnglishWordsDatabaseConstants.COL_WORD },
				new int[] { R.id.list_item });
		setListAdapter(_adapter);
		getLoaderManager().initLoader(LOADER_ID, null, this);
	}

	public static void start(Activity activity)
	{

		//Intent paymentFormIntent = LanguageListActivity.getStartIntent(activity, paymentPageRequestBundle, themeBundle);
		//Intent paymentFormIntent = CreateWalletActivity.getStartIntent(activity, themeBundle);
		//Intent paymentFormIntent = RestoreWalletActivity.getStartIntent(activity, themeBundle);
        /*
        ActivityCompat.startActivityForResult(activity,
                startIntent,
                PaymentPageRequest.REQUEST_ORDER,
                //transitionBundle);
                //avoid glitch problem
                null);
                */
		Intent starter = new Intent(activity, LanguageListActivity.class);

		ActivityCompat.startActivity(activity, starter, null);
		//PaymentFormActivity.start(activity, paymentPageRequest, theme);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (id != LOADER_ID) {
			return null;
		}
		return new CursorLoader(LanguageListActivity.this,
				EnglishWordsContentProvider.CONTENT_URI,
				new String[] { EnglishWordsDatabaseConstants.COL_ID, EnglishWordsDatabaseConstants.COL_WORD }, null, null,
				null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		_adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		_adapter.swapCursor(null);
	}
}
