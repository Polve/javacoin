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

import java.io.OutputStream;
import java.io.IOException;

/**
 * This is an output stream supporting the types in the BitCoin
 * protocol.
 * @author Robert Brautigam
 */
public class BitCoinOutputStream extends OutputStream
{
   private OutputStream output;

   /**
    * Construct this output with an underlying output.
    */
   public BitCoinOutputStream(OutputStream output)
   {
      this.output=output;
   }

   /**
    * Forward to underlying stream.
    */
   public void write(int value)
      throws IOException
   {
      output.write(value);
   }

   /**
    * Forward bulk write to underlying output.
    */
   public void write(byte[] array)
      throws IOException
   {
      output.write(array);
   }

   /**
    * Write the array in reversed byte order.
    */
   public void writeReverse(byte[] array)
      throws IOException
   {
      output.write(ArraysUtil.reverse(array));
   }

   /**
    * Forward flush call to underlying stream.
    */
   public void flush()
      throws IOException
   {
      output.flush();
   }

   /**
    * Forward to underlying stream.
    */
   public void close()
      throws IOException
   {
      output.close();
   }

   public void writeU(long value)
      throws IOException
   {
      write((int)value);
   }

   public void writeUInt16(long value)
      throws IOException
   {
      write((int) value);
      write((int) (value>>8));
   }

   public void writeUInt16BE(long value)
      throws IOException
   {
      write((int) (value>>8));
      write((int) value);
   }

   public void writeUInt32(long value)
      throws IOException
   {
      write((int) value);
      write((int) (value>>8));
      write((int) (value>>16));
      write((int) (value>>24));
   }

   public void writeUInt32BE(long value)
      throws IOException
   {
      write((int) (value>>24));
      write((int) (value>>16));
      write((int) (value>>8));
      write((int) value);
   }

   public void writeUInt64(long value)
      throws IOException
   {
      write((int) value);
      write((int) (value>>8));
      write((int) (value>>16));
      write((int) (value>>24));
      write((int) (value>>32));
      write((int) (value>>40));
      write((int) (value>>48));
      write((int) (value>>56));
   }

   public void writeUIntVar(long value)
      throws IOException
   {
      if ( (value>=0) && (value < 0xfd) )
      {
         writeU(value);
      }
      else if ( (value>=0) && (value <= 0xffff) )
      {
         writeU(0xfd);
         writeUInt16(value);
      }
      else if ( (value>=0) && (value <= 0xffffffffl) )
      {
         writeU(0xfe);
         writeUInt32(value);
      }
      else
      {
         writeU(0xff);
         writeUInt64(value);
      }
   }

   /**
    * Write variable length string to stream.
    */
   public void writeString(String value)
      throws IOException
   {
      writeUIntVar(value.length());
      write(value.getBytes());
   }

   /**
    * Write a string null-padded into the given length.
    */
   public void writeString(String value, long length)
      throws IOException
   {
      byte[] valueBytes = value.getBytes();
      if ( valueBytes.length > length )
         throw new IOException("tried to write a string longer than given length: "+value+" into "+length);
      for ( int i=0; i<valueBytes.length; i++ )
         writeU( (valueBytes[i] & 0xff) );
      for ( int i=0; i< (length-valueBytes.length); i++ )
         writeU(0);
   }
}

