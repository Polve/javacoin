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

/**
 * @author Robert Brautigam
 */
public class VersionMessage extends Message
{
   private long version;
   private long services;
   private long timestamp;
   private NodeAddress receiverAddress;
   private NodeAddress senderAddress;
   private long nonce;
   private String secondaryVersion;
   private long startHeight;

   public VersionMessage(long magic, long version, long services, long timestamp,
         NodeAddress receiverAddress, NodeAddress senderAddress, long nonce, String secondaryVersion, long startHeight)
   {
      super(magic,"version");
      this.version=version;
      this.services=services;
      this.timestamp=timestamp;
      this.receiverAddress=receiverAddress;
      this.senderAddress=senderAddress;
      this.nonce=nonce;
      this.secondaryVersion=secondaryVersion;
      this.startHeight=startHeight;
   }

   VersionMessage()
   {
      super();
   }

   void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      super.readFrom(input,protocolVersion,param);
      version = input.readUInt32();
      services = input.readUInt64();
      timestamp = input.readUInt64()*1000; // We need milliseconds not seconds
      if ( timestamp < 0 )
         throw new IOException("timestamp for version message is out of supported range");
      receiverAddress = new NodeAddress();
      receiverAddress.readFrom(input);
      if ( version >= 106 )
      {
         senderAddress = new NodeAddress();
         senderAddress.readFrom(input);
         nonce = input.readUInt64();
         secondaryVersion = input.readString();
         if ( version >= 209 )
         {
            startHeight = input.readUInt32();
         }
      }
   }

   void writeTo(BitCoinOutputStream output, long protocolVersion)
      throws IOException
   {
      super.writeTo(output,protocolVersion);
      output.writeUInt32(version);
      output.writeUInt64(services);
      output.writeUInt64(timestamp/1000); // Convert millis to seconds
      receiverAddress.writeTo(output);
      if ( version >= 106 )
      {
         senderAddress.writeTo(output);
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
      return super.toString()+" version: "+version+", services: "+services+
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

