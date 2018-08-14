package com.tezos.ui.fragment.interfaces;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by nfillion on 20/05/16.
 */
public interface ICardBehaviour {

    void updateForm(EditText cardNumber, EditText cardCVV, EditText cardExpiry, TextInputLayout securityCodeLayout, TextView securityCodeInfoTextview, ImageView securityCodeInfoImageview, LinearLayout switchLayout, boolean networked, Context context);
    boolean isSecurityCodeValid(EditText cardCVV);
    boolean hasSecurityCode();
    boolean hasSpaceAtIndex(Integer index, Context context);
    String getProductCode();
    boolean isCardStorageEnabled();
}
