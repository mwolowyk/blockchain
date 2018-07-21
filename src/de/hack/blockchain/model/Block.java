package de.hack.blockchain.model;


import de.hack.blockchain.utils.SecurityUtils;
import lombok.Data;

import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class Block {

    public String hash;
    public String previousHash;
    private String data;
    public String merkleRoot;
    public List<Transaction> transactions = new ArrayList<>(); //our data will be a simple message.

    private long timeStamp;
    private int nonce;

    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.timeStamp = new Date().getTime();

        this.hash = calculateHash();
    }

    public String calculateHash(){
        String calculatedhash = SecurityUtils.applySha256(
                previousHash +
                        Long.toString(timeStamp) +
                        Integer.toString(nonce) +
                        merkleRoot
        );
        return calculatedhash;
    }

    public void mineBlock(int difficulty) {
        long start1 = System.currentTimeMillis();

        merkleRoot = SecurityUtils.getMerkleRoot(transactions);

        String target = SecurityUtils.getDificultyString(difficulty);
        while(!hash.substring(0, difficulty).equals(target)){
            nonce ++;
            hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + hash);
        System.out.printf("Took %d ms\n", System.currentTimeMillis() - start1);
    }

    public boolean addTransaction(Transaction transaction){
        if(transaction == null) return false;
        if((previousHash != "0")) {
            if(!transaction.processTransaction()){
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }
        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }
}
