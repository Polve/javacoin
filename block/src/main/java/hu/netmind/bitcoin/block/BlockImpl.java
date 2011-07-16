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

package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionFilter;
import hu.netmind.bitcoin.BlockPath;
import java.util.List;

/**
 * A Block is a container in which BitCoin transactions are grouped. Generating a
 * Block is a relatively hard computational task that is constantly adjusted so that
 * the whole BitCoin network is able to produce one Block approximately every 10 minutes.
 * When a Miner succeeds in generating a Block it will include all the pending transactions
 * in the network into this Block thereby claiming transaction fees (and generating new coins
 * also). Transactions are considered valid if they are in a Block on a longest path, all other
 * transactions are candidates to include in a next Block.
 * @author Robert Brautigam
 */
public class BlockImpl extends PrefilteredTransactionContainer implements Block
{
   private long magic;
   private long creationTime;
   private long nonce;
   private long bits;
   private byte[] merkleRoot;
   private byte[] hash;

   public BlockImpl(List<Transaction> transactions, TransactionFilter preFilter,
         long magic, long creationTime, long nonce, long bits, byte[] merkleRoot)
   {
      this(transactions,preFilter,magic,creationTime,nonce,bits,merkleRoot,null);
   }

   public BlockImpl(List<Transaction> transactions, TransactionFilter preFilter,
         long magic, long creationTime, long nonce, long bits, byte[] merkleRoot, byte[] hash)
   {
      this.magic=magic;
      this.creationTime=creationTime;
      this.nonce=nonce;
      this.bits=bits;
      this.merkleRoot=merkleRoot;
      this.hash=hash;
      if ( hash == null )
         calculateHash();
      setPreFilter(preFilter);
      addTransactions(transactions);
   }

   /**
    * Get all the stored transactions.
    */
   protected List<Transaction> getStoredTransactions()
   {
      // TODO
      return null;
   }

   /**
    * Add transactions permanently to this block. This may be only invoked once when constructing
    * this object.
    */
   protected void addStoredTransactions(List<Transaction> transactions)
   {
      // TODO
   }

   /**
    * Calculate the hash of this block.
    */
   private void calculateHash()
   {
      // TODO
   }

   /**
    * Get the previous block.
    */
   public Block getPreviousBlock()
   {
      // TODO
      return null;
   }

   /**
    * Get the longest path this block is on. Note, that might not be the overall
    * longest path in the block chain.
    */
   public BlockPath getLongestPath()
   {
      // TODO
      return null;
   }

   public long getMagic()
   {
      return magic;
   }
   public long getCreationTime()
   {
      return creationTime;
   }
   public long getNonce()
   {
      return nonce;
   }
   public long getBits()
   {
      return bits;
   }
   public byte[] getMerkleRoot()
   {
      return merkleRoot;
   }
   public byte[] getHash()
   {
      return hash;
   }
}

