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
public class BitCoinOutputStreamTests
{
   private static final Logger logger = LoggerFactory.getLogger(BitCoinOutputStreamTests.class);

   public void testWriteUMax()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeU(0xff);
      Assert.assertEquals(output.toString(),"FF");
   }

   public void testWriteUInt16()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUInt16(0xfdec);
      Assert.assertEquals(output.toString(),"EC FD");
   }

   public void testWriteUInt16BE()
      throws IOException
   {
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      output.writeUInt16BE(0xfdec);
      Assert.assertEquals(output.toString(),"FD EC");
   }

}

