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

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * @author Robert Brautigam
 */
public class AddrMessage extends ChecksummedMessage
{
   private List<AddressEntry> addressEntries;

   public AddrMessage(long magic, List<AddressEntry> addressEntries)
      throws IOException
   {
      super(magic,"addr");
      this.addressEntries=addressEntries;
   }

   AddrMessage()
      throws IOException
   {
      super();
   }

   void readFrom(BitCoinInputStream input, long version, Object param)
      throws IOException
   {
      super.readFrom(input,version,param);
      addressEntries = new ArrayList<AddressEntry>();
      long count = input.readUIntVar();
      for ( long i=0; i<count; i++ )
      {
         long timestamp = 0;
         if ( version >= 31402 )
            timestamp = input.readUInt32()*1000;
         NodeAddress address = new NodeAddress();
         address.readFrom(input);
         addressEntries.add(new AddressEntry(timestamp,address));
      }
   }

   void writeTo(BitCoinOutputStream output, long version)
      throws IOException
   {
      super.writeTo(output,version);
      // Write size
      output.writeUIntVar(addressEntries.size());
      // Write all entries one by one
      for ( AddressEntry entry : addressEntries )
      {
         if ( version >= 31402 )
            output.writeUInt32(entry.getTimestamp()/1000);
         entry.getAddress().writeTo(output);
      }
   }

   public List<AddressEntry> getAddressEntries()
   {
      return (List<AddressEntry>) addressEntries;
   }

   public String toString()
   {
      return super.toString()+", addresses: "+addressEntries;
   }

   public static class AddressEntry
   {
      private long timestamp;
      private NodeAddress address;

      public String toString()
      {
         return address.toString();
      }

      public AddressEntry(long timestamp, NodeAddress address)
      {
         this.timestamp=timestamp;
         this.address=address;
      }

      public long getTimestamp()
      {
         return timestamp;
      }

      public NodeAddress getAddress()
      {
         return address;
      }
   }
}

