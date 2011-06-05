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

/**
 * This output stream is used to overwrite some values in an existing
 * serialized message. This is useful for values that can be computed
 * only after the bulk of the message was already serialized, like length
 * and checksum values.
 * @author Robert Brautigam
 */
public class OverwriterBitCoinOutputStream extends BitCoinOutputStream
{
   private byte[] byteArray;
   private int position = 0; 

   /**
    * @param byteArray The array to overwrite content in.
    * @param offset The offset at which to begin overwriting.
    */
   public OverwriterBitCoinOutputStream(byte[] byteArray, int offset)
   {
      this.byteArray=byteArray;
      this.position=offset;
   }

   public void write(int value)
      throws IOException
   {
      if ( position >= byteArray.length )
         throw new IOException("tried to write past the array length to position "+position);
      byteArray[position++] = (byte) value;
   }
}

