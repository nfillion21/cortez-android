package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.tezos.core.models.Account;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.PaymentAccountsFragment;

/**
 * Created by nfillion on 25/02/16.
 */
public class PaymentAccountsActivity extends AppCompatActivity implements PaymentAccountsFragment.OnCardSelectedListener
{
    public static String SELECTED_REQUEST_CODE_KEY = "selectedRequestCodeKey";

    public static void start(Activity activity, CustomTheme theme, Selection selection)
    {
        Intent starter = getStartIntent(activity, theme, selection);
        ActivityCompat.startActivityForResult(activity, starter, PaymentFormActivity.TRANSFER_SELECT_REQUEST_CODE, null);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @NonNull
    static Intent getStartIntent(Context context, CustomTheme theme, Selection selection)
    {
        Intent starter = new Intent(context, PaymentAccountsActivity.class);
        if (theme == null)
        {
            theme = new CustomTheme(R.color.tz_primary,R.color.tz_primary_dark,R.color.tz_light);
        }

        starter.putExtra(CustomTheme.TAG, theme.toBundle());
        starter.putExtra(SELECTED_REQUEST_CODE_KEY, selection.getStringValue());

        return starter;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_accounts);

        Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme customTheme = CustomTheme.fromBundle(customThemeBundle);

        if (savedInstanceState == null)
        {
            attachAccountGridFragment();
        }

        //useful when this activity is gonna be called with makeTransition
        //supportPostponeEnterTransition();
    }

    private void attachAccountGridFragment()
    {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.payment_products_container);
        if (!(fragment instanceof PaymentAccountsFragment))
        {
            Bundle customThemeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            Intent intent = getIntent();
            String selectionString = intent.getStringExtra(SELECTED_REQUEST_CODE_KEY);
            fragment = PaymentAccountsFragment.newInstance(customThemeBundle, Selection.fromStringValue(selectionString));
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.payment_products_container, fragment)
                .commit();
    }

    @Override
    public void onCardClicked(Account account)
    {
        Intent intent = getIntent();
        String selectionString = intent.getStringExtra(SELECTED_REQUEST_CODE_KEY);

        Selection selection = Selection.fromStringValue(selectionString);
        intent.putExtra(Account.TAG, account.toBundle());

        switch (selection)
        {
            case SelectionSource:
            {
                setResult(R.id.transfer_src_selection_succeed, intent);
            }
            break;

            case SelectionDestination:
            {
                setResult(R.id.transfer_dst_selection_succeed, intent);
            }
            break;

            default: //no-op;
                break;
        }

        finish();
    }

    public enum Selection
    {
        SelectionSource ("SelectionSource"),
        SelectionDestination ("SelectionDestination");

        protected final String selection;
        Selection(String method)
        {
            this.selection = method;
        }

        public String getStringValue()
        {
            return this.selection;
        }

        public static Selection fromStringValue(String value)
        {
            if (value == null) return null;

            if (value.equalsIgnoreCase(SelectionSource.getStringValue()))
            {
                return SelectionSource;
            }

            if (value.equalsIgnoreCase(SelectionDestination.getStringValue()))
            {
                return SelectionDestination;
            }
            return null;
        }
    }
}