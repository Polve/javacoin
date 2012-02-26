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

package hu.netmind.bitcoin.wallet;

import hu.netmind.bitcoin.Coin;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.Wallet;
import hu.netmind.bitcoin.TransactionOutput;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.InvalidCoinException;
import java.util.Arrays;
import java.math.BigInteger;

/**
 * All wallets contain Coins, which are the Coins the owner of the Wallet
 * can pay with. 
 * As coins are immutable, it assumes that the ownership of past "coins"
 * never change, that means it doesn't account (allow) keys to be lost (or
 * imported). Coins can have different values, so not all coins are equal.
 * Coins can be used for calculating the total amount
 * of "BitCoins" (or value) the owner has, and can be used to create new transactions
 * (spend coins). Only full coins can be spent, however it is possible to reclaim
 * "change" from a transaction as a new coin.
 * @author Robert Brautigam
 */
public class CoinImpl implements Coin
{
   // Attributes that identify origin (these are immutable)
   private byte[] blockHash;
   private long blockHeight;
   private byte[] transactionHash;
   private int transactionOutputIndex;
   private boolean coinbase;
   private long value;

   // Spend indicators (these are mutable)
   private boolean spent;
   private long spentTime;
   private long spentHeight;   

   /**
    * Construct an unspent coin.
    * @param blockHash The hash of the block which contains the TransactionOutput this
    * coin referes to. (Each coin is in one-to-one relationship with a single 
    * TransactionOut)
    * @param blockHeight The "height" the block is at.
    * @param transactionHash The hash of the transaction that contains the TransactionOut.
    * @param transactionOutputIndex The index of the TransactionOut in the 
    * specified Transaction. This information is necessary to spend the Coin.
    * @param coinbase Whether the transaction that contains this Coin is a coinbase
    * transaction.
    * @param value The value of this coin (in 100.000.000th of a BTC).
    */
   public CoinImpl(byte[] blockHash, long blockHeight, byte[] transactionHash,
         int transactionOutputIndex, boolean coinbase, long value)
   {
      this.blockHash=blockHash;
      this.blockHeight=blockHeight;
      this.transactionHash=transactionHash;
      this.transactionOutputIndex=transactionOutputIndex;
      this.coinbase=coinbase;
      this.value=value;
      this.spent=false;
      this.spentTime=0;
      this.spentHeight=0;
   }

   public byte[] getBlockHash()
   {
      return blockHash;
   }
   public long getBlockHeight()
   {
      return blockHeight;
   }
   public byte[] getTransactionHash()
   {
      return transactionHash;
   }
   public int getTransactionOutputIndex()
   {
      return transactionOutputIndex;
   }
   public boolean isCoinbase()
   {
      return coinbase;
   }
   public long getValue()
   {
      return value;
   }
   public boolean isSpent()
   {
      return spent;
   }
   public long getSpentTime()
   {
      return spentTime;
   }
   public long getSpentHeight()
   {
      return spentHeight;
   }

   protected void setSpent(boolean spent)
   {
      this.spent=spent;
   }

   protected void setSpentTime(long spentTime)
   {
      this.spentTime=spentTime;
   }

   protected void setSpentHeight(long spentHeight)
   {
      this.spentHeight=spentHeight;
   }

   public String toString()
   {
      return ""+new BigInteger(1,transactionHash).toString(16)+"/"+transactionOutputIndex+" "+value+" units";
   }
}



