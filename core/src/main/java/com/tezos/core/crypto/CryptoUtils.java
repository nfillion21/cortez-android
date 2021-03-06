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

package com.tezos.core.crypto;

import android.text.TextUtils;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.List;

import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Validation.InvalidChecksumException;
import io.github.novacrypto.bip39.Validation.InvalidWordCountException;
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException;
import io.github.novacrypto.bip39.Validation.WordNotFoundException;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

import static org.libsodium.jni.NaCl.sodium;

public class CryptoUtils
{
    public static String generateMnemonics()
    {
        StringBuilder sb = new StringBuilder();
        byte[] entropy = new byte[Words.TWENTY_FOUR.byteLength()];
        new SecureRandom().nextBytes(entropy);

        new MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append);

        return sb.toString();
    }

    public static byte[] generateSeed(String mnemonics, String passphrase)
    {
        final byte[] seed = new SeedCalculator().calculateSeed(mnemonics, passphrase);
        return seed;
    }

    public static boolean validateMnemonics(String mnemonics)
    {

        boolean isValid;
        boolean isCatched = false;

        try
        {
            MnemonicValidator.ofWordList(English.INSTANCE).validate(mnemonics);
        }
        catch (InvalidChecksumException e)
        {
            e.printStackTrace();
            isCatched = true;
        }
        catch (InvalidWordCountException e)
        {
            e.printStackTrace();
            isCatched = true;
        }
        catch (WordNotFoundException e)
        {
            e.printStackTrace();
            isCatched = true;
        }
        catch (UnexpectedWhiteSpaceException e)
        {
            e.printStackTrace();
            isCatched = true;
        }
        finally
        {
            isValid = !isCatched;
        }

        return isValid;
    }

    public static boolean validateMnemonics(List<String> words)
    {
        boolean isValid;

        if (words == null || words.contains(null))
        {
            isValid = false;
        }
        else
        {
            String separatedWords = TextUtils.join(" ", words);
            isValid = validateMnemonics(separatedWords);
        }

        return isValid;
    }
    /*
    public static JSONObject generateKeys(String mnemonics)
    {
        //TODO need to protect the private key
        return generateKeys(mnemonics, "");
    }
    */

    public static String generatePkh(byte[] sodiumPublicKey)
    {
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

        //create tezos PKHash
        byte[] genericHash = new byte[20];
        try
        {
            genericHash = CryptoGenericHash.cryptoGenericHash(sodiumPublicKey, genericHash.length);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String generatePkh(String mnemonics, String passphrase)
    {

        byte[] src_seed = generateSeed(mnemonics, passphrase);

        byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

        KeyPair key = new KeyPair(seed);
        byte[] sodiumPublicKey = key.getPublicKey().toBytes();

        // then we got the KeyPair from the seed, thanks to sodium.


        //create tezos PKHash
        byte[] genericHash = new byte[20];
        try {
            genericHash = CryptoGenericHash.cryptoGenericHash(sodiumPublicKey, genericHash.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // These are our prefixes
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String generatePkhTz3(byte[] publicKey)
    {
        // then we got the KeyPair from the seed, thanks to sodium.

        // These are our prefixes
        byte[] tz3Prefix = {(byte) 6, (byte) 161, (byte) 164};

        //create tezos PKHash
        /*
        byte[] genericHash = new byte[20];
        try {
            genericHash = CryptoGenericHash.cryptoGenericHash(publicKey, genericHash.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */


        byte[] payload_hash = new byte[20];
        sodium().crypto_generichash(payload_hash, payload_hash.length, publicKey, publicKey.length, new byte[]{0}, 0);


        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz3Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(payload_hash, 0, prefixedGenericHash, 3, 20);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String generateP2Sig(byte[] signature)
    {
        // then we got the KeyPair from the seed, thanks to sodium.

        // These are our prefixes
        byte[] p2pkPrefix = {(byte) 54, (byte) 240, (byte) 44, (byte) 52};

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[signature.length + p2pkPrefix.length];

        System.arraycopy(p2pkPrefix, 0, prefixedPubKey, 0, p2pkPrefix.length);
        System.arraycopy(signature, 0, prefixedPubKey, p2pkPrefix.length, signature.length);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
        byte[] prefixedPubKeyWithChecksum = new byte[signature.length + p2pkPrefix.length*2];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, signature.length + p2pkPrefix.length);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, signature.length + p2pkPrefix.length, p2pkPrefix.length);

        String p2pkString = Base58.encode(prefixedPubKeyWithChecksum);
        return p2pkString;
    }

    public static String generateEDSig(byte[] signature)
    {
        // then we got the KeyPair from the seed, thanks to sodium.

        // These are our prefixes
        byte[] edPrefix = {(byte) 9, (byte) 245, (byte) 205, (byte) 134, (byte) 18};

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[signature.length + edPrefix.length];

        System.arraycopy(edPrefix, 0, prefixedPubKey, 0, edPrefix.length);
        System.arraycopy(signature, 0, prefixedPubKey, edPrefix.length, signature.length);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
        byte[] prefixedPubKeyWithChecksum = new byte[prefixedPubKey.length + 4];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, signature.length + edPrefix.length);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, signature.length + edPrefix.length, 4);

        String edString = Base58.encode(prefixedPubKeyWithChecksum);
        return edString;
    }

    public static String generatePk(String mnemonics, String passphrase)
    {
        byte[] src_seed = generateSeed(mnemonics, passphrase);
        byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

        // ok we got the seed.

        KeyPair key = new KeyPair(seed);
        byte[] sodiumPublicKey = key.getPublicKey().toBytes();

        // then we got the KeyPair from the seed, thanks to sodium.

        // These are our prefixes
        byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};

        // begins eztz b58encode

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[36];

        System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
        System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
        byte[] prefixedPubKeyWithChecksum = new byte[40];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

        String tezosPkString = Base58.encode(prefixedPubKeyWithChecksum);
        return tezosPkString;
    }

    public static String genericHashToPkh(byte[] genericHash)
    {
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String genericHashToPk(byte[] genericHash)
    {
        // These are our prefixes
        byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};

        // begins eztz b58encode

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[36];

        System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);
        System.arraycopy(genericHash, 0, prefixedPubKey, 4, 32);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
        byte[] prefixedPubKeyWithChecksum = new byte[40];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

        String tezosPkString = Base58.encode(prefixedPubKeyWithChecksum);
        return tezosPkString;
    }

    public static String generateP2Pk(byte[] publicKey)
    {
        // then we got the KeyPair from the seed, thanks to sodium.

        // These are our prefixes
        byte[] p2pkPrefix = {(byte) 3, (byte) 178, (byte) 139, (byte) 127};

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[publicKey.length + p2pkPrefix.length];

        System.arraycopy(p2pkPrefix, 0, prefixedPubKey, 0, p2pkPrefix.length);
        System.arraycopy(publicKey, 0, prefixedPubKey, p2pkPrefix.length, publicKey.length);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
        byte[] prefixedPubKeyWithChecksum = new byte[publicKey.length + p2pkPrefix.length*2];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, publicKey.length + p2pkPrefix.length);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, publicKey.length + p2pkPrefix.length, p2pkPrefix.length);

        String p2pkString = Base58.encode(prefixedPubKeyWithChecksum);
        return p2pkString;
    }

    public static String genericHashToP2pk(byte[] genericHash)
    {

        // These are our prefixes
        //byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};
        byte[] p2pkPrefix = {(byte) 3, (byte) 178, (byte) 139, (byte) 127};

        // begins eztz b58encode

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[36];

        System.arraycopy(p2pkPrefix, 0, prefixedPubKey, 0, 4);
        System.arraycopy(genericHash, 0, prefixedPubKey, 4, 32);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);
        byte[] prefixedPubKeyWithChecksum = new byte[40];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

        String tezosPkString = Base58.encode(prefixedPubKeyWithChecksum);
        return tezosPkString;
    }


    public static String genericHashToPkhTz3(byte[] genericHash)
    {
        // These are our prefixes
        byte[] tz3Prefix = {(byte) 6, (byte) 161, (byte) 164};

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz3Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String genericHashToPkhTz2(byte[] genericHash)
    {
        // These are our prefixes
        byte[] tz3Prefix = {(byte) 6, (byte) 161, (byte) 161};

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz3Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String genericHashToKT(byte[] genericHash)
    {
        byte[] tz1Prefix = {(byte) 2, (byte) 90, (byte) 121};

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String generateSk(String mnemonics, String passphrase)
    {
        byte[] src_seed = generateSeed(mnemonics, passphrase);

        byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

        // ok we got the seed.

        KeyPair key = new KeyPair(seed);
        byte[] sodiumPublicKey = key.getPublicKey().toBytes();
        byte[] sodiumPrivateKey = key.getPrivateKey().toBytes();

        // then we got the KeyPair from the seed, thanks to sodium.

        // These are our prefixes
        byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};
        byte[] edskPrefix = {(byte) 43, (byte) 246, (byte) 78, (byte) 7};

        // begins eztz b58encode

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[36];

        System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);

        System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);


        byte[] prefixedPubKeyWithChecksum = new byte[40];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);


        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

        // Create Tezos SK.
        byte[] prefixedSecKey = new byte[68];
        System.arraycopy(edskPrefix, 0, prefixedSecKey, 0, 4);
        System.arraycopy(sodiumPrivateKey, 0, prefixedSecKey, 4, 64);

        firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedSecKey);
        byte[] prefixedSecKeyWithChecksum = new byte[72];
        System.arraycopy(prefixedSecKey, 0, prefixedSecKeyWithChecksum, 0, 68);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedSecKeyWithChecksum, 68, 4);

        //ici il faut virer le prefix, pour atteindre 32 bytes de data.

        String TezosSkString = Base58.encode(prefixedSecKeyWithChecksum);
        return TezosSkString;
    }
}
