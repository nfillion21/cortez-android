package com.tezos.core.crypto;

import android.text.TextUtils;

import org.bitcoinj.core.Sha256Hash;
import org.json.JSONObject;

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

    private byte[] generateSeed(String mnemonics, String passphrase)
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

    public JSONObject generateKeys(String mnemonics, String passphrase)
    {
        JSONObject response = new JSONObject();

        try
        {
            passphrase = "";

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

            // on recupere les 4 premiers bytes dans le prefix (le prefix entier en fait)
            System.arraycopy(edpkPrefix, 0, prefixedPubKey, 0, 4);

            // et dans les 32 bytes d'apres, on place les 32 premiers bytes de la sodium public key
            System.arraycopy(sodiumPublicKey, 0, prefixedPubKey, 4, 32);

            // prefixedPubKey is 36 bytes full.

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
            response.put("mnemonic", mnemonics);
            response.put("passphrase", passphrase);
            response.put("sk", TezosSkString);
            response.put("pk", TezosPkString);
            response.put("pkh", pkHash);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return response;
    }
}
