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

import hu.netmind.bitcoin.api.Block;
import java.util.Observable;

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
public abstract class CachingBalanceCalculator extends Observable implements BalanceCalculator
{
   /**
    * Add a cache entry. The entry means that the algorithm determined that
    * the path leading up to the specified block from the genesis block,
    * including the transactions in the specified block have the specified
    * amount.
    * @param block The block up to which the amount is calculated.
    * @param amount The amount calculated.
    */
   private void addEntry(Block block, long amount)
   {
      // TODO
   }

   /**
    * Get the entry for a given block. 
    * @return 
    */
   private long getEntry(Block block)
   {
      // TODO
      return -1;
   }

   /**
    * Get the balance up to the specified Block, including the
    * transactions contained in the given block. If the amount for the
    * block is already known, it will come from cache. If not, it will
    * be calculated.
    * @param block The block up to which to calculate the balance.
    * @return The amount known from cache or calculated if it's not cached already.
    */
   protected long getBalance(Block block)
   {
      // TODO
      return -1;
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

