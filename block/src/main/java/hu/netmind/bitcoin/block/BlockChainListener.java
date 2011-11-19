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
 * Used for listening for block chain internal events.
 * @author Robert Brautigam
 */
public interface BlockChainListener
{
   /**
    * Called when the chain receives a block for which the previous
    * block does not exist. The block itself will be stored as
    * orphan.
    */
   void notifyMissingBlock(byte[] blockHash);

   /**
    * Called when a fully verified block enters a chain.
    */
   void notifyAddedBlock(BlockImpl block);
}

