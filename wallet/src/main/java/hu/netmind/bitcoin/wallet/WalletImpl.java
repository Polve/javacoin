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

import java.util.Observable;
import java.util.Observer;
import hu.netmind.bitcoin.Key;
import hu.netmind.bitcoin.Wallet;
import hu.netmind.bitcoin.Miner;
import hu.netmind.bitcoin.KeyStore;
import hu.netmind.bitcoin.TransactionFactory;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.NotEnoughMoneyException;
import hu.netmind.bitcoin.VerificationException;

/**
 * This implementation of a Wallet supports a plug-in mechanism to
 * configure the exact behaviour of the Wallet.<br>
 * Class is thread-safe (can be used from multiple threads).
 * @author Robert Brautigam
 */
public class WalletImpl extends Observable implements Wallet
{
   private final Miner miner;                           // Miner is responsible for taking Transactions
   private final BalanceCalculator balanceCalculator;   // Maintains/calculates the balance
   private final TransactionFactory transactionFactory; // Creates transactions
   private final KeyStore keyStore;                     // Holds all of our keys

   /**
    * Initialize the Wallet with the block chain, key store and all algorithms.
    * @param keyStore The cryptographical keys the owner of this wallet owns.
    * @param miner Miner that takes care of transactions.
    * @param balanceCalculator The calculator for the total balance.
    */
   public WalletImpl(Miner miner, BalanceCalculator balanceCalculator,
         TransactionFactory transactionFactory, KeyStore keyStore)
   {
      this.miner=miner;
      this.balanceCalculator=balanceCalculator;
      this.transactionFactory=transactionFactory;
      this.keyStore=keyStore;
      // Register listener to always call update when the balance might change
      balanceCalculator.addObserver(new Observer()
            {
               public void update(Observable obserable, Object data)
               {
                  if ( data == BalanceCalculator.Event.BALANCE_CHANGE )
                  {
                     setChanged();
                     notifyObservers(Event.BALANCE_CHANGE);
                  }
               }
            });
   }

   /**
    * Get the actual calculated balance.
    */
   public long getBalance()
   {
      // Simply get it from the calculator, who also maintains it
      return balanceCalculator.getBalance();
   }

   /**
    * Send a given amount from this wallet to the specified address.
    * @throws NotEnoughMoneyException If the amount was not present
    * according to the Miner. Note that if the miner is not setup
    * to do validation, this exception never arises, only the transaction
    * will never be verified by any other block either.
    */
   public void sendMoney(String to, long amount)
      throws NotEnoughMoneyException, VerificationException
   {
      // Create transaction 
      Transaction transaction = transactionFactory.createTransaction(
            new Address(to).getKeyHash(),amount);
      // Make transaction available to the miner, so it can work on
      // including it in the next block
      miner.addTransaction(transaction);
   }

   /**
    * Create a new address in this wallet others can transfer to. It is
    * recommended not to cache this address, but to create new addresses
    * for each transaction.
    */
   public String createAddress()
      throws VerificationException
   {
      Key key = keyStore.createKey();
      return new Address(key.getType(),key.getHash()).toString();
   }
}


