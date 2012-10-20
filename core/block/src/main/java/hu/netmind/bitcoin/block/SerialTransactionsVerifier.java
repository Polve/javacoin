package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Script;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.VerificationException;
import it.nibbles.bitcoin.utils.BtcUtil;

/**
 *
 * @author Alessandro Polverini
 */
public class SerialTransactionsVerifier extends BlockTransactionsVerifier
{

   public SerialTransactionsVerifier(BlockChainLinkStorage linkStorage, ScriptFactory scriptFactory, boolean simplifiedVerification)
   {
      super(linkStorage, scriptFactory, simplifiedVerification);
      logger.info("Serial Transaction Verifier instantiated");
   }

   @Override
   public long verifyBlockTransactions(BlockChainLink previousLink, Block block) throws VerificationException
   {
      long inValue = 0;
      long outValue = 0;
      for (Transaction tx : block.getTransactions())
      {
         // Validate without context
         tx.validate();
         // Checks 16.1.1-7: Verify only if this is supposed to be a full node
         long localInValue;
         long localOutValue = 0;
         if ((!simplifiedVerification) && (!tx.isCoinbase()))
         {
            localInValue = verifyTransaction(previousLink, block, tx);
            for (TransactionOutput out : tx.getOutputs())
            {
               localOutValue += out.getValue();
            }
            inValue += localInValue;
            outValue += localOutValue;
            // Check 16.1.6: Using the referenced output transactions to get
            // input values, check that each input value, as well as the sum, are in legal money range
            // Check 16.1.7: Reject if the sum of input values < sum of output values
            if (localInValue < localOutValue)
               throw new VerificationException("more money spent (" + localOutValue + ") then available (" + localInValue + ") in transaction: " + tx);
         }
      }
      return inValue - outValue;
   }

   /**
    * Verify that a transaction is valid according to sub-rules applying to the
    * block tree.
    *
    * @param link The link that represents the branch if the new transaction.
    * @param block The block we're trying to add.
    * @return The total value of the inputs after verification.
    */
   protected long verifyTransaction(BlockChainLink link, Block block, Transaction tx)
      throws VerificationException
   {
      long value = 0;
      for (TransactionInput in : tx.getInputs())
      {
         // Check 16.1.1: For each input, look in the [same] branch to find the
         // referenced output transaction. Reject if the output transaction is missing for any input.
         Transaction outTx = null;
         BlockChainLink outLink = linkStorage.getPartialClaimedLink(link, in);
         if (outLink != null) // Check in chain before
            outTx = getTransaction(outLink.getBlock(), in.getClaimedTransactionHash());
         if (outTx == null) // Check in this block if not yet found
            outTx = getTransaction(block, in.getClaimedTransactionHash());
         if (outTx == null)
            throw new VerificationException("transaction output not found for input: " + in);
         // Check 16.1.2: For each input, if we are using the nth output of the
         // earlier transaction, but it has fewer than n+1 outputs, reject.
         if (outTx.getOutputs().size() <= in.getClaimedOutputIndex())
            throw new VerificationException("transaction output index for input is out of range: "
               + (in.getClaimedOutputIndex() + 1) + " vs. " + outTx.getOutputs().size());
         // Check 16.1.3: For each input, if the referenced output transaction is coinbase,
         // it must have at least COINBASE_MATURITY confirmations; else reject.
         if ((outTx.isCoinbase()) && (outLink.getHeight() + BlockChainImpl.COINBASE_MATURITY > link.getHeight()))
            throw new VerificationException("input (" + in + ") referenced coinbase transaction "
               + outTx + " which was not mature enough (only " + (link.getHeight() - outLink.getHeight() + 1) + " blocks before)");
         // Check 16.1.4: Verify crypto signatures for each input; reject if any are bad
         TransactionOutput out = outTx.getOutputs().get(in.getClaimedOutputIndex());
         value += out.getValue(); // Remember value that goes in from this out
         try
         {
            Script script = scriptFactory.createScript(in.getSignatureScript(), out.getScript());
            if (!script.execute(in))
               throw new VerificationException("verification script for input " + in + " returned 'false' for verification, script was: "
                  + script + " in tx " + BtcUtil.hexOut(tx.getHash()));
         } catch (ScriptException e)
         {
            throw new VerificationException("verification script for input " + in + " in tx " + BtcUtil.hexOut(tx.getHash()) + " failed to execute", e);
         }
         // Check 16.1.5: For each input, if the referenced output has already been
         // spent by a transaction in the [same] branch, reject
         if (linkStorage.outputClaimedInSameBranch(link, in))
            throw new VerificationException("Block: " + BtcUtil.hexOut(block.getHash()) + " Tx: " + BtcUtil.hexOut(tx.getHash())
               + " output claimed by " + in + " is already claimed in another block of the same branch: "
               + BtcUtil.hexOut(linkStorage.getClaimerLink(link, in).getBlock().getHash()));
      }
      return value;
   }
}
