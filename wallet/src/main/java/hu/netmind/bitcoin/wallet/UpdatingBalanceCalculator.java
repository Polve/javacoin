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

/**
 * Provides mechanisms to remember the last calculated balance.
 * It is the responsibility of the implementations to hook into events which
 * might update the balance.
 */
public abstract class UpdatingBalanceCalculator extends Observable implements BalanceCalculator
{
   private long currentBalance = -1;

   /**
    * Get the currently calculated balance.
    */
   public synchronized long getBalance()
   {
      if ( currentBalance < 0 )
      {
         // Initialize balance
         currentBalance = 0;
         calculateBalance();
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
      // Notify listeners of change
      setChanged();
      notifyObservers(Event.BALANCE_CHANGE);
   }

   /**
    * Implement this method to actually (re-)calculate the balance based on
    * the new information.
    */
   protected abstract void calculateBalance();
}

