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

/**
 * Implement this interface to store block and transaction objects.
 * @author Robert Brautigam
 */
public interface BlockStorage
{
   /**
    * Get the genesis block of this storage. This might be null if there are no
    * blocks yet in the storage.
    */
   BlockImpl getGenesisBlock();

   /**
    * Get the block stored which has the greatest aggregated complexity
    * and is connected to the genesis block.
    */
   BlockImpl getLastBlock();

   /**
    * Get the block to the given hash value.
    * @return The Block with the given hash, or null if no such block exists.
    */
   BlockImpl getBlock(byte[] hash);

   /**
    * Save a block into the storage. If the block exists, it will be overwritten.
    */
   void addBlock(BlockImpl block);
}

