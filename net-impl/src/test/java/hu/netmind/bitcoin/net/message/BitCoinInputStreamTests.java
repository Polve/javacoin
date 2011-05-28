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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Brautigam
 */
@Test
public class BitCoinInputStreamTests
{
   private static final Logger logger = LoggerFactory.getLogger(BitCoinInputStreamTests.class);

   /**
    * Helper method to convert strings like: "F0 A4 32 05", to
    * actual byte array for easier testing.
    */
   private byte[] toByteArray(String hexString)
   {
      StringTokenizer tokenizer = new StringTokenizer(hexString," ");
      byte[] result = new byte[tokenizer.countTokens()];
      for ( int i=0; i<result.length; i++ )
         result[i] = (byte) Integer.valueOf(tokenizer.nextToken(),16).intValue();
      logger.debug("converted {} to byte array; {}",hexString,Arrays.toString(result));
      return result;
   }

   public void testReadUMax()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(toByteArray("FF"));
      Assert.assertEquals(input.readU(),0xFF);
   }

   public void testReadUInt16()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(toByteArray("FA 43"));
      Assert.assertEquals(input.readUInt16(),0x43FA);
   }

   public void testReadUInt16BE()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(toByteArray("43 FA"));
      Assert.assertEquals(input.readUInt16BE(),0x43FA);
   }

   public void testReadUInt32()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(toByteArray("FA 43 11 F2"));
      Assert.assertEquals(input.readUInt32(),0xF21143FAl);
   }

   public void testReadUInt64Low()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            toByteArray("FA 43 11 F2 32 5F 6E 4F"));
      Assert.assertEquals(input.readUInt64(),0x4F6E5F32F21143FAl);
   }

   public void testReadUInt64High()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            toByteArray("FA 43 11 F2 32 5F 6E AA"));
      Assert.assertEquals(input.readUInt64(),0xAA6E5F32F21143FAl);
   }
}

