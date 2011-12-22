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

package hu.netmind.bitcoin.block.bdb;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * @author Robert Brautigam
 */
public class BytesBinding extends TupleBinding<byte[]>
{
   public byte[] entryToObject(TupleInput in)
   {
      return readBytes(in,32);
   }

   public void objectToEntry(byte[] hash, TupleOutput out)
   {
      out.write(hash);
   }

   static byte[] readBytes(TupleInput in, int length)
   {
      byte[] result = new byte[length];
      int offset = 0;
      int read = 0;
      while ( offset < length )
      {
         read = in.read(result,offset,length-offset);
         if ( read < 0 )
            throw new BDBStorageException("could not read serialized form of link, end-of-stream");
         offset+=read;
      }
      return result;
   }

}

