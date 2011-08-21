/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package hu.netmind.bitcoin.wallet;

import hu.netmind.bitcoin.Block;
import java.util.Observable;
import java.util.Observer;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.KeyFactory;
import java.util.List;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides basic mechanisms to cache pre-calculated values to Block items.
 * The idea is, that blocks already in the chain never change, so them and
 * all parents of that Block are fixed. This means the balance for a 
 * previously calculated path starting from the genesis block never change,
 * assuming new keys don't appear in the key store which have previously
 * been used (import), and the calculation algorithm does not depend on 
 * blocks coming <i>after</i> the cached block (which sometimes make sense actually).
 * The events that might update the balance are the following:
 * <ul>
 *    <li>The BlockChain changes. In this case there might be new transactions 
 *        integrated into the longest chain, or some side-branch becomes the
 *        longest chain.</li>
 *    <li>The KeyFactory changes. If the owner receives new keys which were not
 *        generated, but imported, we don't know whether those keys were ever
 *        used, and so the balance might be impacted in unknown ways. Also if
 *        a key is removed the balance potentially changes.</li>
 *    <li>The Miner receives transactions which are not yet incorporated into
 *        the BlockChain. (This is not handled, since this implementation does
 *        not calculate the open amount in the Miner)</li>
 * </ul>
 * @author Robert Brautigam
 */
public abstract class CachingBalanceCalculator extends UpdatingBalanceCalculator
{
   private static final Logger logger = LoggerFactory.getLogger(CachingBalanceCalculator.class);

   private BlockBalanceCache cache;
   private BlockChain blockChain;
   private KeyFactory keyFactory;

   public CachingBalanceCalculator(BlockChain blockChain, KeyFactory keyFactory,
         BlockBalanceCache cache)
   {
      this.blockChain=blockChain;
      this.keyFactory=keyFactory;
      this.cache=cache;
      // Register listeners to all
      blockChain.addObserver(new Observer()
            {
               public void update(Observable source, Object event)
               {
                  // If block change changes, we merely need to calculate
                  // the balances of the new blocks
                  calculateBalance();
               }
            });
      keyFactory.addObserver(new Observer()
            {
               public void update(Observable source, Object event)
               {
                  // Currently there is no import of keys, so new keys
                  // do not have retroactive impact, so we don't have
                  // to clean the cache.
                  calculateBalance();
               }
            });
   }

   /**
    * To calculate/update the balance this method just tries to calculate
    * the aggregated transaction value up to the last Block in the longest
    * chain.
    */
   protected void calculateBalance()
   {
      // This could be implemented really simply with a recursive function,
      // but we don't want to stress the stack that much, because there are hundreds
      // of thousands of blocks. Instead we use heap to store the path we've walked.
      // So this algorithm tries to find the first parent
      // for which we know the balance from cache, then go forward again and
      // put the values into the cache.
      Block currentBlock = blockChain.getLastBlock();
      LinkedList<Block> path = new LinkedList<Block>();
      // Search for newest block for which we know the balance
      while ( (currentBlock!=null) && (cache.getEntry(currentBlock) == null) )
      {
         path.addFirst(currentBlock); // Save block path
         currentBlock = currentBlock.getPreviousBlock();
      }
      // Initialize balance. Note: currentBlock is not in path, and is either null,
      // or non-null if there was a cache entry for a parent.
      long balance = 0;
      if ( currentBlock != null )
         balance = cache.getEntry(currentBlock);
      // Now walk through the same path, this time forward, cache and add all blocks
      for ( Block block : path )
      {
         balance += calculateBalance(block);
         cache.addEntry(block,balance);
      }
      // Set the new total balance
      setBalance(balance);
   }

   /**
    * Calculate the balance for a given Block. This method should be implemented,
    * and the value returned should contain the open amount beloning to the owner.
    * @return The amount in the specified block. Note this can be negative if the user
    * spent some money in this block, positive is the user received money, and zero
    * if there are no relevant transactions to the user, or the relevant transactions
    * are completely balanced.
    */
   protected abstract long calculateBalance(Block block);
}

