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

package hu.netmind.bitcoin.api;

/**
 * A miner is responsible for creating new blocks into the BlockChain.
 * Creating Blocks is computationally difficult, it is in fact so
 * difficult that it is more a matter of luck to get a new one. When creating
 * a new block, the Miner is expected to incorporate "orphan" transactions, 
 * as well as refer to the newest Block already in the chain. If the Miner
 * does not link to the newest Block, it might start an alternate branch
 * in the block chain which ultimately leads to the Block being inactive.
 * The Miner can choose which transactions to incorporate into the Block,
 * the difficulty does not actually increase with more transactions. If 
 * transactions contain a "transaction fee" the Miner can also claim that.
 * @author Robert Brautigam
 */
public interface Miner extends Observable, TransactionContainer
{
   /**
    * Add a transaction to the pool of "to be processed" transactions.
    * Note that only those transactions will be incorporated into blocks
    * that are consistent with the current longest block chain.
    */
   void addTransaction(Transaction transaction);
}

