import com.google.gson.GsonBuilder;
import de.hack.blockchain.NoobChain;
import de.hack.blockchain.model.*;
import de.hack.blockchain.service.BlockService;
import de.hack.blockchain.utils.SecurityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public Main(){

    }

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static List<Block> blockchain = new ArrayList<>();
    public static BlockService blockService = new BlockService();

    public static Transaction genesisTransaction;

    private static int difficulty = 1;
    private static Wallet walletA;
    private static Wallet walletB;

    public static void main(String[] args) {

        walletA = new Wallet();
        walletB = new Wallet();

        Wallet coinbase = new Wallet();

        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0";
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId));
        NoobChain.UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        System.out.println("Creating and Mining Genesis block... ");

        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        System.out.println("Starting testing");

        //testing
        Block block1 = new Block(genesis.hash);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("\nWalletA is Attempting to send funds (40) to WalletB...");
        block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f));
        addBlock(block1);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block2 = new Block(block1.hash);
        System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
        block2.addTransaction(walletA.sendFunds(walletB.publicKey, 1000f));
        addBlock(block2);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        Block block3 = new Block(block2.hash);
        System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
        block3.addTransaction(walletB.sendFunds( walletA.publicKey, 20));
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        isChainValid();
    }

    private static void createWallets() {
        walletA = new Wallet();
        walletB = new Wallet();

        System.out.println("Private and public keys");
        System.out.println(SecurityUtils.getStringFromKey(walletA.privateKey));
        System.out.println(SecurityUtils.getStringFromKey(walletA.publicKey));

        Transaction transaction =new Transaction(walletA.publicKey, walletB.publicKey, 5, null);
        transaction.signature = transaction.generateSignature(walletA.privateKey);

        System.out.println("Is signature verified");
        System.out.println(transaction.verifySignature());
    }

    private static void generateBlockchain() {
        blockchain.add(new Block("0"));
        System.out.println("Trying to Mine block 1... ");
        blockchain.get(0).mineBlock(difficulty);

        for (int i = 1; i < 10; i++) {
            executeMine(i);
        }

        System.out.println("\nBlockchain is Valid: " + blockService.isChainValid(blockchain, difficulty));

        String blockchainJson = new GsonBuilder().setPrettyPrinting().create().toJson(blockchain);
        System.out.println("\nThe block chain: ");
        System.out.println(blockchainJson);
    }

    private static void executeMine(int i) {
        blockchain.add(new Block(blockchain.get(blockchain.size() - 1).hash));
        System.out.printf("Trying to Mine block %d... \n", i);
        blockchain.get(i).mineBlock(difficulty);
    }

    public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //compare registered hash and calculated hash:
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return false;
            }
            //compare previous hash and registered previous hash
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            //check if hash is solved
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            //loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
                    System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
                    return false;
                }
                if( currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }

            }

        }
        System.out.println("Blockchain is valid");
        return true;
    }

    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }


}
