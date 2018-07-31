package com.tezos.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tezos.core.models.Account;
import com.tezos.core.models.Address;
import com.tezos.core.models.CustomTheme;
import com.tezos.ui.R;
import com.tezos.ui.fragment.PaymentAccountsFragment;
import com.tezos.ui.interfaces.IConfirmCredentialHandler;
import com.tezos.ui.interfaces.IPasscodeHandler;
import com.tezos.ui.utils.ConfirmCredentialHelper;
import com.tezos.ui.utils.ScreenUtils;

/**
 * Created by nfillion on 25/02/16.
 */
public class PaymentAccountsActivity extends AppCompatActivity implements PaymentAccountsFragment.OnCardSelectedListener, IConfirmCredentialHandler, IPasscodeHandler
{
    public static String SELECTED_REQUEST_CODE_KEY = "selectedRequestCodeKey";
    public static String FROM_SCREEN_KEY = "FromScreenKey";

    public static void start(Activity activity, CustomTheme theme, FromScreen fromScreen, Selection selection)
    {
        Intent starter = getStartIntent(activity, theme, fromScreen, selection);
        ActivityCompat.startActivityForResult(activity, starter, TransferFormActivity.TRANSFER_SELECT_REQUEST_CODE, null);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @NonNull
    static Intent getStartIntent(Context context, CustomTheme theme, FromScreen fromScreen, Selection selection)
    {
        Intent starter = new Intent(context, PaymentAccountsActivity.class);
        starter.putExtra(CustomTheme.TAG, theme.toBundle());
        starter.putExtra(SELECTED_REQUEST_CODE_KEY, selection.getStringValue());
        starter.putExtra(FROM_SCREEN_KEY, fromScreen.getStringValue());

        return starter;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AddAddressActivity.ADD_ADDRESS_REQUEST_CODE)
        {
            if (resultCode == R.id.add_address_succeed)
            {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.payment_products_container);
                if (fragment != null)
                {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment_accounts);

        initToolbar();

        if (savedInstanceState == null)
        {
            attachAccountGridFragment();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        launchPasscode();
    }

    @Override
    public void launchPasscode() {
        ScreenUtils.launchPasscode(this);
    }

    private void initToolbar()
    {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);
        CustomTheme theme = CustomTheme.fromBundle(themeBundle);

        toolbar.setBackgroundColor(ContextCompat.getColor(this, theme.getColorPrimaryId()));
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this,
                theme.getColorPrimaryDarkId()));
        try
        {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        catch (Exception e)
        {
            Log.getStackTraceString(e);
        }

        ImageButton mCloseButton = findViewById(R.id.close_button);
        mCloseButton.setColorFilter((ContextCompat.getColor(this, theme.getTextColorPrimaryId())));
        mCloseButton.setOnClickListener(v -> {
            //requests stop in onDestroy.
            finish();
        });

        TextView mTitleBar = findViewById(R.id.barTitle);
        mTitleBar.setTextColor(ContextCompat.getColor(this, theme.getTextColorPrimaryId()));

        String selectionString = getIntent().getStringExtra(PaymentAccountsActivity.SELECTED_REQUEST_CODE_KEY);

        String fromScreenString = getIntent().getStringExtra(FROM_SCREEN_KEY);
        FromScreen fromScreen = FromScreen.fromStringValue(fromScreenString);

        if (fromScreen.equals(FromScreen.FromHome))
        {
            mTitleBar.setText(getString(R.string.addresses_title));
        }
        else if (fromScreen.equals(FromScreen.FromTransfer))
        {
            Selection selection = Selection.fromStringValue(selectionString);
            if (selection.equals(Selection.SelectionAccounts))
            {
                mTitleBar.setText(getString(R.string.select_source_title));
            }
            else if (selection.equals(Selection.SelectionAddresses))
            {
                mTitleBar.setText(getString(R.string.select_destination_title));
            }
            else
            {
                //TODO accounts AND addresses
            }
        }
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
    public void onCardClicked(Address address)
    {
        Intent intent = getIntent();

        String fromScreenString = intent.getStringExtra(FROM_SCREEN_KEY);
        FromScreen fromScreen = FromScreen.fromStringValue(fromScreenString);
        if (fromScreen.equals(FromScreen.FromTransfer))
        {
            String selectionString = intent.getStringExtra(SELECTED_REQUEST_CODE_KEY);

            Selection selection = Selection.fromStringValue(selectionString);
            intent.putExtra(Account.TAG, address.toBundle());

            switch (selection)
            {
                case SelectionAccounts:
                {
                    setResult(R.id.transfer_src_selection_succeed, intent);
                }
                break;

                case SelectionAccountsAndAddresses:
                {
                    setResult(R.id.transfer_dst_selection_succeed, intent);
                }
                break;

                default: //no-op;
                    break;
            }

            finish();
        }
        else
        {
            Bundle themeBundle = getIntent().getBundleExtra(CustomTheme.TAG);

            Intent starter = new Intent(this, AddressDetailsActivity.class);
            starter.putExtra(CustomTheme.TAG, themeBundle);
            starter.putExtra(Address.TAG, address.toBundle());
            ActivityCompat.startActivityForResult(this, starter, -1, null);
        }
    }

    @Override
    public void launchConfirmCredential()
    {
        ConfirmCredentialHelper.launchConfirmCredential(this);
    }

    public enum Selection
    {
        SelectionAccounts ("SelectionAccounts"),
        SelectionAddresses ("SelectionAddresses"),
        SelectionAccountsAndAddresses("SelectionAccountsAndAddresses");

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

            if (value.equalsIgnoreCase(SelectionAccounts.getStringValue()))
            {
                return SelectionAccounts;
            }

            if (value.equalsIgnoreCase(SelectionAddresses.getStringValue()))
            {
                return SelectionAddresses;
            }

            if (value.equalsIgnoreCase(SelectionAccountsAndAddresses.getStringValue()))
            {
                return SelectionAccountsAndAddresses;
            }
            return null;
        }
    }

    public enum FromScreen
    {
        FromHome ("FromHome"),
        FromTransfer ("FromTransfer");

        protected final String selection;
        FromScreen(String method)
        {
            this.selection = method;
        }

        public String getStringValue()
        {
            return this.selection;
        }

        public static FromScreen fromStringValue(String value)
        {
            if (value == null) return null;

            if (value.equalsIgnoreCase(FromHome.getStringValue()))
            {
                return FromHome;
            }

            if (value.equalsIgnoreCase(FromTransfer.getStringValue()))
            {
                return FromTransfer;
            }
            return null;
        }
    }
}