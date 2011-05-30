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
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hu.netmind.bitcoin.net.*;

/**
 * Tests whether messages serialize and deserialize correctly.
 * @author Robert Brautigam
 */
@Test
public class MessageTests
{
   public void testVerackDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+
            "76 65 72 61  63 6B 00 00 00 00 00 00 "+
            "00 00 00 00"));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      Verack message = (Verack) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"verack");
      Assert.assertEquals(message.getLength(),0);
   }

   public void testVerackSerialize()
      throws IOException
   {
      // Setup a verack message
      VerackImpl verack = new VerackImpl(Message.MAGIC_MAIN);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(verack,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
            "F9 BE B4 D9 "+
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+
            "00 00 00 00");
   }
}

