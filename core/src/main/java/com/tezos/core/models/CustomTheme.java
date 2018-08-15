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

package com.tezos.core.models;

import android.os.Bundle;

import com.tezos.core.mapper.AbstractMapper;
import com.tezos.core.mapper.interfaces.BundleMapper;
import com.tezos.core.serialization.AbstractSerializationMapper;

/**
 * Created by nfillion on 29/03/16.
 */
public class CustomTheme extends AbstractModel
{
    private int colorPrimaryId;
    private int colorPrimaryDarkId;
    private int textColorPrimaryId;

    public static final String TAG = "theme";

    public CustomTheme() {
    }

    public static CustomTheme fromBundle(Bundle bundle)
    {
        CustomThemeMapper mapper = new CustomThemeMapper(bundle);
        return mapper.mappedObjectFromBundle();
    }

    public Bundle toBundle()
    {
        CustomThemeSerializationMapper mapper = new CustomThemeSerializationMapper(this);
        return mapper.getSerializedBundle();
    }

    public CustomTheme(int colorPrimaryId, int colorPrimaryDarkId, int textColorPrimaryId)
    {
        this.colorPrimaryId = colorPrimaryId;
        this.colorPrimaryDarkId = colorPrimaryDarkId;
        this.textColorPrimaryId = textColorPrimaryId;
    }

    public int getColorPrimaryId() {
        return colorPrimaryId;
    }

    public void setColorPrimaryId(int colorPrimaryId) {
        this.colorPrimaryId = colorPrimaryId;
    }

    public int getColorPrimaryDarkId() {
        return colorPrimaryDarkId;
    }

    public void setColorPrimaryDarkId(int colorPrimaryDarkId) {
        this.colorPrimaryDarkId = colorPrimaryDarkId;
    }

    public int getTextColorPrimaryId() {
        return textColorPrimaryId;
    }

    public void setTextColorPrimaryId(int textColorPrimaryId) {
        this.textColorPrimaryId = textColorPrimaryId;
    }

    protected static class CustomThemeSerializationMapper extends AbstractSerializationMapper {

        protected CustomThemeSerializationMapper(CustomTheme customTheme) {
            super(customTheme);
        }

        @Override
        protected String getQueryString() {

            return null;
        }

        @Override
        protected Bundle getSerializedBundle() {

            return super.getSerializedBundle();
        }
    }

    protected static class CustomThemeMapper extends AbstractMapper {
        public CustomThemeMapper(Bundle object) {
            super(object);
        }

        @Override
        protected boolean isValid() {

            if (this.getBehaviour() instanceof BundleMapper) {

                return true;
            }

            return true;
        }

        @Override
        protected CustomTheme mappedObjectFromJSON() {

            return null;
        }

        @Override
        protected CustomTheme mappedObjectFromBundle() {

            CustomTheme customTheme = new CustomTheme();

            customTheme.setColorPrimaryId(this.getIntegerForKey("colorPrimary"));
            customTheme.setColorPrimaryDarkId(this.getIntegerForKey("colorPrimaryDark"));
            customTheme.setTextColorPrimaryId(this.getIntegerForKey("colorTextPrimary"));

            return customTheme;
        }
    }
}
