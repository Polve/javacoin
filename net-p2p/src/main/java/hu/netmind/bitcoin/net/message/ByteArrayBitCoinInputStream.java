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

/**
 * This is a BitCoin input stream which reads messages from a given byte array.
 * @author Robert Brautigam
 */
public class ByteArrayBitCoinInputStream extends BitCoinInputStream
{
   private byte[] bytes = null;
   private int pointer = 0;
   private int mark = 0;

   public boolean markSupported()
   {
      return true;
   }

   public void mark(int length)
   {
      mark = pointer;
   }

   public void reset()
   {
      pointer = mark;
   }

   public ByteArrayBitCoinInputStream(byte[] bytes)
   {
      this.bytes=bytes;
   }

   public int read()
   {
      if ( (bytes==null) || (pointer > bytes.length-1) )
         return -1;
      int result = (bytes[pointer++] & 0xff);
      return result;
   }
}

