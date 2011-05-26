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

package hu.netmind.bitcoin.impl.wallet;

import hu.netmind.bitcoin.Block;
import java.util.Observable;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.KeyStore;
import hu.netmind.bitcoin.Miner;
import java.util.List;
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
 * @author Robert Brautigam
 */
public abstract class CachingBalanceCalculator extends UpdatingBalanceCalculator
{
   private static final Logger logger = LoggerFactory.getLogger(CachingBalanceCalculator.class);

   private BlockBalanceCache cache;

   public CachingBalanceCalculator(BlockChain blockChain, KeyStore keyStore, Miner miner,
         BlockBalanceCache cache)
   {
      super(blockChain, keyStore, miner);
      this.cache=cache;
   }

   /**
    * To calculate/update the balance this method just tries to calculate
    * the aggregated transaction value up to the last Block in the longest
    * chain.
    */
   protected void updateBalance()
   {
      // This could be implemented really simply with a recursive function,
      // but we don't want to stress the stack that much. There are hundreds
      // of thousands of blocks. So this algorithm tries to find the first parent
      // for which we know the balance from cache, then go forward again and
      // put the values into the cache.
      List<Block> chain = getBlockChain().getLongestChain();
      int index;
      // Search for newest block for which we know the balance
      for ( index = chain.size()-1; (index >= 0) && (cache.getEntry(chain.get(index))==null) ; index-- );
      // Now calculate the newer ones (note index is -1 if no cache entries at all)
      long balance = 0;
      if ( index >= 0 )
         balance = cache.getEntry(chain.get(index));
      for ( index++ ; index < chain.size() ; index++ )
      {
         balance += calculateBalance(chain.get(index));
         cache.addEntry(chain.get(index),balance);
      }
      // Set the new balance
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

