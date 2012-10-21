package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Script;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.VerificationException;
import it.nibbles.bitcoin.utils.BtcUtil;
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
    * @param link The link that represents the branch if the new transaction.
    * @param block The block we're trying to add.
    * @return The total value of the inputs after verification.
    */
   public long verifyTransaction(BlockChainLink link, Block block, Transaction tx)
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
         if (linkStorage.outputClaimedInSameBranch(link, in))
            throw new VerificationException("Block: " + BtcUtil.hexOut(block.getHash()) + " Tx: " + BtcUtil.hexOut(tx.getHash())
               + " output claimed by " + in + " is already claimed in another block of the same branch: "
               + BtcUtil.hexOut(linkStorage.getClaimerLink(link, in).getBlock().getHash()));
      }
      return value;
   }
}
