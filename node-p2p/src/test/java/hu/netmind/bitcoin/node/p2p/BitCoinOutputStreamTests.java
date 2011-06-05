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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert Brautigam
 */
@Test
public class BitCoinOutputStreamTests
{
   private static final Logger logger = LoggerFactory.getLogger(BitCoinOutputStreamTests.class);

   public void testWriteUMax()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeU(0xff);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"FF");
   }

   public void testWriteUInt16()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUInt16(0xfdec);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"EC FD");
   }

   public void testWriteUInt16BE()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUInt16BE(0xfdec);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"FD EC");
   }

   public void testWriteUInt32()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUInt32(0xfdec1122);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"22 11 EC FD");
   }

   public void testWriteUInt32BE()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUInt32BE(0xfdec1122);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"FD EC 11 22");
   }

   public void testWriteUInt64()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUInt64(0xfdec112255667788l);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"88 77 66 55 22 11 EC FD");
   }

   public void testWriteUIntVar8()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUIntVar(0xec);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"EC");
   }

   public void testWriteUIntVar16()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUIntVar(0xec22);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"FD 22 EC");
   }

   public void testWriteUIntVar32()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUIntVar(0xec221100l);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"FE 00 11 22 EC");
   }

   public void testWriteUIntVar64()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUIntVar(0xec11223344556677l);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"FF 77 66 55 44 33 22 11 EC");
   }

   public void testWriteFixStringEmpty()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeString("",8);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"00 00 00 00 00 00 00 00");
   }

   public void testWriteFixStringNormal()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeString("ABCD",8);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"41 42 43 44 00 00 00 00");
   }

   public void testWriteFixStringFull()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeString("ABCDEFGH",8);
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"41 42 43 44 45 46 47 48");
   }

   public void testWriteVarStringEmpty()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeString("");
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"00");
   }
   
   public void testWriteVarStringNormal()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeString("ABCD");
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),"04 41 42 43 44");
   }
}

