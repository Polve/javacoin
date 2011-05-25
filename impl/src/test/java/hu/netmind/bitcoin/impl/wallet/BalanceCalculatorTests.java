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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.easymock.EasyMock;
import org.easymock.Capture;
import hu.netmind.bitcoin.api.*;
import java.util.Observer;

/**
 * @author Robert Brautigam
 */
@Test
public class BalanceCalculatorTests
{
   private BlockChain blockChain;
   private KeyStore keyStore;
   private Miner miner;

   @BeforeMethod
   public void setupMocks()
   {
      // Setup mocks
      blockChain = EasyMock.createMock(BlockChain.class);
      keyStore = EasyMock.createMock(KeyStore.class);
      miner = EasyMock.createMock(Miner.class);
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
}

