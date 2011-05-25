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

import java.util.List;

/**
 * The main technical artifact in the BitCoin network is the shared
 * knowledge collected in every node called BlockChain. This chain
 * contains Blocks (which in turn contain transactions) starting from
 * the very first Block called the "Genesis Block". Every Block in the
 * chain refers to the previous Block by reference (and cyrptographically),
 * so technically it is a tree of Blocks in which the root is the
 * Genesis Block, and occasionally it contains side-chains also. There is
 * a special "chain" (path) of Blocks called the "longest chain" which all clients
 * must accept as the official chain. Although it's named "longest", it is
 * the chain that required the most work, not necessarily the longest by
 * number of blocks. <br><i>Note:</i> Implementations are responsible for
 * keeping the block chain consistent and following the rules of the BitCoin
 * network.
 * @author Robert Brautigam
 */
public interface BlockChain extends Observable
{
   enum Event
   {
      LONGEST_CHAIN_CHANGE
   };

   /**
    * Get a transaction container that represents the longest chain in this
    * block chain.
    * @return A transaction container which can be queried and contains only the
    * blocks and transactions of the longest chain.
    */
   TransactionContainer getLongestChainTransactionContainer();

   /**
    * Get a transaction container representing all the transactions in the block chain.
    * Note that a single transaction can be incorporated into multiple branches, in
    * which case that transaction will show up multiple times in the returned container.
    * @return The container representing all transactions in the block chain, uniqueness
    * is not guaranteed.
    */
   TransactionContainer getTransactionContainer();

}

