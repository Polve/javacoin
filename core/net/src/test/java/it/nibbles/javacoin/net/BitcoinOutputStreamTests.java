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

package it.nibbles.javacoin.net;

import it.nibbles.javacoin.net.HexUtil;
import it.nibbles.javacoin.net.BitcoinOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Robert Brautigam
 */
@Test
public class BitcoinOutputStreamTests
{
   private static final Logger logger = LoggerFactory.getLogger(BitcoinOutputStreamTests.class);

   public void testWriteUMax()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeU(0xff);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"FF");
   }

   public void testWriteUInt16()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUInt16(0xfdec);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"EC FD");
   }

   public void testWriteUInt16BE()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUInt16BE(0xfdec);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"FD EC");
   }

   public void testWriteUInt32()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUInt32(0xfdec1122);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"22 11 EC FD");
   }

   public void testWriteUInt32BE()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUInt32BE(0xfdec1122);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"FD EC 11 22");
   }

   public void testWriteUInt64()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUInt64(0xfdec112255667788l);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"88 77 66 55 22 11 EC FD");
   }

   public void testWriteUIntVar8()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUIntVar(0xec);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"EC");
   }

   public void testWriteUIntVar16()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUIntVar(0xec22);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"FD 22 EC");
   }

   public void testWriteUIntVar32()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUIntVar(0xec221100l);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"FE 00 11 22 EC");
   }

   public void testWriteUIntVar64()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeUIntVar(0xec11223344556677l);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"FF 77 66 55 44 33 22 11 EC");
   }

   public void testWriteFixStringEmpty()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeString("",8);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"00 00 00 00 00 00 00 00");
   }

   public void testWriteFixStringNormal()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeString("ABCD",8);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"41 42 43 44 00 00 00 00");
   }

   public void testWriteFixStringFull()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeString("ABCDEFGH",8);
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"41 42 43 44 45 46 47 48");
   }

   public void testWriteVarStringEmpty()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeString("");
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"00");
   }
   
   public void testWriteVarStringNormal()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeString("ABCD");
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"04 41 42 43 44");
   }

   public void testWriteByteArray()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.write(new byte[] { (byte)0x04, (byte)0x10, (byte)0xFF });
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"04 10 FF");
   }

   public void testWriteReverseByteArray()
      throws IOException
   {
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      output.writeReverse(new byte[] { (byte)0x04, (byte)0x10, (byte)0xFF });
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),"FF 10 04");
   }
}

