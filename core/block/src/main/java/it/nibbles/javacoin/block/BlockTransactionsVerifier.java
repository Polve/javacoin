package it.nibbles.javacoin.block;

import it.nibbles.javacoin.BitcoinException;
import it.nibbles.javacoin.Block;
import it.nibbles.javacoin.Script;
import it.nibbles.javacoin.ScriptException;
import it.nibbles.javacoin.ScriptFactory;
import it.nibbles.javacoin.Transaction;
import it.nibbles.javacoin.TransactionInput;
import it.nibbles.javacoin.TransactionOutput;
import it.nibbles.javacoin.VerificationException;
import it.nibbles.javacoin.utils.BtcUtil;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessandro Polverini <alex@nibbles.it>
 */
public abstract class BlockTransactionsVerifier
{

   protected static final Logger logger = LoggerFactory.getLogger(BlockTransactionsVerifier.class);
   protected static KnownExceptions transactionExceptions = new KnownExceptions();
   protected BlockChainLinkStorage linkStorage;
   protected ScriptFactory scriptFactory;
   protected boolean simplifiedVerification;

   public BlockTransactionsVerifier(BlockChainLinkStorage linkStorage, ScriptFactory scriptFactory, boolean simplifiedVerification)
   {
      this.linkStorage = linkStorage;
      this.scriptFactory = scriptFactory;
      this.simplifiedVerification = simplifiedVerification;
   }

   public abstract long verifyBlockTransactions(BlockChainLink previousLink, Block block)
      throws VerificationException, BitcoinException;

   /**
    * Search for a specific transaction in a block.
    *
    * @param block The block to search transaction in.
    * @param txHash The transaction hash to match.
    * @return The transaction in the block which has the given hash, null
    * otherwise.
    */
   public Transaction getTransaction(Block block, byte[] txHash)
   {
      for (Transaction txCandidate : block.getTransactions())
         if (Arrays.equals(txCandidate.getHash(), txHash))
            return txCandidate;
      return null;
   }

   /**
    * Verify that a transaction is valid according to sub-rules applying to the
    * block tree.
    *
    * @param previousLink The link that represents the branch if the new transaction.
    * @param block The block we're trying to add.
    * @return The total value of the inputs after verification.
    */
   public long verifyTransaction(BlockChainLink previousLink, Block block, Transaction tx)
      throws VerificationException
   {
      long value = 0;
      for (TransactionInput in : tx.getInputs())
      {
         // Check 16.1.1: For each input, look in the [same] branch to find the
         // referenced output transaction. Reject if the output transaction is missing for any input.
         Transaction outTx = null;
         BlockChainLink outLink = linkStorage.getPartialClaimedLink(previousLink, in);
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
         if (outTx.isCoinbase() && (outLink.getHeight() + BlockChainImpl.COINBASE_MATURITY > (previousLink.getHeight()+1)))
            throw new VerificationException("input (" + in + ") referenced coinbase transaction "
               + outTx + " in block at height "+outLink.getHeight()+" which was not mature enough (only " + (previousLink.getHeight() - outLink.getHeight() + 1) + " blocks before)"
               +" current link height is "+previousLink.getHeight());
         // Check 16.1.4: Verify crypto signatures for each input; reject if any are bad
         TransactionOutput out = outTx.getOutputs().get(in.getClaimedOutputIndex());
         value += out.getValue(); // Remember value that goes in from this out

         if (!transactionExceptions.isExempt(tx.getHash(), ValidationCategory.ScriptValidation))
         {
            Script script = scriptFactory.createScript(in.getSignatureScript(), out.getScript());
            try
            {
               if (!script.execute(in))
               {
                  logger.warn("FALSE executing script on " + tx + "\n"
                     + "inScript:  " + in.getSignatureScript() + " outScript:  " + out.getScript() + " bip16: " + script.isValidBip16());
                  throw new VerificationException("verification script for input " + in + " returned 'false' for verification, script was: "
                     + script + " in tx " + BtcUtil.hexOut(tx.getHash()));
               }
            } catch (ScriptException e)
            {
               logger.warn("ScriptException executing script on " + tx + "\n"
                  + "inScript:  " + in.getSignatureScript() + " outScript:  " + out.getScript() + " bip16: " + script.isValidBip16());
               throw new VerificationException("verification script for input " + in + " in tx " + BtcUtil.hexOut(tx.getHash()) + " failed to execute", e);
            }
         }
         // Check 16.1.5: For each input, if the referenced output has already been
         // spent by a transaction in the [same] branch, reject
         if (linkStorage.outputClaimedInSameBranch(previousLink, in))
            throw new VerificationException("Block: " + BtcUtil.hexOut(block.getHash()) + " Tx: " + BtcUtil.hexOut(tx.getHash())
               + " output claimed by " + in + " is already claimed in another block of the same branch: "
               + BtcUtil.hexOut(linkStorage.getClaimerLink(previousLink, in).getBlock().getHash()));
      }
      return value;
   }
}
