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

import java.io.ByteArrayOutputStream;

/**
 * This BitCoin output stream implementation uses a byte array backing that can be
 * view anytime.
 * @author Robert Brautigam
 */
public class ByteArrayBitCoinOutputStream extends BitCoinOutputStream
{
   private ByteArrayOutputStream output = new ByteArrayOutputStream();

   public void write(int value)
   {
      output.write(value);
   }

   public String toString()
   {
      return HexUtil.toHexString(output.toByteArray());
   }

}

