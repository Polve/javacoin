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

package hu.netmind.bitcoin;

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
    * Get the value of this coin in 100.000.000th of BitCoin.
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
    * Get the block this coin was generated in. Note that this might
    * be an expensive operation.
    */
   Block getBlock();

   /**
    * Gets an optional comment that was added to this coin when it was generated.
    */
   String getComment();

   /**
    * Gets an optional external identification that was added when it was generated.
    */
   String getExternalId();

   /**
    * Get the transaction output this coin represents. Note that this
    * might be an expensive operation.
    */
   TransactionOutput getOutput();

   /**
    * Get whether the coin is already spent or not.
    */
   boolean isSpent();

   /**
    * Mark this coin as spent. When a coin is marked as spent, it will
    * no longer appear in the Wallet (although it might re-appear if
    * it does not appear as spent in the official block chain).
    * @throws InvalidCoinException If the coin was already spent, or it
    * was not part of a Wallet anymore (Note: the contents of a Wallet
    * can change from the network).
    */
   void spend()
      throws InvalidCoinException;
}

