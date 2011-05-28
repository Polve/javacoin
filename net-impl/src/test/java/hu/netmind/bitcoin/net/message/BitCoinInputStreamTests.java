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

   public void testReadUMax()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream("FF");
      Assert.assertEquals(input.readU(),0xFF);
   }

   public void testReadUInt16()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream("FA 43");
      Assert.assertEquals(input.readUInt16(),0x43FA);
   }

   public void testReadUInt16BE()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream("43 FA");
      Assert.assertEquals(input.readUInt16BE(),0x43FA);
   }

   public void testReadUInt32()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream("FA 43 11 F2");
      Assert.assertEquals(input.readUInt32(),0xF21143FAl);
   }

   public void testReadUInt64Low()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "FA 43 11 F2 32 5F 6E 4F");
      Assert.assertEquals(input.readUInt64(),0x4F6E5F32F21143FAl);
   }

   public void testReadUInt64High()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "FA 43 11 F2 32 5F 6E AA");
      Assert.assertEquals(input.readUInt64(),0xAA6E5F32F21143FAl);
   }

   @Test( expectedExceptions = IOException.class )
   public void testOverread()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream("54 21 55");
      input.readUInt32();
   }

   @Test( expectedExceptions = IOException.class )
   public void testReadEmpty()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(new byte[] {});
      input.readUInt16();
   }

   public void testReadUIntVar8()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "11 22 33 44");
      Assert.assertEquals(input.readUIntVar(),0x11);
   }

   public void testReadUIntVar16()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "FD 22 33 44");
      Assert.assertEquals(input.readUIntVar(),0x3322);
   }

   public void testReadUIntVar32()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "FE 22 33 44 55");
      Assert.assertEquals(input.readUIntVar(),0x55443322l);
   }

   public void testReadUIntVar64()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "FF 22 33 44 55 66 77 88 99");
      Assert.assertEquals(input.readUIntVar(),0x9988776655443322l);
   }

   public void testReadFixStringEmpty()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "00 00 00 00 00 00 00 00");
      Assert.assertEquals(input.readString(8),"");
   }

   public void testReadFixStringNormal()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "41 42 43 44 00 00 00 00");
      Assert.assertEquals(input.readString(8),"ABCD");
   }

   public void testReadFixStringFull()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "41 42 43 44 45 46 47 48");
      Assert.assertEquals(input.readString(8),"ABCDEFGH");
   }

   @Test ( expectedExceptions = IOException.class )
   public void testReadFixStringNotPropelyPadded()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(
            "41 42 43 44 00 00 01 00");
      input.readString(8);
   }
}

