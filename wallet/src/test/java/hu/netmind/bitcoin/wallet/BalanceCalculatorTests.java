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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.easymock.EasyMock;
import org.easymock.Capture;
import hu.netmind.bitcoin.*;
import java.util.Observer;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
@Test
public class BalanceCalculatorTests
{
   private BlockChain blockChain;
   private KeyStore keyStore;
   private Miner miner;
   private BlockBalanceCache cache;

   @BeforeMethod
   public void setupMocks()
   {
      // Setup mocks
      blockChain = EasyMock.createMock(BlockChain.class);
      keyStore = EasyMock.createMock(KeyStore.class);
      miner = EasyMock.createMock(Miner.class);
      cache = EasyMock.createMock(BlockBalanceCache.class);
   }

   public void testUpdateInit()
   {
      UpdatingBalanceCalculator calculator = 
         new UpdatingBalanceCalculatorImpl(blockChain, keyStore, miner);
      Assert.assertEquals(calculator.getBalance(),1);
   }

   public void testUpdateBlockChain()
   {
      // Capture the observer the calculator registers
      Capture<Observer> observerTrap = new Capture<Observer>();
      blockChain.addObserver(EasyMock.capture(observerTrap));
      EasyMock.replay(blockChain);
      // Create calculator
      UpdatingBalanceCalculator calculator = 
         new UpdatingBalanceCalculatorImpl(blockChain, keyStore, miner);
      // Simulate update change
      observerTrap.getValue().update(null,null);
      // Check for increased balance because the update was invoked
      Assert.assertEquals(calculator.getBalance(),2);
   }

   public void testUpdateKeyStore()
   {
      // Capture the observer the calculator registers
      Capture<Observer> observerTrap = new Capture<Observer>();
      keyStore.addObserver(EasyMock.capture(observerTrap));
      EasyMock.replay(keyStore);
      // Create calculator
      UpdatingBalanceCalculator calculator = 
         new UpdatingBalanceCalculatorImpl(blockChain, keyStore, miner);
      // Simulate update change
      observerTrap.getValue().update(null,null);
      // Check for increased balance because the update was invoked
      Assert.assertEquals(calculator.getBalance(),2);
   }

   public void testUpdateMiner()
   {
      // Capture the observer the calculator registers
      Capture<Observer> observerTrap = new Capture<Observer>();
      miner.addObserver(EasyMock.capture(observerTrap));
      EasyMock.replay(miner);
      // Create calculator
      UpdatingBalanceCalculator calculator = 
         new UpdatingBalanceCalculatorImpl(blockChain, keyStore, miner);
      // Simulate update change
      observerTrap.getValue().update(null,null);
      // Check for increased balance because the update was invoked
      Assert.assertEquals(calculator.getBalance(),2);
   }

   public void testCachingWithNothingCached()
   {
      // Create the blocks
      List<Block> blocks = new ArrayList<Block>();
      for ( int i=0; i<5; i++ )
         blocks.add(EasyMock.createMock(Block.class));
      // Create block chain
      EasyMock.expect(blockChain.getLongestChain()).andReturn(blocks).anyTimes();
      blockChain.addObserver((Observer)EasyMock.anyObject());
      EasyMock.replay(blockChain);
      // Prepare what calls the cache should receive
      EasyMock.expect(cache.getEntry((Block) EasyMock.anyObject())).andReturn(null).anyTimes();
      for ( int i=0; i<5; i++ )
         cache.addEntry(blocks.get(i),(long) (i+1)*5);
      EasyMock.replay(cache);
      // Setup the object to test
      Map<Block,Long> blockBalances = new HashMap<Block,Long>();
      for ( int i=0; i<5; i++ )
         blockBalances.put(blocks.get(i),(long) 5); // Each block is worth 5
      CachingBalanceCalculatorImpl calculator = new CachingBalanceCalculatorImpl(
            blockChain, keyStore, miner, cache);
      calculator.setBalanceMap(blockBalances);
      // Check results
      Assert.assertEquals(calculator.getBalance(),25);
      EasyMock.verify(cache);
   }

   public void testCachingWithLatestCached()
   {
      // Create the blocks
      List<Block> blocks = new ArrayList<Block>();
      for ( int i=0; i<5; i++ )
         blocks.add(EasyMock.createMock(Block.class));
      // Create block chain
      EasyMock.expect(blockChain.getLongestChain()).andReturn(blocks).anyTimes();
      blockChain.addObserver((Observer)EasyMock.anyObject());
      EasyMock.replay(blockChain);
      // Prepare what calls the cache should receive
      EasyMock.expect(cache.getEntry(blocks.get(4))).andReturn((long) 25).anyTimes();
      EasyMock.replay(cache); // No other calls should be made
      // Setup the object to test
      Map<Block,Long> blockBalances = new HashMap<Block,Long>();
      for ( int i=0; i<5; i++ )
         blockBalances.put(blocks.get(i),(long) 5); // Each block is worth 5
      CachingBalanceCalculatorImpl calculator = new CachingBalanceCalculatorImpl(
            blockChain, keyStore, miner, cache);
      calculator.setBalanceMap(blockBalances);
      // Check results
      Assert.assertEquals(calculator.getBalance(),25);
      EasyMock.verify(cache);
   }

   public void testCachingWithMiddleCached()
   {
      // Create the blocks
      List<Block> blocks = new ArrayList<Block>();
      for ( int i=0; i<5; i++ )
         blocks.add(EasyMock.createMock(Block.class));
      // Create block chain
      EasyMock.expect(blockChain.getLongestChain()).andReturn(blocks).anyTimes();
      blockChain.addObserver((Observer)EasyMock.anyObject());
      EasyMock.replay(blockChain);
      // Prepare what calls the cache should receive
      EasyMock.expect(cache.getEntry(blocks.get(4))).andReturn(null);
      EasyMock.expect(cache.getEntry(blocks.get(3))).andReturn(null);
      EasyMock.expect(cache.getEntry(blocks.get(2))).andReturn((long) 15).anyTimes(); // 3rd has cached data
      cache.addEntry(blocks.get(3),(long) 20);
      cache.addEntry(blocks.get(4),(long) 25);
      EasyMock.replay(cache); // No other calls should be made
      // Setup the object to test
      Map<Block,Long> blockBalances = new HashMap<Block,Long>();
      for ( int i=0; i<5; i++ )
         blockBalances.put(blocks.get(i),(long) 5); // Each block is worth 5
      CachingBalanceCalculatorImpl calculator = new CachingBalanceCalculatorImpl(
            blockChain, keyStore, miner, cache);
      calculator.setBalanceMap(blockBalances);
      // Check results
      Assert.assertEquals(calculator.getBalance(),25);
      EasyMock.verify(cache);
   }
}

