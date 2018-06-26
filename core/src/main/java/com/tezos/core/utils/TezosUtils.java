package com.tezos.core.utils;

import java.security.SecureRandom;

import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

public class TezosUtils {

    public static String generateMnemonic()
    {
        String result = "";

        /*
        try
        {
            MnemonicCode mc = new MnemonicCode();
            byte[] bytes = new byte[20];
            new java.util.Random().nextBytes(bytes);
            List<String> code = mc.toMnemonic(bytes);
            result = code.toString();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        */

        return result;
    }

    public static String generateNovaMnemonic()
    {
        /*
        StringBuilder sb = new StringBuilder();
        byte[] entropy = new byte[Words.TWENTY_FOUR.byteLength()];
        new SecureRandom().nextBytes(entropy);

        new MnemonicGenerator(English.INSTANCE)
                .createMnemonic(entropy, sb::append);
        //System.out.println(sb.toString());

        //MnemonicGenerator.Target target = sb::append;

        return sb.toString();
        */

        return "orchard roof outside sustain depth robot inherit across coil hospital gravity guilt feel napkin hire tank yard mandate theme learn hollow gravity permit undo";
    }

    public static byte[] generateNovaSeed(String mnemonic)
    {
        final byte[] seed = new SeedCalculator().calculateSeed(mnemonic, "");

        //ExtendedPrivateKey root = ExtendedPrivateKey.fromSeed(seed, Bitcoin.MAIN_NET);
        //ExtendedPrivateKey root2 = ExtendedPrivateKey.fromSeed(seed, Bitcoin.MAIN_NET);
        return seed;
    }
}
