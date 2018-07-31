package com.tezos.core.crypto;

import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.crypto.engines.TEAEngine;

import java.security.PublicKey;
import java.security.SecureRandom;
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

    public static String generatePkh(String mnemonics, String passphrase)
    {

        byte[] src_seed = generateSeed(mnemonics, passphrase);

        byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

        KeyPair key = new KeyPair(seed);
        byte[] sodiumPublicKey = key.getPublicKey().toBytes();

        // then we got the KeyPair from the seed, thanks to sodium.

        // These are our prefixes
        byte[] edpkPrefix = {(byte) 13, (byte) 15, (byte) 37, (byte) 217};
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

        // begins eztz b58encode

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[36];

        System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);

        System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);


        byte[] prefixedPubKeyWithChecksum = new byte[40];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);


        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);

        // ends eztz b58encode

        //create tezos PKHash
        byte[] genericHash = new byte[20];
        try {
            genericHash = CryptoGenericHash.cryptoGenericHash(sodiumPublicKey, genericHash.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] prefixedGenericHash = new byte[23];
        System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
        System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

        firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
        byte[] prefixedPKhashWithChecksum = new byte[27];
        System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

        String pkHash = Base58.encode(prefixedPKhashWithChecksum);

        return pkHash;
    }

    public static String generateSk(String mnemonics, String passphrase)
    {

        /*
        byte[] src_seed = generateSeed(mnemonics, passphrase);

        byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);

        KeyPair key = new KeyPair(seed);
        byte[] sodiumPrivateKey = key.getPrivateKey().toBytes();
        byte[] sodiumPublicKey = key.getPublicKey().toBytes();

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

        String tezosSkString = Base58.encode(prefixedSecKeyWithChecksum);
        return tezosSkString;
        */










        //MnemonicCode mc = new MnemonicCode();
        //String cleanMnemonic = mnemonic.replace("[", "");
        //cleanMnemonic = cleanMnemonic.replace("]", "");

        //List<String> items = Arrays.asList(mnemonic.split("\\s*,\\s*"));
        //byte[] src_seed = mnemonics.toSeed(items, passphrase);


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
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

        // begins eztz b58encode

        // Create Tezos PK.
        byte[] prefixedPubKey = new byte[36];

        System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);

        System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

        byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);


        byte[] prefixedPubKeyWithChecksum = new byte[40];

        System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);


        System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);


        String TezosPkString = Base58.encode(prefixedPubKeyWithChecksum);

        // ends eztz b58encode

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

    public static String generatePk(String mnemonics, String passphrase)
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
        byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

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


    /*
    public static Bundle generateKeys(String mnemonics, String passphrase)
    {
        try
        {

            byte[] src_seed = generateSeed(mnemonics, "");
            byte[] seed = Arrays.copyOfRange(src_seed, 0, 32);
            KeyPair key = new KeyPair(seed);

            return key;
        }
        catch (Exception e)
        {
            //keyBundle = null;
            e.printStackTrace();
        }

        return null;
    }
    */

    /*
    public static JSONObject generateKeys(String mnemonics, String passphrase)
    {
        //Bundle keyBundle;

        try
        {

            //MnemonicCode mc = new MnemonicCode();
            //String cleanMnemonic = mnemonic.replace("[", "");
            //cleanMnemonic = cleanMnemonic.replace("]", "");

            //List<String> items = Arrays.asList(mnemonic.split("\\s*,\\s*"));
            //byte[] src_seed = mnemonics.toSeed(items, passphrase);


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
            byte[] tz1Prefix = {(byte) 6, (byte) 161, (byte) 159};

            // begins eztz b58encode

            // Create Tezos PK.
            byte[] prefixedPubKey = new byte[36];

            System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);

            System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

            byte[] firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedPubKey);


            byte[] prefixedPubKeyWithChecksum = new byte[40];

            System.arraycopy(prefixedPubKey, 0, prefixedPubKeyWithChecksum, 0, 36);


            System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPubKeyWithChecksum, 36, 4);


            String TezosPkString = Base58.encode(prefixedPubKeyWithChecksum);

            // ends eztz b58encode

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

            //create tezos PKHash
            byte[] genericHash = new byte[20];
            genericHash = CryptoGenericHash.cryptoGenericHash(sodiumPublicKey, genericHash.length);

            byte[] prefixedGenericHash = new byte[23];
            System.arraycopy(tz1Prefix, 0, prefixedGenericHash, 0, 3);
            System.arraycopy(genericHash, 0, prefixedGenericHash, 3, 20);

            firstFourOfDoubleChecksum = TzSha256Hash.hashTwiceThenFirstFourOnly(prefixedGenericHash);
            byte[] prefixedPKhashWithChecksum = new byte[27];
            System.arraycopy(prefixedGenericHash, 0, prefixedPKhashWithChecksum, 0, 23);
            System.arraycopy(firstFourOfDoubleChecksum, 0, prefixedPKhashWithChecksum, 23, 4);

            String pkHash = Base58.encode(prefixedPKhashWithChecksum);

            // Builds JSON to return.
            JSONObject payload = new JSONObject();
            payload.put("mnemonic", mnemonics);
            payload.put("passphrase", passphrase);
            payload.put("sk", TezosSkString);
            payload.put("pk", TezosPkString);
            payload.put("pkh", pkHash);
            //payload.put("keypair", );

            keyBundle = new Bundle();
            //keyBundle.putString(PUBLIC_KEY_HASH_KEY, pkHash);

            return payload;
        }
        catch (Exception e)
        {
            //keyBundle = null;
            e.printStackTrace();
        }

        return null;
    }
    */
}
