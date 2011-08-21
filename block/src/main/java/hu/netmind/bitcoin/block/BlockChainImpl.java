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

import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.VerificationException;
import java.util.Observable;

/**
 * The BlockChain is responsible for maintaining the list of valid Blocks
 * and also calculating the longest chain starting from the Genesis Block.
 * @author Robert Brautigam
 */
public class BlockChainImpl extends Observable implements BlockChain
{
   private BlockImpl genesisBlock = null;
   private BlockStorage blockStorage = null;

   public BlockChainImpl(BlockImpl genesisBlock, BlockStorage blockStorage)
      throws VerificationException
   {
      this.genesisBlock=genesisBlock;
      this.blockStorage=blockStorage;
      // Check if the genesis blocks equal, or add genesis block if storage is empty.
      // Here we assume that the storage is not tampered!
      BlockImpl storedGenesisBlock = blockStorage.getGenesisBlock();
      if ( storedGenesisBlock == null )
      {
         addBlock(genesisBlock);
      } else {
         if ( ! storedGenesisBlock.equals(genesisBlock) )
            throw new VerificationException("genesis block in storage is not the same as the block chain's");
      }
   }

   /**
    * Add a block to the chain. The block is only added if it is verified, and
    * passes all known checks. If the block already exists in the chain, nothing
    * is done (there are no changes).
    */
   public void addBlock(BlockImpl block)
      throws VerificationException
   {
      // If block exists, do nothing
      if ( blockStorage.getBlock(block.getHash()) != null )
         return;
      // TODO: verify block
   }

   public Block getGenesisBlock()
   {
      return genesisBlock;
   }

   public Block getLastBlock()
   {
      return blockStorage.getLastBlock();
   }

}

