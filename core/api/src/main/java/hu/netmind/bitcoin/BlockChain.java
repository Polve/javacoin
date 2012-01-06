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
      LONGEST_PATH_CHANGE
   };

   /**
    * Get the Genesis Block (the first block).
    */
   Block getGenesisBlock();

   /**
    * Get the last Block on the "longest path".
    */
   Block getLastBlock();

   /**
    * Get the block preceding the given block.
    * @return The Block that is referenced from the
    * given block in the chain, or null if no such block
    * exists. There is no preceding block if that was not yet
    * added to the chain, or the current block is the genesis
    * block.
    */
   Block getPreviousBlock(Block current);

   /**
    * Add a block to the chain. Block is first validated against
    * BitCoin rules, then added if it passed all tests.
    */
   void addBlock(Block block)
      throws VerificationException;
}
