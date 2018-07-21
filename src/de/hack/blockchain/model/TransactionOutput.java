package de.hack.blockchain.model;

import de.hack.blockchain.utils.SecurityUtils;

import java.security.PublicKey;

public class TransactionOutput {
    public String id;
    public PublicKey recipient;
    public float value; //the amount of coins they own
    public String parentTransactionId; //the id of the transaction this output was created in

    public TransactionOutput(PublicKey recipient, float value, String parentTransactionId) {
        this.id = SecurityUtils.applySha256(SecurityUtils.getStringFromKey(recipient)+ Float.toString(value)+parentTransactionId);
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
    }

    public boolean isMine(PublicKey publicKey){
        return publicKey == recipient;
    }
}
