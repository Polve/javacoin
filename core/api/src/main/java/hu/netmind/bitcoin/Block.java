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

package hu.netmind.bitcoin;

import java.util.List;
import java.io.Serializable;

/**
 * A Block is an already accepted collection of transactions in the
 * block chain, although it may not be part of the longest chain.
 * Blocks are created by "Miners", machines that participate in the
 * BitCoin network in order to earn money for this effort. Creating
 * Blocks is (computationally) difficult, adjusted so that the combined
 * processing power of all the machines participating will generate
 * approximately one Block every 10 minutes (for all times). Miners earn
 * money two ways: 
 * <ul>
 *    <li>Each created block can contain "new" money for which
 * the amount is agreed in the network ("coinbase transactions"). </li>
 *    <li>Transactions in the block may have transaction fees 
 * defined which the Miner can collect if it includes it in a
 * Block. Fees are defined by the clients who create the transactions,
 * and it is not guaranteed that a fee is included anyway.</li>
 * </ul>
 * @author Robert Brautigam
 */
public interface Block extends Serializable
{
   /**
    * Get the creation time in millis.
    */
   long getCreationTime();

   /**
    * Get the nonce used to generate the hash.
    */
   long getNonce();

   /**
    * Get the difficulty target for this block in a compressed form.
    */
   long getCompressedTarget();

   /**
    * Get the hash of the previous block.
    */
   byte[] getPreviousBlockHash();

   /**
    * Get the merkle root of the transactions.
    */
   byte[] getMerkleRoot();

   /**
    * Return the hash of this block.
    */
   byte[] getHash();

   /**
    * Get the transactions in this block.
    */
   List<Transaction> getTransactions();

   /**
    * Validate that this block and all contained transactions
    * are consistent and follows all rules that don't require any context.
    */
   void validate()
      throws VerificationException;
}

