/**
 * Copyright (C) 2012 nibbles.it
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package it.nibbles.javacoin.block;

import it.nibbles.javacoin.Block;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Alessandro Polverini
 */
public class OrphanBlockSet
{

   private Map<HashWrapper, List<SoftReference<Block>>> cacheByPrevHash = new HashMap<>();

   public void addBlock(Block b)
   {
      SoftReference ref = new SoftReference(b);
      List<SoftReference<Block>> list = cacheByPrevHash.get(new HashWrapper(b.getPreviousBlockHash()));
      if (list == null)
      {
         list = new LinkedList<>();
         cacheByPrevHash.put(new HashWrapper(b.getPreviousBlockHash()), list);
      }
      list.add(ref);
   }

   public Block removeBlockByPreviousHash(byte[] hash)
   {
      List<SoftReference<Block>> list = cacheByPrevHash.get(new HashWrapper(hash));
      if (list == null)
         return null;
      while (!list.isEmpty())
      {
         SoftReference<Block> ref = list.remove(0);
         if (list.isEmpty())
            cacheByPrevHash.remove(new HashWrapper(hash));
         Block b = ref.get();
         if (b != null)
            return b;
      }
      return null;
   }

}
