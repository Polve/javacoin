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
import java.util.Map;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.KeyFactory;
import hu.netmind.bitcoin.Miner;

/**
 * A simple implementation which can be preconfigured with block values for
 * testing.
 * @author Robert Brautigam
 */
public class CachingBalanceCalculatorImpl extends CachingBalanceCalculator
{
   private Map<Block, Long> balanceMap;
   private int calculateCount = 0;

   public CachingBalanceCalculatorImpl(BlockChain blockChain,
         KeyFactory keyFactory, BlockBalanceCache cache)
   {
      super(blockChain, keyFactory, cache);
   }

   protected void calculateBalance()
   {
      super.calculateBalance();
      calculateCount++;
   }

   public int getCalculateCount()
   {
      return calculateCount;
   }

   public void setBalanceMap(Map<Block,Long> balanceMap)
   {
      this.balanceMap=balanceMap;
   }

   public long calculateBalance(Block block)
   {
      return balanceMap.get(block);
   }
}

