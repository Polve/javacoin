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

package hu.netmind.bitcoin.net;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
public class GetDataMessage extends ChecksummedMessage
{
   private List<InventoryItem> items;

   public GetDataMessage(long magic, List<InventoryItem> items)
      throws IOException
   {
      super(magic,"getdata");
      this.items=items;
   }

   GetDataMessage()
      throws IOException
   {
      super();
   }

   void readFrom(BitCoinInputStream input, long version, Object param)
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

   void writeTo(BitCoinOutputStream output, long version)
      throws IOException
   {
      super.writeTo(output,version);
      output.writeUIntVar(items.size());
      for ( InventoryItem item : items )
         item.writeTo(output);
   }

   public String toString()
   {
      return super.toString()+" items: "+items;
   }

   public List<InventoryItem> getInventoryItems()
   {
      return items;
   }

}
