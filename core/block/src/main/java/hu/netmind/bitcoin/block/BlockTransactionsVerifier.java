package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitcoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.VerificationException;
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
}
