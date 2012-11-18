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
    * blocks yet in the storage. Orphan blocks are never returned.
    */
   BlockChainLink getGenesisLink();

   /**
    * Get the link stored which has the greatest aggregated complexity
    * and is connected to the genesis block. Orphan blocks are never returned.
    */
   BlockChainLink getLastLink();

   /**
    * Return the height of the best chain
    * @return The height of the chain with most work
    */
   public int getHeight();

   /**
    * Get the link to the given hash value.
    * @return The link with the block with the given hash, or null if no such link exists.
    */
   BlockChainLink getLink(byte[] hash);

   /**
    * Returns true if a link with the given hash value is in the storage.
    * @return The link with the block with the given hash, or null if no such link exists.
    */
   boolean blockExists(byte[] hash);

   /**
    * Get the link to the given hash value.
    * @return The link with the block header with the given hash, or null if no such link exists.
    */
   BlockChainLink getLinkBlockHeader(byte[] hash);

//   /**
//    * Get the link following the current one given on the same
//    * branch the target is.
//    */
//   BlockChainLink getNextLink(byte[] current, byte[] target);
//
//   /**
//    * Get the links for which the previous link contains the block with hash
//    * given. Orphan blocks are returned.
//    */
//   List<BlockChainLink> getNextLinks(byte[] hash);

   /**
    * Get the common parent of the two given hashes if there is one.
    * @return The highest common parent of the two blocks specified. Null if there
    * is no common parent for any reason.
    */
   BlockChainLink getCommonLink(byte[] first, byte[] second);

   /**
    * Determine whether a block given is reachable from an intermediate
    * block by only going forward. 
    * @param target The block to reach.
    * @param source The source from which the target is attempted to be reached.
    * @return True if the target can be reached from the source, false otherwise.
    */
   boolean isReachable(byte[] target, byte[] source);

   /**
    * Find the link which contains the claimed transaction by the input given.
    * All the links including the given one and all parents
    * of it will be checked (the link denotes the branch to search). Note that
    * only the transaction hash should be checked, the output index is not
    * relevant for this search.
    * @param link The link that represents the top of the branch to search. If
    * this is an orphan link, the result is undefined.
    * @param in The transaction input claiming the output to find the link for.
    * @return The link that contains the claimed output, or null if no such
    * link exist.
    */
   BlockChainLink getClaimedLink(BlockChainLink link, TransactionInput in);

   /**
    * Find the link which contains the claimed transaction by the input given.
    * This method behaves exactly like @getClaimedLink but returns a block
    * with just the interesting transaction, for performance purposes
    * @param link The link that represents the top of the branch to search. If
    * this is an orphan link, the result is undefined.
    * @param in The transaction input claiming the output to find the link for.
    * @return The link that contains the claimed output, or null if no such
    * link exist.
    */
   BlockChainLink getPartialClaimedLink(BlockChainLink link, TransactionInput in);
   
   /**
    * Find the link which contains a claim for the same transaction output
    * that is claimed by the given input.
    * @param link The link with the block that represents the branch to search.
    * Supplying an orphan link yields an undefined result.
    * @param in The transaction input claiming the output to find the link for.
    * @return The link that contains the said input, or null if the same output
    * is not claimed in the given branch.
    */
   BlockChainLink getClaimerLink(BlockChainLink link, TransactionInput in);

   /**
    * Return true if a block contains a claim for the same transaction output
    * that is claimed by the given input.
    * @param link The link with the block that represents the branch to search.
    * Supplying an orphan link yields an undefined result.
    * @param in The transaction input claiming the output to find the link for.
    * @return True if a block in exists which contains the said input,
    * or null if the same output is not claimed in the given branch.
    */
   boolean outputClaimedInSameBranch(BlockChainLink link, TransactionInput in);

   /**
    * Save a link into the storage. If the link exists, it will be overwritten.
    */
   void addLink(BlockChainLink link);

   /**
    * @param height
    * @return The block at specified height of the best chain
    */
   byte[] getHashOfMainChainAtHeight(long height);

   /**
    * @param height
    * @return The block link at specified height of the best chain
    */
   BlockChainLink getLinkAtHeight(long height);
}
