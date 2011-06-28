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
 * A block path is a single path from the genesis block to one selected leaf node.
 * The "longest" of these paths is what called "longest chain" in bitcoin terminology.
 * The term "path" here is used to distinguish between the whole tree and a single path
 * which both are called "block chain" in bitcoin terminology.
 * @author Robert Brautigam
 */
public interface BlockPath
{
   /**
    * Get the blocks starting from the genesis block as a list.
    */
   List<Block> getBlocks();

   /**
    * Get the claimer input for a given output.
    * @return The input if the output's transaction is in this path,
    * and an input is found in this path which claims this output. 
    * Null otherwise.
    */
   TransactionInput getClaimerInput(TransactionOutput output);
}

