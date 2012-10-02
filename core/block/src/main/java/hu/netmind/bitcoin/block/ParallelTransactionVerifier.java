/**
 * Copyright (C) 2012 nibbles.it
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Script;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.VerificationException;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessandro Polverini
 */
public class ParallelTransactionVerifier
{

   private static final Logger logger = LoggerFactory.getLogger(ParallelTransactionVerifier.class);
   private static KnownExceptions exceptions = new KnownExceptions();
   private ExecutorService executorService;
   private BlockChainLinkStorage linkStorage;
   private ScriptFactory scriptFactory;
   private BlockChainLink link;
   private Block block;
   private boolean simplifiedVerification;
   private int numThreads;
   long inValue;
   long outValue;

   public ParallelTransactionVerifier(int maxThreads)
   {
      numThreads = maxThreads <= 0 ? Runtime.getRuntime().availableProcessors() : maxThreads;
      executorService = Executors.newFixedThreadPool(numThreads, new DaemonThreadFactory());
      logger.debug("Parallel Transaction Verifier instantiated with {} threads", numThreads);
   }

   public long verifyTransactions(BlockChainLinkStorage linkStorage, ScriptFactory scriptFactory,
      BlockChainLink previousLink, Block block, boolean simplifiedVerification)
      throws VerificationException, BitCoinException
   {
      this.linkStorage = linkStorage;
      this.scriptFactory = scriptFactory;
      this.link = previousLink;
      this.block = block;
      this.simplifiedVerification = simplifiedVerification;
      inValue = outValue = 0;
      logger.debug("Parallel checking of {} transactions...", block.getTransactions().size());
      List<Callable<Void>> todo = new ArrayList<>(block.getTransactions().size());
      for (Transaction tx : block.getTransactions())
      {
         todo.add(new SingleTxVerifier(tx));
      }
      try
      {
         List<Future<Void>> allRes = executorService.invokeAll(todo);
         for (Iterator<Future<Void>> it = allRes.iterator(); it.hasNext();)
         {
            Future<Void> res = it.next();
            try
            {
               res.get();
            } catch (ExecutionException ex)
            {
               Throwable t = ex.getCause();
               if (t instanceof VerificationException)
                  throw (VerificationException) t;
               else
               {
                  logger.error("Unexpected error: " + t.getMessage(), t);
                  throw new BitCoinException("Unexpected exception while veryfing block", t);
               }
            }
         }
      } catch (InterruptedException ex)
      {
         executorService.shutdownNow();
         executorService = Executors.newFixedThreadPool(numThreads);
         throw new VerificationException("Verification threads interrupted");
      }
      return inValue - outValue;
   }

   class SingleTxVerifier implements Callable<Void>
   {

      private Transaction tx;

      public SingleTxVerifier(Transaction tx)
      {
         this.tx = tx;
      }

      @Override
      public Void call() throws VerificationException
      {
         // Validate without context
         tx.validate();
         // Checks 16.1.1-7: Verify only if this is supposed to be a full node
         long localOutValue = 0;
         if ((!simplifiedVerification) && (!tx.isCoinbase()))
         {
            long localInValue = verifyTransaction();
            for (TransactionOutput out : tx.getOutputs())
            {
               localOutValue += out.getValue();
            }
            synchronized (this)
            {
               inValue += localInValue;
               outValue += localOutValue;
            }
            // Check 16.1.6: Using the referenced output transactions to get
            // input values, check that each input value, as well as the sum, are in legal money range
            // Check 16.1.7: Reject if the sum of input values < sum of output values
            if (localInValue < localOutValue)
               throw new VerificationException("more money spent (" + localOutValue + ") then available (" + localInValue + ") in transaction: " + tx);
         }
         return null;
      }

      /**
       * Verify that a transaction is valid according to sub-rules applying to
       * the block tree.
       *
       * @param link The link that represents the branch if the new transaction.
       * @param block The block we're trying to add.
       * @return The total value of the inputs after verification.
       */
      public long verifyTransaction()
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

            if (!exceptions.isExempt(tx.getHash(), ValidationCategory.ScriptValidation))
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

      /**
       * Search for a specific transaction in a block.
       *
       * @param block The block to search transaction in.
       * @param txHash The transaction hash to match.
       * @return The transaction in the block which has the given hash, null
       * otherwise.
       */
      private Transaction getTransaction(Block block, byte[] txHash)
      {
         for (Transaction txCandidate : block.getTransactions())
         {
            if (Arrays.equals(txCandidate.getHash(), txHash))
               return txCandidate;
         }
         return null;
      }
   }

   class DaemonThreadFactory implements ThreadFactory
   {

      int counter = 0;

      @Override
      public Thread newThread(Runnable r)
      {
         counter++;
         Thread t = new Thread(r);
         t.setDaemon(true);
         t.setName("Transaction Verifier " + counter);
         return t;
      }
   }
}
