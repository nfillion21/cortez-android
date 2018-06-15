package com.tezos.android.fragment.interfaces;

import android.content.Context;
import android.support.design.widget.TextInputLayout;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by nfillion on 22/05/16.
 */

public class CardBehaviour {

    private ICardBehaviour cardBehaviour;

    public CardBehaviour() {
    }

    public void updatePaymentProduct(String paymentProductCode) {
    }

    public ICardBehaviour getCardBehaviour() {
        return cardBehaviour;
    }

    public void setCardBehaviour(ICardBehaviour cardBehaviour) {
        this.cardBehaviour = cardBehaviour;
    }

    public void updateForm(EditText cardNumber, EditText cardCVV, EditText cardExpiry, TextInputLayout securityCodeLayout, TextView securityCodeInfoTextview, ImageView securityCodeInfoImageview, LinearLayout switchLayout, boolean networked, Context context) {

        this.getCardBehaviour().updateForm(cardNumber, cardCVV, cardExpiry, securityCodeLayout, securityCodeInfoTextview, securityCodeInfoImageview, switchLayout, networked, context);
    }

    public boolean isSecurityCodeValid(EditText cardCVV) {

        return this.getCardBehaviour().isSecurityCodeValid(cardCVV);
    }

    public String getProductCode() {

        return this.getCardBehaviour().getProductCode();
    }

    public boolean hasSecurityCode() {

        return this.getCardBehaviour().hasSecurityCode();
    }

    public boolean hasSpaceAtIndex(Integer index, Context context) {
        return this.getCardBehaviour().hasSpaceAtIndex(index, context);
    }
}
