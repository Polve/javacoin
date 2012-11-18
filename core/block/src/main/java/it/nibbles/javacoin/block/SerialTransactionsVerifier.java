package it.nibbles.javacoin.block;

import it.nibbles.javacoin.Block;
import it.nibbles.javacoin.ScriptFactory;
import it.nibbles.javacoin.Transaction;
import it.nibbles.javacoin.TransactionOutput;
import it.nibbles.javacoin.VerificationException;

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
}
