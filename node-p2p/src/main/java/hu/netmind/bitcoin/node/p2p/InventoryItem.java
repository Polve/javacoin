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

package hu.netmind.bitcoin.node.p2p;

import hu.netmind.bitcoin.net.InventoryItem;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Robert Brautigam
 */
public class InventoryItem
{
   public static final enum Type
   {
      TYPE_ERROR,
      TYPE_TX,
      TYPE_BLOCK,
   };

   private int type = TYPE_ERROR;
   private byte[] hash;

   public InventoryItem(int type, byte[] hash)
   {
      this.type=type;
      this.hash=hash;
   }

   InventoryItem()
   {
   }

   void readFrom(BitCoinInputStream input)
      throws IOException
   {
      type = (int) input.readUInt32();
      hash = input.readBytes(32);
   }

   void writeTo(BitCoinOutputStream output)
      throws IOException
   {
      output.writeUInt32(type);
      output.write(hash);
   }

   public String toString()
   {
      return type+":"+Arrays.toString(hash);
   }

   public int getType()
   {
      return type;
   }

   public byte[] getHash()
   {
      return hash;
   }
}

