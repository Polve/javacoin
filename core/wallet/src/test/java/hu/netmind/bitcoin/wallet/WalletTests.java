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

/**
 * @author Robert Brautigam
 */
@Test
public class WalletTests
{
   private WalletImpl wallet;
   private BalanceCalculator balanceCalculator;
   private TransactionFactory transactionFactory;
   private Miner miner;
   private KeyFactory keyFactory;

   @BeforeMethod
   public void setupWallet()
   {
      // Setup mocks
      balanceCalculator = EasyMock.createMock(BalanceCalculator.class);
      transactionFactory = EasyMock.createMock(TransactionFactory.class);
      miner = EasyMock.createMock(Miner.class);
      keyFactory = EasyMock.createMock(KeyFactory.class);
      // Create wallet
      wallet = new WalletImpl(miner,balanceCalculator,transactionFactory,keyFactory,
            Address.Type.MAIN);
   }

   public void testBalanceChangedEvent()
   {
      // Capture the observer the wallet registers to blockchain
      Capture<Observer> observerTrap = new Capture<Observer>();
      balanceCalculator.addObserver(EasyMock.capture(observerTrap));
      EasyMock.replay(balanceCalculator);
      wallet = new WalletImpl(miner,balanceCalculator,transactionFactory,keyFactory,
            Address.Type.MAIN);
      // Register our own observer in wallet
      Observer walletObserver = EasyMock.createMock(Observer.class);
      walletObserver.update(wallet,Wallet.Event.BALANCE_CHANGE);
      EasyMock.replay(walletObserver);
      wallet.addObserver(walletObserver);
      // Trigger blockchain change
      observerTrap.getValue().update(null,BalanceCalculator.Event.BALANCE_CHANGE);
      // Check if wallet passed the event along
      EasyMock.verify(walletObserver);
   }

   public void testGetBalance()
   {
      // Setup a test balance
      EasyMock.expect(balanceCalculator.getBalance()).andReturn((long )1234).anyTimes();
      EasyMock.replay(balanceCalculator);
      // Test
      Assert.assertEquals(wallet.getBalance(),1234);
   }

   public void testSendMoney()
      throws NotEnoughMoneyException, VerificationException
   {
      // Setup transaction that will be returned
      Transaction transaction = EasyMock.createMock(Transaction.class);
      EasyMock.expect(transactionFactory.createTransaction(
               (byte[]) EasyMock.anyObject(),EasyMock.anyLong())).andReturn(transaction);
      EasyMock.replay(transactionFactory);
      // Setup miner to expect the transaction
      miner.addTransaction(transaction);
      EasyMock.replay(miner);
      // Send money
      wallet.sendMoney("14rJHyyXwPzRLptBm46VYUQrucQ9BMsXCS",1234);
      // Verify mine was called
      EasyMock.verify(miner);
   }

   @Test(expectedExceptions = NotEnoughMoneyException.class)
   public void testSendMoreMoneyThanAvailable()
      throws NotEnoughMoneyException, VerificationException
   {
      // Setup factory so it throws an exception
      EasyMock.expect(transactionFactory.createTransaction(
               (byte[]) EasyMock.anyObject(),EasyMock.anyLong())).andThrow(
            new NotEnoughMoneyException("Give me more money!"));
      EasyMock.replay(transactionFactory);
      // Send money
      wallet.sendMoney("14rJHyyXwPzRLptBm46VYUQrucQ9BMsXCS",1234);
   }
}

