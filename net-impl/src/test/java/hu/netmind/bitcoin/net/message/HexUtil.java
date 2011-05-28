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

import java.util.StringTokenizer;

/**
 * @author Robert Brautigam
 */
public class HexUtil
{
   /**
    * Helper method to convert strings like: "F0 A4 32 05", to
    * actual byte array for easier testing.
    */
   public static byte[] toByteArray(String hexString)
   {
      StringTokenizer tokenizer = new StringTokenizer(hexString," ");
      byte[] result = new byte[tokenizer.countTokens()];
      for ( int i=0; i<result.length; i++ )
         result[i] = (byte) Integer.valueOf(tokenizer.nextToken(),16).intValue();
      return result;
   }

}


