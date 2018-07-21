package de.hack.blockchain.utils;

import de.hack.blockchain.model.Transaction;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SecurityUtils {

    private static final String SIGNATURE_ALGORITHM = "ECDSA";
    private static final String SIGNATURE_PROVIDER = "BC";
    private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String ECGENPARAM_STDNAME = "prime192v1";
    private static final String MSGDIGEST_ALGORITHM = "SHA-256";
    private static final String CHARSET_NAME = "UTF-8";

    public static String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(MSGDIGEST_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(CHARSET_NAME));
            StringBuilder hexString = new StringBuilder();
            for (byte aHash : hash) {
                String hex = Integer.toHexString(0xff & aHash);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static KeyPair generateKeyPair() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(SIGNATURE_ALGORITHM, SIGNATURE_PROVIDER);
            SecureRandom random = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(ECGENPARAM_STDNAME);
            keyGen.initialize(ecSpec, random);
            keyPair = keyGen.generateKeyPair();
        }
        catch (Exception e){
            throw new RuntimeException(e);
        }
        return keyPair;
    }

    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static byte[] applyECDSASig(PrivateKey privateKey, String input){
        Signature dsa;
        byte[] output = new byte[0];
        try{
            dsa = Signature.getInstance(SIGNATURE_ALGORITHM, SIGNATURE_PROVIDER);
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        }
        catch (Exception e){
            throw  new RuntimeException(e);
        }
        return output;
    }

    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature){
        try{
            Signature ecdsaVerify = Signature.getInstance(SIGNATURE_ALGORITHM, SIGNATURE_PROVIDER);
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        }
        catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    //Tacks in array of transactions and returns a merkle root.
    public static String getMerkleRoot(List<Transaction> transactions) {
        int count = transactions.size();
        List<String> previousTreeLayer = new ArrayList<>();
        for(Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.transactionId);
        }
        List<String> treeLayer = previousTreeLayer;
        while(count > 1) {
            treeLayer = new ArrayList<>();
            for(int i=1; i < previousTreeLayer.size(); i++) {
                treeLayer.add(applySha256(previousTreeLayer.get(i-1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }
        String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
        return merkleRoot;
    }

    //Returns difficulty string target, to compare to hash. eg difficulty of 5 will return "00000"
    public static String getDificultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }
}