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

package hu.netmind.bitcoin.net.message;

import hu.netmind.bitcoin.net.NodeAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @author Robert Brautigam
 */
public class NodeAddressImpl implements NodeAddress
{
   private long services;
   private InetSocketAddress address;

   public NodeAddressImpl(long services, InetSocketAddress address)
   {
      this.services=services;
      this.address=address;
   }

   NodeAddressImpl()
   {
   }

   void readFrom(BitCoinInputStream input)
      throws IOException
   {
      services = input.readUInt64();
      InetAddress addr = InetAddress.getByAddress(input.readBytes(16));
      long port = input.readUInt16BE();
      address = new InetSocketAddress(addr,(int) port);
   }

   void writeTo(BitCoinOutputStream output)
      throws IOException
   {
      output.writeUInt64(services);
      byte[] addr = address.getAddress().getAddress();
      if ( addr.length == 4 )
      {
         output.write(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte)0xff, (byte)0xff });
         output.write(addr);
      }
      else if ( addr.length == 16 )
      {
         output.write(addr);
      }
      else
         throw new IOException("ip address was not 4 or 16 bytes: "+Arrays.toString(addr));
      output.writeUInt16BE(address.getPort());
   }

   public String toString()
   {
      return address.toString()+" (services: "+services+")";
   }

   public long getServices()
   {
      return services;
   }

   public InetSocketAddress getAddress()
   {
      return address;
   }

}
