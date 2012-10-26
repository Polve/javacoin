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
package hu.netmind.bitcoin.block;

import hu.netmind.bitcoin.Block;
import java.lang.ref.SoftReference;
import java.util.Arrays;
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

   class HashWrapper
   {

      byte[] hash;

      public HashWrapper(byte[] hash)
      {
         this.hash = hash;
      }

      @Override
      public int hashCode()
      {
         int len = hash.length;
         switch (len) {
            case 0: return 0;
            case 1: return hash[0];
            case 2: return hash[1]+hash[0]*256;
            default: return hash[len-1]+hash[len-2]*256+hash[len-3]*256*256;
         }
         //return hash[31] + hash[30] * 256 + hash[29] * 256 * 256;
      }

      @Override
      public boolean equals(Object obj)
      {
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         final HashWrapper other = (HashWrapper) obj;
         if (!Arrays.equals(this.hash, other.hash))
            return false;
         return true;
      }
   }
}
