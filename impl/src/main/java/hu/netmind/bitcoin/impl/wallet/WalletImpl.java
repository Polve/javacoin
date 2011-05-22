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

import java.util.Observable;
import java.util.Observer;
import hu.netmind.bitcoin.api.Wallet;
import hu.netmind.bitcoin.api.BlockChain;
import hu.netmind.bitcoin.api.Miner;
import hu.netmind.bitcoin.api.TransactionFactory;
import hu.netmind.bitcoin.api.Transaction;
import hu.netmind.bitcoin.api.NotEnoughMoneyException;

/**
 * This implementation of a Wallet supports a plug-in mechanism to
 * configure the exact behaviour of the Wallet.
 * @author Robert Brautigam
 */
public class WalletImpl extends Observable implements Wallet
{
   private final BlockChain blockChain;                 // The consistent shared block chain
   private final Miner miner;                           // Miner is responsible for taking Transactions
   private final BalanceCalculator balanceCalculator;   // Maintains/calculates the balance
   private final TransactionFactory transactionFactory; // Organizes send transactions

   /**
    * Initialize the Wallet with the block chain, key store and all algorithms.
    * @param blockChain The shared information about accepted transactions in
    * a block chain.
    * @param keyStore The cryptographical keys the owner of this wallet owns.
    * @param miner Miner that takes care of transactions.
    * @param balanceCalculator The calculator for the total balance.
    */
   public WalletImpl(BlockChain blockChain, Miner miner,
         BalanceCalculator balanceCalculator, TransactionFactory transactionFactory)
   {
      this.blockChain=blockChain;
      this.miner=miner;
      this.balanceCalculator=balanceCalculator;
      this.transactionFactory=transactionFactory;
      // Register listener to always call update when the balance might change
      blockChain.addObserver(new Observer()
            {
               public void update(Observable obserable, Object data)
               {
                  setChanged(); // It "might" changed something
                  notifyObservers();
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
    */
   public void sendMoney(String to, long amount)
      throws NotEnoughMoneyException
   {
      // Create transaction 
      Transaction transaction = transactionFactory.createTransaction(to,amount);
      // Make transaction available to our transaction pool
      miner.addTransaction(transaction);
   }
}


