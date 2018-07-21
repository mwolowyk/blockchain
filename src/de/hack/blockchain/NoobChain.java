package de.hack.blockchain;

import de.hack.blockchain.model.TransactionOutput;

import java.util.HashMap;
import java.util.Map;

public class NoobChain {
    public static Map<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>(); //list of all unspent transactions.
    public static float minimumTransaction = 0.1f;
}
