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
import hu.netmind.bitcoin.api.BlockChain;
import hu.netmind.bitcoin.api.KeyStore;
import hu.netmind.bitcoin.api.Miner;

/**
 * Provides mechanisms to remember the last calculated balance, and listen to 
 * events that might update the balance. The events that might update the
 * balance are the following:
 * <ul>
 *    <li>The BlockChain changes. In this case there might be new transactions 
 *        integrated into the longest chain, or some side-branch becomes the
 *        longest chain.</li>
 *    <li>The KeyStore changes. If the owner receives new keys which were not
 *        generated, but imported, we don't know whether those keys were ever
 *        used, and so the balance might be impacted in unknown ways. Also if
 *        a key is removed the balance potentially changes.</li>
 *    <li>The Miner receives transactions which are not yet incorporated into
 *        the BlockChain.</li>
 * </ul>
 */
public abstract class UpdatingBalanceCalculator extends Observable implements BalanceCalculator
{
   private BlockChain blockChain;
   private KeyStore keyStore;
   private Miner miner;

   private long currentBalance = -1;

   /**
    * Construct the balance calculator with the observed objects.
    */
   public UpdatingBalanceCalculator(BlockChain blockChain, KeyStore keyStore, Miner miner)
   {
      this.blockChain=blockChain;
      this.keyStore=keyStore;
      this.miner=miner;
      // Register listeners to all
      blockChain.addObserver(new Observer()
            {
               public void update(Observable source, Object event)
               {
                  fireUpdateBalance();
               }
            });
      keyStore.addObserver(new Observer()
            {
               public void update(Observable source, Object event)
               {
                  fireUpdateBalance();
               }
            });
      miner.addObserver(new Observer()
            {
               public void update(Observable source, Object event)
               {
                  fireUpdateBalance();
               }
            });
   }

   public BlockChain getBlockChain()
   {
      return blockChain;
   }

   public Miner getMiner()
   {
      return miner;
   }

   public KeyStore getKeyStore()
   {
      return keyStore;
   }

   private void fireUpdateBalance()
   {
      // First update the balance
      updateBalance();
      // Then fire the notification to observers
      notifyObservers(Event.BALANCE_CHANGE);
   }

   /**
    * Get the currently calculated balance.
    */
   public synchronized long getBalance()
   {
      if ( currentBalance < 0 )
      {
         // Initialize balance
         currentBalance = 0;
         updateBalance();
      }
      return currentBalance;
   }

   /**
    * Set the balance. This method may be called by <code>updateBalance()</code>
    * to set a new balance.
    */
   protected synchronized void setBalance(long balance)
   {
      this.currentBalance=balance;
      setChanged();
   }

   /**
    * Implement this method to actually (re-)calculate the balance based on
    * the new information.
    */
   protected abstract void updateBalance();
}

