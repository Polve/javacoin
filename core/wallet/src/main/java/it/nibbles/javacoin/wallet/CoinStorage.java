/**
 * Copyright (C) 2012 NetMind Consulting Bt.
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

package it.nibbles.javacoin.wallet;

import java.util.List;
import it.nibbles.javacoin.Coin;

/**
 * A persistent store of coins. Wallets call all methods in a synchronized way,
 * so this storage doesn't have to be multithreaded.
 * @author Robert Brautigam
 */
public interface CoinStorage
{
   /**
    * Get the latest block hash that was used to generate the coins in this
    * storage.
    */
   byte[] getLastCheckedBlockHash();

   /**
    * Get all the unspent coins in this storage.
    */
   List<CoinImpl> getUnspentCoins();

   /**
    * Get all the coins in the storage.
    */
   List<CoinImpl> getCoins();

   /**
    * Get the coin from the specified output if there is one.
    */
   CoinImpl getCoin(byte[] transactionHash, int outputIndex);

   /**
    * Update all coins listed in an atomic operation.
    */
   void update(CoinImpl... coins);

   /**
    * Remove some coins, and at the same time set the last checked block
    * hash to the given value.
    */
   void remove(byte[] lastCheckedBlockHash, CoinImpl... coins);
}

