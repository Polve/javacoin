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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Robert Brautigam
 */
public class GetHeadersMessage extends Message
{
   private List<byte[]> hashStarts;
   private byte[] hashStop;

   public GetHeadersMessage(long magic, List<byte[]> hashStarts, byte[] hashStop)
      throws IOException
   {
      super(magic,"getheaders");
      this.hashStarts=hashStarts;
      this.hashStop=hashStop;
   }

   GetHeadersMessage()
      throws IOException
   {
      super();
   }

   @Override
   void readFrom(BitCoinInputStream input, long version, Object param)
      throws IOException
   {
      super.readFrom(input,version,param);
      long size = input.readUIntVar();
      hashStarts = new ArrayList<byte[]>();
      for ( long i=0; i<size; i++ )
         hashStarts.add(input.readReverseBytes(32));
      hashStop = input.readReverseBytes(32);
   }

   @Override
   void writeTo(BitCoinOutputStream output, long version)
      throws IOException
   {
      super.writeTo(output,version);
      output.writeUIntVar(hashStarts.size());
      for ( byte[] hash : hashStarts )
         output.writeReverse(hash);
      output.writeReverse(hashStop);
   }

   @Override
   public String toString()
   {
      return super.toString()+" hash starts: "+hashStarts+", stop: "+hashStop;
   }

   public List<byte[]> getHashStarts()
   {
      return hashStarts;
   }

   public byte[] getHashStop()
   {
      return hashStop;
   }
}

