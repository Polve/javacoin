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

package hu.netmind.bitcoin.impl.wallet;

import hu.netmind.bitcoin.api.Block;

/**
 * This cache stores balance values to specific block entries.
 * @author Robert Brautigam
 */
public interface BlockBalanceCache
{
   /**
    * Add an entry into the cache.
    */
   void addEntry(Block block, Long balance);

   /**
    * Get a balance for a given block.
    * @return The balance value or null if it is not in the cache.
    */
   Long getEntry(Block block);
}

