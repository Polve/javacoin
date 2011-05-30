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

import hu.netmind.bitcoin.net.Version;
import java.io.IOException;

/**
 * @author Robert Brautigam
 */
public class VersionImpl extends MessageImpl implements Version
{
   private long version;
   private long services;
   private long timestamp;
   private NodeAddressImpl senderAddress;
   private NodeAddressImpl receiverAddress;
   private long nonce;
   private String secondaryVersion;
   private long startHeight;

   public VersionImpl(long magic, long version, long services, long timestamp,
         NodeAddressImpl senderAddress, NodeAddressImpl receiverAddress, long nonce, String secondaryVersion, long startHeight)
   {
      super(magic,"version");
      this.version=version;
      this.services=services;
      this.timestamp=timestamp;
      this.senderAddress=senderAddress;
      this.receiverAddress=receiverAddress;
      this.nonce=nonce;
      this.secondaryVersion=secondaryVersion;
      this.startHeight=startHeight;
   }

   public VersionImpl(BitCoinInputStream input, Object param)
      throws IOException
   {
      super(input,param);
      version = input.readUInt32();
      services = input.readUInt64();
      timestamp = input.readUInt64();
      if ( timestamp < 0 )
         throw new IOException("timestamp for version message is out of supported range");
      senderAddress = new NodeAddressImpl(input);
      if ( version >= 106 )
      {
         receiverAddress = new NodeAddressImpl(input);
         nonce = input.readUInt64();
         secondaryVersion = input.readString();
         if ( version >= 209 )
         {
            startHeight = input.readUInt32();
         }
      }
   }

   void preWriteTo(BitCoinOutputStream output)
      throws IOException
   {
      super.preWriteTo(output);
      output.writeUInt32(version);
      output.writeUInt64(services);
      output.writeUInt64(timestamp);
      senderAddress.writeTo(output);
      if ( version >= 106 )
      {
         receiverAddress.writeTo(output);
         output.writeUInt64(nonce);
         output.writeString(secondaryVersion);
         if ( version >= 209 )
         {
            output.writeUInt32(startHeight);
         }
      }
   }

   public String toString()
   {
      super.toString()+" version: "+version+", services: "+services+
         ", sender: "+senderAddress+", receiver: "+receiverAddress+
         ", secondary version: "+secondaryVersion+", start height: "+startHeight;
   }

   public long getVersion()
   {
      return version;
   }

   public long getServices()
   {
      return services;
   }

   public long getTimestamp()
   {
      return timestamp;
   }

   public NodeAddress getSenderAddress()
   {
      return senderAddress;
   }

   public NodeAddress getReceiverAddress()
   {
      return receiverAddress;
   }

   public long getNonce()
   {
      return nonce;
   }

   public String getSecondaryVersion()
   {
      return secondaryVersion;
   }

   public long getStartHeight()
   {
      return startHeight;
   }

}
