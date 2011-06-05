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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import java.util.Arrays;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
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
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FF"));
      Assert.assertEquals(input.readU(),0xFF);
   }

   public void testReadUInt16()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FA 43"));
      Assert.assertEquals(input.readUInt16(),0x43FA);
   }

   public void testReadUInt16BE()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("43 FA"));
      Assert.assertEquals(input.readUInt16BE(),0x43FA);
   }

   public void testReadUInt32()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FA 43 11 F2"));
      Assert.assertEquals(input.readUInt32(),0xF21143FAl);
   }

   public void testReadUInt32BE()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FA 43 11 F2"));
      Assert.assertEquals(input.readUInt32BE(),0xFA4311F2l);
   }

   public void testReadUInt64Low()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FA 43 11 F2 32 5F 6E 4F"));
      Assert.assertEquals(input.readUInt64(),0x4F6E5F32F21143FAl);
   }

   public void testReadUInt64High()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FA 43 11 F2 32 5F 6E AA"));
      Assert.assertEquals(input.readUInt64(),0xAA6E5F32F21143FAl);
   }

   @Test( expectedExceptions = IOException.class )
   public void testOverread()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("54 21 55"));
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
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("11 22 33 44"));
      Assert.assertEquals(input.readUIntVar(),0x11);
   }

   public void testReadUIntVar16()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FD 22 33 44"));
      Assert.assertEquals(input.readUIntVar(),0x3322);
   }

   public void testReadUIntVar32()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FE 22 33 44 55"));
      Assert.assertEquals(input.readUIntVar(),0x55443322l);
   }

   public void testReadUIntVar64()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("FF 22 33 44 55 66 77 88 99"));
      Assert.assertEquals(input.readUIntVar(),0x9988776655443322l);
   }

   public void testReadFixStringEmpty()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("00 00 00 00 00 00 00 00"));
      Assert.assertEquals(input.readString(8),"");
   }

   public void testReadFixStringNormal()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("41 42 43 44 00 00 00 00"));
      Assert.assertEquals(input.readString(8),"ABCD");
   }

   public void testReadFixStringFull()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("41 42 43 44 45 46 47 48"));
      Assert.assertEquals(input.readString(8),"ABCDEFGH");
   }

   @Test ( expectedExceptions = IOException.class )
   public void testReadFixStringNotPropelyPadded()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("41 42 43 44 00 00 01 00"));
      input.readString(8);
   }

   public void testReadVarStringEmpty()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("00"));
      Assert.assertEquals(input.readString(),"");
   }

   public void testReadVarStringNormal()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("04 41 42 43 44"));
      Assert.assertEquals(input.readString(),"ABCD");
   }

   public void testReadByteArray()
      throws IOException
   {
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("04 FF A3"));
      byte[] result = input.readBytes(3);
      Assert.assertEquals(result.length, 3);
      Assert.assertEquals(result[0] & 0xff, 0x04);
      Assert.assertEquals(result[1] & 0xff, 0xff);
      Assert.assertEquals(result[2] & 0xff, 0xa3);
   }

   public void testListener()
      throws IOException
   {
      final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray("04 FF A3"));
      input.setListener(new BitCoinInputStream.Listener() {
               public void update(int value)
               {
                  byteOutput.write(value);
               }
            });
      byte[] result = input.readBytes(3);
      Assert.assertEquals(result,byteOutput.toByteArray());
   }
}

