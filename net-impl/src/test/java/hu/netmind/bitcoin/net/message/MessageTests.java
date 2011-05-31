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
import java.util.Date;
import java.util.Calendar;
import java.net.InetSocketAddress;
import java.net.InetAddress;

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
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "76 65 72 61  63 6B 00 00 00 00 00 00 "+ // "verack" command
            "00 00 00 00"));                         // Payload is 0 bytes long
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
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "00 00 00 00");                          // Payload is 0 bytes long
   }

   public void testVersionDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                                                   // Main network magic bytes
          "76 65 72 73 69 6F 6E 00 00 00 00 00 "+                                           // "version" command
          "55 00 00 00 "+                                                                   // Payload is 85 bytes long
                                                                                            // No checksum in version message
          "9C 7C 00 00 "+                                                                   // 31900 (version 0.3.19)
          "01 00 00 00 00 00 00 00 "+                                                       // 1 (NODE_NETWORK services)
          "E6 15 10 4D 00 00 00 00 "+                                                       // Mon Dec 20 21:50:14 EST 2010
          "01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 01 DA F6 "+ // Sender address info - see Network Address
          "01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 02 20 8D "+ // Recipient address info - see Network Address
          "DD 9D 20 2C 3A B4 57 13 "+                                                       // Node random unique ID
          "00 "+                                                                            // "" sub-version string (string is 0 bytes long)
          "55 81 01 00 "));                                                                 // Last block sending node has is block #98645
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      Version message = (Version) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"version");
      Assert.assertEquals(message.getLength(),85);
      Assert.assertEquals(message.getVersion(),31900);
      Assert.assertEquals(message.getServices(),1);
      // Check timestamp
      Assert.assertEquals(message.getTimestamp(),1000l*0x4D1015e6l);
      // Check sender and recipient
      Assert.assertEquals(message.getSenderAddress().getServices(),1);
      Assert.assertEquals(message.getSenderAddress().getAddress().getPort(),56054);
      Assert.assertEquals(message.getSenderAddress().getAddress().getAddress().getHostAddress(),"10.0.0.1");
      Assert.assertEquals(message.getReceiverAddress().getServices(),1);
      Assert.assertEquals(message.getReceiverAddress().getAddress().getPort(),8333);
      Assert.assertEquals(message.getReceiverAddress().getAddress().getAddress().getHostAddress(),"10.0.0.2");
      // Other stuff
      Assert.assertEquals(message.getNonce(),0x1357B43A2C209DDDl);
      Assert.assertEquals(message.getSecondaryVersion(),"");
      Assert.assertEquals(message.getStartHeight(),98645);
   }

   public void testVersionSerialize()
      throws IOException
   {
      // Setup a verack message
      VersionImpl version = new VersionImpl(Message.MAGIC_MAIN,31900,1,1000l*0x4D1015e6l,
            new NodeAddressImpl(1,new InetSocketAddress(InetAddress.getByName("10.0.0.1"),56054)),
            new NodeAddressImpl(1,new InetSocketAddress(InetAddress.getByName("10.0.0.2"),8333)),
            0x1357B43A2C209DDDl,"",98645);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(version,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                                                   // Main network magic bytes
          "76 65 72 73 69 6F 6E 00 00 00 00 00 "+                                           // "version" command
          "55 00 00 00 "+                                                                   // Payload is 85 bytes long
                                                                                            // No checksum in version message
          "9C 7C 00 00 "+                                                                   // 31900 (version 0.3.19)
          "01 00 00 00 00 00 00 00 "+                                                       // 1 (NODE_NETWORK services)
          "E6 15 10 4D 00 00 00 00 "+                                                       // Mon Dec 20 21:50:14 EST 2010
          "01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 01 DA F6 "+ // Sender address info - see Network Address
          "01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 02 20 8D "+ // Recipient address info - see Network Address
          "DD 9D 20 2C 3A B4 57 13 "+                                                       // Node random unique ID
          "00 "+                                                                            // "" sub-version string (string is 0 bytes long)
          "55 81 01 00");                                                                   // Last block sending node has is block #98645
   }

}

