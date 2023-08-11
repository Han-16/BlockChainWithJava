package Qchain;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

public class Transaction {

    public String transactionId; // this is also the hash of the transaction.
    public PublicKey sender; // senders address/public key.
    public PublicKey reciepient; // Recipients address/public key.
    public float value;
    public byte[] signature; // this is to prevent anybody else from spending funds in our wallet.

    public ArrayList<TransactionInput> inputs = new ArrayList<>();
    public ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0;

    // Constructor
    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.reciepient = to;
        this.value = value;
        this.inputs = inputs;
    }

    // This Calculates the transaction hash which will be used as its Id
    private String calculateHash() {
        sequence ++;
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender)
                + StringUtil.getStringFromKey(reciepient)
                + Float.toString(value) + sequence
        );
    }

    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(reciepient) + Float.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    public boolean processTransaction() {
        if (!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        for (TransactionInput i : inputs) {
            i.UTXO = QChain.UTXOs.get(i.transactionOutputId);
        }

        if (getInputValue() < QChain.minimumTransaction) {
            System.out.println("#Transaction Inputs to small : " + getInputValue());
            return false;
        }

        //generate transaction outputs:
        float leftOver = getInputValue() - value;
        transactionId = calculateHash();

        outputs.add(new TransactionOutput(this.reciepient, value, transactionId));
        outputs.add(new TransactionOutput(this.sender, leftOver, transactionId));

        for (TransactionOutput o : outputs) {
            QChain.UTXOs.put(o.id, o);
        }

        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;
            QChain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    public float getInputValue() {
        float total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue;
            total += i.UTXO.value;
        }
        return total;
    }

    public float getOutputValue() {
        float total = 0;
        for (TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }
}
