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

package com.tezos.core.errors;

/**
 * Created by nfillion on 24/02/16.
 */
public class Errors {

    public static final String TAG = "Errors";

    public static final String HTTPOtherDescription = "An unknown error occurred while attempting to make the HTTP request.";
    public static final String HTTPNetworkUnavailableDescription = "The network is unavailable.";
    public static final String HTTPConfigDescription = "There's a remote/local configuration error.";
    //public static final String HTTPConnectionFailedDescription = "The request has been interrupted. The server may have received the request.";
    public static final String HTTPClientDescription = "Wrong parameters have been sent to the server.";
    public static final String HTTPServerDescription = "There's a server side error.";

    //public static final String HTTPPlainResponseKey = "HPFErrorCodeHTTPPlainResponseKey";
    //public static final String HTTPParsedResponseKey = "HPFErrorCodeHTTPParsedResponseKey";
    //public static final String HTTPStatusCodeKey = "HPFErrorCodeHTTPStatusCodeKey";

    //public static final String HPIDescriptionKey = "HPFErrorCodeAPIDescriptionKey";
    //public static final String HPIMessageKey = "HPFErrorCodeAPIMessageKey";
    //public static final String HPICodeKey = "HPFErrorCodeAPICodeKey";

    public enum Code {

        /// Unknown network/HTTP error
        HTTPOther(0),

        /// Network is unavailable
        HTTPNetworkUnavailable(1),

        /// Config error (such as SSL, bad URL, etc.)
        HTTPConfig(2),

        /// The connection has been interupted
        //HTTPConnectionFailed(3),

        /// HTTP client error (400)
        HTTPClient(3),

        /// HTTP client error (typically a 500 error)
        HTTPServer(4);

        protected final Integer code;

        Code(Integer code) {
            this.code = code;
        }

        public Integer getIntegerValue() {
            return this.code;
        }
    }
}
