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

import hu.netmind.bitcoin.api.Observable;

/**
 * Calculates the balance of the block chain for a given key set. Implementations
 * can choose the method of calculation, the algorithm of selecting transactions,
 * as well as associated risks, and caching mechanisms. Implementation must be
 * thread-safe.
 * @author Robert Brautigam
 */
public interface BalanceCalculator extends Observable
{
   enum Event
   {
      BALANCE_CHANGE
   };

   /**
    * Get the balance calculated according to the parameters defined for the
    * implementation.
    * @return The balance as calculated in hundred millionth BTC.
    */
   long getBalance();
}

