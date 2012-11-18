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

package it.nibbles.javacoin;

/**
 * A Coin is a variable value item that represents an undivisible 
 * unit that can be spent. It corresponds roughly to a TransactionOutput,
 * but it is related to a single owner and has higher level constructs
 * for spending.
 * @author Robert Brautigam
 */
public interface Coin
{
   /**
    * Get the value of this coin in 100.000.000th of Bitcoin.
    */
   long getValue();

   /**
    * Returns whether this coin was generated in a coinbase transaction.
    */
   boolean isCoinbase();

   /**
    * Gets the height of the block it was generated in.
    */
   long getBlockHeight();

   /**
    * Get the block hash this coin was created in.
    */
   byte[] getBlockHash();

   /**
    * Get the transaction hash this coin was created in.
    */
   byte[] getTransactionHash();

   /**
    * Get the transaction output index this coin was created in.
    */
   int getTransactionOutputIndex();
}

