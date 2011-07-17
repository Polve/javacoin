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

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Robert Brautigam
 */
public class HeadersMessage extends ChecksummedMessage
{
   private List<BlockHeader> headers;

   public HeadersMessage(long magic, List<BlockHeader> headers)
      throws IOException
   {
      super(magic,"headers");
      this.headers=headers;
   }

   HeadersMessage()
      throws IOException
   {
      super();
   }

   void readFrom(BitCoinInputStream input, long protocolVersion, Object param)
      throws IOException
   {
      super.readFrom(input,protocolVersion,param);
      long headerCount = input.readUIntVar();
      if ( (headerCount<0) || (headerCount>=Integer.MAX_VALUE) )
         throw new IOException("too many headers in the block: "+headerCount);
      headers = new ArrayList<BlockHeader>();
      for ( long i=0; i<headerCount; i++ )
      {
         BlockHeader header = new BlockHeader();
         header.readFrom(input,protocolVersion,param);
         headers.add(header);
      }
   }

   void writeTo(BitCoinOutputStream output, long protocolVersion)
      throws IOException
   {
      super.writeTo(output,protocolVersion);
      output.writeUIntVar(headers.size());
      for ( BlockHeader header : headers )
         header.writeTo(output);
   }

   public String toString()
   {
      return super.toString()+" headers: "+headers;
   }

   public List<BlockHeader> getHeaders()
   {
      return headers;
   }


}

