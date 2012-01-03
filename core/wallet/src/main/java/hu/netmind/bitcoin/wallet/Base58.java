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

package hu.netmind.bitcoin.wallet;

import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Base58 algorithm using the original BitCoin
 * alphabet.
 * @author Robert Brautigam
 */
public class Base58
{
   private static final Logger logger = LoggerFactory.getLogger(Base58.class);

   // The alphabet is copied from the original bitcoin source
   private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
   private static final BigInteger BASE = BigInteger.valueOf(58);

   /**
    * Encode a byte array using this encoding.
    */
   public static String encode(byte[] data)
   {
      // Convert to base56
      BigInteger value = new BigInteger(1,data);
      StringBuilder builder = new StringBuilder();
      while ( value.compareTo(BigInteger.ZERO) > 0 )
      {
         BigInteger[] result = value.divideAndRemainder(BASE);
         builder.insert(0,ALPHABET.charAt(result[1].intValue()));
         value = result[0];
      }
      // Prepend with leading zeroes (one for all zero bytes)
      for ( int i=0; (i<data.length) && (data[i]==0); i++ )
         builder.insert(0,ALPHABET.charAt(0));
      // Return the string
      return builder.toString();
   }

   /**
    * Decode a string into a byte array.
    */
   public static byte[] decode(String str)
   {
      BigInteger value = BigInteger.ZERO;
      int zeroCount = 0;
      for ( int i=0; i<str.length(); i++ )
      {
         if ( (str.charAt(i) == '1') && (value==BigInteger.ZERO) )
         {
            zeroCount++;
            continue; // Just skip zeroes, but remember how many it was
         }
         int num = ALPHABET.indexOf(str.charAt(i));
         if ( num < 0 )
            throw new NumberFormatException("found character '"+str.charAt(i)+"' which is not a valid base58 character");
         value = value.multiply(BASE);
         value = value.add(BigInteger.valueOf(num));
      }
      // Get bytes back, sometime it has leading zeros!
      byte[] valueBytes = value.toByteArray();
      int phantomZeros = 0;
      while ( valueBytes[phantomZeros] == 0 )
         phantomZeros++;
      // Return result with zeros padded
      byte[] result = new byte[valueBytes.length+zeroCount-phantomZeros];
      System.arraycopy(valueBytes,phantomZeros,result,zeroCount,valueBytes.length-phantomZeros);
      return result;
   }
}


