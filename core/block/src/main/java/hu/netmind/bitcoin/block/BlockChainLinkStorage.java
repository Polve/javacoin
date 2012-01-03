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

import hu.netmind.bitcoin.TransactionInput;
import java.util.List;

/**
 * Implement this interface to store chain links.
 * @author Robert Brautigam
 */
public interface BlockChainLinkStorage
{
   /**
    * Get the very first link in the storage, which should contain the genesis
    * block of this chain. This might be null if there are no
    * blocks yet in the storage.
    */
   BlockChainLink getGenesisLink();

   /**
    * Get the link stored which has the greatest aggregated complexity
    * and is connected to the genesis block.
    */
   BlockChainLink getLastLink();

   /**
    * Get the link to the given hash value.
    * @return The link with the block with the given hash, or null if no such link exists.
    */
   BlockChainLink getLink(byte[] hash);

   /**
    * Get the links for which the previous link contains the block with hash
    * given.
    */
   List<BlockChainLink> getNextLinks(byte[] hash);

   /**
    * Find the link with the block which contains the claimed output by the input given.
    * All the links with blocks including the given one and all parents
    * of it will be checked (this link denotes the branch to search).
    * @param link The link with the block that represents the branch to search.
    * @param in The tansaction input claiming the output to find the block for.
    * @return The link that contains the block that contains the said transaction.
    */
   BlockChainLink getClaimedLink(BlockChainLink link, TransactionInput in);

   /**
    * Find the link with the block which contains a claim for the same transaction output
    * that is claimed by the given input.
    * @param link The link with the block that represents the branch to search.
    * @param in The tansaction input claiming the output to find the block for.
    * @return The block that contains the said input if any.
    */
   BlockChainLink getClaimerLink(BlockChainLink link, TransactionInput in);

   /**
    * Save a link into the storage. If the link exists, it will be overwritten.
    */
   void addLink(BlockChainLink link);

   /**
    * Update an orphan block to make it non-orphan.
    */
   void updateLink(BlockChainLink link);
}

