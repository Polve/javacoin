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

import hu.netmind.bitcoin.Block;
import java.io.Serializable;

/**
 * One link in a block chain. The representation of a link
 * in a block chain is more than just the block itself, it has some status information
 * and aggregated values about the block. The only information which might change
 * during the lifetime of a link is the orphan attribute, which might start out as
 * 'true' and might change to 'false' later, at which point no other modification
 * are allowed.
 * @author Robert Brautigam
 */
public class BlockChainLink implements Serializable
{
   public static final long ROOT_HEIGHT = 0;

   private Block block;
   private Difficulty totalDifficulty;
   private long height;

   public BlockChainLink(Block block, Difficulty totalDifficulty,
         long height)
   {
      this.block=block;
      this.totalDifficulty=totalDifficulty;
      this.height=height;
   }

   public Block getBlock()
   {
      return block;
   }

   public Difficulty getTotalDifficulty()
   {
      return totalDifficulty;
   }

   public long getHeight()
   {
      return height;
   }

   @Override
   public String toString()
   {
      return "Link at height "+height+" block: "+block;
   }
}

