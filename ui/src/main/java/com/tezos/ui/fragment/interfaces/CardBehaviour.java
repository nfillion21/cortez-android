/*
(*****************************************************************************)
(*                                                                           *)
(* Open Source License                                                       *)
(* Copyright (c) 2018 Nomadic Development, Inc. <contact@tezcore.com>        *)
(*                                                                           *)
(* Permission is hereby granted, free of charge, to any person obtaining a   *)
(* copy of this software and associated documentation files (the "Software"),*)
(* to deal in the Software without restriction, including without limitation *)
(* the rights to use, copy, modify, merge, publish, distribute, sublicense,  *)
(* and/or sell copies of the Software, and to permit persons to whom the     *)
(* Software is furnished to do so, subject to the following conditions:      *)
(*                                                                           *)
(* The above copyright notice and this permission notice shall be included   *)
(* in all copies or substantial portions of the Software.                    *)
(*                                                                           *)
(* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR*)
(* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  *)
(* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL   *)
(* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER*)
(* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING   *)
(* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER       *)
(* DEALINGS IN THE SOFTWARE.                                                 *)
(*                                                                           *)
(*****************************************************************************)
*/

package com.tezos.ui.fragment.interfaces;

import android.content.Context;
import com.google.android.material.textfield.TextInputLayout;
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
