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

package it.nibbles.javacoin.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Robert Brautigam
 */
public class InvMessage extends Message
{
   private List<InventoryItem> items;

   public InvMessage(long magic, List<InventoryItem> items)
      throws IOException
   {
      super(magic,"inv");
      this.items=items;
   }

   InvMessage()
      throws IOException
   {
      super();
   }

   @Override
   void readFrom(BitcoinInputStream input, long version, Object param)
      throws IOException
   {
      super.readFrom(input,version,param);
      long size = input.readUIntVar();
      items = new ArrayList<InventoryItem>();
      for ( long i=0; i<size; i++ )
      {
         InventoryItem item = new InventoryItem();
         item.readFrom(input);
         items.add(item);
      }
   }

   @Override
   void writeTo(BitcoinOutputStream output, long version)
      throws IOException
   {
      super.writeTo(output,version);
      output.writeUIntVar(items.size());
      for ( InventoryItem item : items )
         item.writeTo(output);
   }

   @Override
   public String toString()
   {
      return super.toString()+" items: "+items;
   }

   public List<InventoryItem> getInventoryItems()
   {
      return items;
   }

}

