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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, "MA  02111 "+//1307  USA
 */

package hu.netmind.bitcoin.node.p2p;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
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
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+ // "verack" command
            "00 00 00 00"));                         // Payload is 0 bytes long
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      VerackMessage message = (VerackMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"verack");
      Assert.assertEquals(message.getLength(),0);
   }

   public void testVerackSerialize()
      throws IOException
   {
      // Setup a verack message
      VerackMessage verack = new VerackMessage(Message.MAGIC_MAIN);
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
      VersionMessage message = (VersionMessage) marshal.read(input);
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
      VersionMessage version = new VersionMessage(Message.MAGIC_MAIN,31900,1,1000l*0x4D1015e6l,
            new NodeAddress(1,new InetSocketAddress(InetAddress.getByName("10.0.0.1"),56054)),
            new NodeAddress(1,new InetSocketAddress(InetAddress.getByName("10.0.0.2"),8333)),
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

   public void testAddrDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "61 64 64 72 00 00 00 00 00 00 00 00 "+             // "addr"
          "1F 00 00 00 "+                                     // payload is 31 bytes long
          "ED 52 39 9B "+                                     // checksum of payload
          "01 "+                                              // 1 address in this message
          "E2 15 10 4D "+                                     // Mon Dec 20 21:50:10 EST 2010 (only when version is >= 31402)
          "01 00 00 00 00 00 00 00 "+                         // 1 (NODE_NETWORK service - see version message)
          "00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 01 "+ // IPv4: 10.0.0.1, IPv6: ::ffff:10.0.0.1 (IPv4-mapped IPv6 address)
          "20 8D"));                                          // port 8333
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      marshal.setVersion(39010);
      AddrMessage message = (AddrMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"addr");
      Assert.assertEquals(message.getAddressEntries().size(),1);
      Assert.assertEquals(message.getChecksum(),0x9b3952edl);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      AddrMessage.AddressEntry entry = message.getAddressEntries().get(0);
      Assert.assertEquals(entry.getTimestamp(),1000*0x4d1015e2l);
      Assert.assertEquals(entry.getAddress().getServices(),1);
      Assert.assertEquals(entry.getAddress().getAddress().getPort(),8333);
      Assert.assertEquals(entry.getAddress().getAddress().getAddress().getHostAddress(),"10.0.0.1");
   }

   public void testAddrSerialize()
      throws IOException
   {
      // Setup a verack message
      AddrMessage.AddressEntry entry = new AddrMessage.AddressEntry(1000*0x4d1015e2l,
            new NodeAddress(1,new InetSocketAddress(InetAddress.getByName("10.0.0.1"),8333)));
      List<AddrMessage.AddressEntry> entries = new ArrayList<AddrMessage.AddressEntry>();
      entries.add(entry);
      AddrMessage addr = new AddrMessage(Message.MAGIC_MAIN,entries);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      marshal.setVersion(39010);
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(addr,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "61 64 64 72 00 00 00 00 00 00 00 00 "+             // "addr"
          "1F 00 00 00 "+                                     // payload is 31 bytes long
          "ED 52 39 9B "+                                     // checksum of payload
          "01 "+                                              // 1 address in this message
          "E2 15 10 4D "+                                     // Mon Dec 20 21:50:10 EST 2010 (only when version is >= 31402)
          "01 00 00 00 00 00 00 00 "+                         // 1 (NODE_NETWORK service - see version message)
          "00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 01 "+ // IPv4: 10.0.0.1, IPv6: ::ffff:10.0.0.1 (IPv4-mapped IPv6 address)
          "20 8D");                                           // port 8333
   }

   public void testInvDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "69 6E 76 00 00 00 00 00 00 00 00 00 "+             // "inv"
          "25 00 00 00 "+                                     // payload is 36 bytes long
          "41 01 8A 30 "+                                     // checksum
          "01 "+                                              // number of items
          "01 00 00 00 "+                                     // type 1 (tx)
          "00 01 02 03 04 05 06 07 08 09 0A "+                // hash of tx
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F"));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      InvMessage message = (InvMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"inv");
      Assert.assertEquals(message.getInventoryItems().size(),1);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      InventoryItem item = message.getInventoryItems().get(0);
      Assert.assertEquals(item.getType(),1);
      Assert.assertEquals(item.getHash(),new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
   }

   public void testInvSerialize()
      throws IOException
   {
      // Setup an inv message
      InventoryItem item = new InventoryItem(InventoryItem.TYPE_TX,
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      List<InventoryItem> items = new ArrayList<InventoryItem>();
      items.add(item);
      InvMessage inv = new InvMessage(Message.MAGIC_MAIN,items);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(inv,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "69 6E 76 00 00 00 00 00 00 00 00 00 "+             // "inv"
          "25 00 00 00 "+                                     // payload is 36 bytes long
          "41 01 8A 30 "+                                     // checksum
          "01 "+                                              // number of items
          "01 00 00 00 "+                                     // type 1 (tx)
          "00 01 02 03 04 05 06 07 08 09 0A "+                // hash of tx
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F");
   }

   public void testGetDataDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 64 61 74 61 00 00 00 00 00 "+             // "getdata"
          "25 00 00 00 "+                                     // payload is 36 bytes long
          "41 01 8A 30 "+                                     // checksum
          "01 "+                                              // number of items
          "01 00 00 00 "+                                     // type 1 (tx)
          "00 01 02 03 04 05 06 07 08 09 0A "+                // hash of tx
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F"));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      GetDataMessage message = (GetDataMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"getdata");
      Assert.assertEquals(message.getInventoryItems().size(),1);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      InventoryItem item = message.getInventoryItems().get(0);
      Assert.assertEquals(item.getType(),1);
      Assert.assertEquals(item.getHash(),new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
   }

   public void testGetDataSerialize()
      throws IOException
   {
      // Setup message
      InventoryItem item = new InventoryItem(InventoryItem.TYPE_TX,
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      List<InventoryItem> items = new ArrayList<InventoryItem>();
      items.add(item);
      GetDataMessage getdata = new GetDataMessage(Message.MAGIC_MAIN,items);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(getdata,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 64 61 74 61 00 00 00 00 00 "+             // "getdata"
          "25 00 00 00 "+                                     // payload is 36 bytes long
          "41 01 8A 30 "+                                     // checksum
          "01 "+                                              // number of items
          "01 00 00 00 "+                                     // type 1 (tx)
          "00 01 02 03 04 05 06 07 08 09 0A "+                // hash of tx
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F");
   }

   public void testGetBlocksDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 62 6C 6F 63 6B 73 00 00 00 "+             // "getblocks"
          "41 00 00 00 "+                                     // payload 65 bytes long
          "56 4A 69 A5 "+                                     // checksum
          "01 "+                                              // 1 start hash
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A "+                // end hash
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F"));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      GetBlocksMessage message = (GetBlocksMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"getblocks");
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getHashStarts().size(),1);
      Assert.assertEquals(message.getHashStarts().get(0),new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      Assert.assertEquals(message.getHashStop(),new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
   }

   public void testGetBlocksSerialize()
      throws IOException
   {
      // Setup message
      List<byte[]> hashStarts = new ArrayList<byte[]>();
      hashStarts.add(
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      byte[] hashStop = 
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 };
      GetBlocksMessage getblocks = new GetBlocksMessage(Message.MAGIC_MAIN,hashStarts,hashStop);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(getblocks,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 62 6C 6F 63 6B 73 00 00 00 "+             // "getblocks"
          "41 00 00 00 "+                                     // payload 65 bytes long
          "56 4A 69 A5 "+                                     // checksum
          "01 "+                                              // 1 start hash
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A "+                // end hash
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F");
   }

   public void testGetHeadersDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 68 65 61 64 65 72 73 00 00 "+             // "getheaders"
          "41 00 00 00 "+                                     // payload 65 bytes long
          "56 4A 69 A5 "+                                     // checksum
          "01 "+                                              // 1 start hash
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A "+                // end hash
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F"));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      GetHeadersMessage message = (GetHeadersMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"getheaders");
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getHashStarts().size(),1);
      Assert.assertEquals(message.getHashStarts().get(0),new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      Assert.assertEquals(message.getHashStop(),new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
   }

   public void testGetHeadersSerialize()
      throws IOException
   {
      // Setup message
      List<byte[]> hashStarts = new ArrayList<byte[]>();
      hashStarts.add(
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      byte[] hashStop = 
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 };
      GetHeadersMessage getheaders = new GetHeadersMessage(Message.MAGIC_MAIN,hashStarts,hashStop);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(getheaders,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 68 65 61 64 65 72 73 00 00 "+             // "getheaders"
          "41 00 00 00 "+                                     // payload 65 bytes long
          "56 4A 69 A5 "+                                     // checksum
          "01 "+                                              // 1 start hash
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A "+                // end hash
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F");
   }

   public void testReadUnrecognizedCommand()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network
            "61 61 61 00 00 00 00 00 00 00 00 00 "+  // "aaa" command (unrecognized)
            "10 00 00 00 "+                          // payload is 16 bytes
            "12 34 56 78 "+                          // checksum
            "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+ // junk payload
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "00 00 00 00"));                         // Payload is 0 bytes long
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      VerackMessage message = (VerackMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"verack");
      Assert.assertEquals(message.getLength(),0);
   }

   public void testTxDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                       // main network magic bytes
          "74 78 00 00 00 00 00 00 00 00 00 00 "+               // "tx" command
          "02 01 00 00 "+                                       // payload is 258 bytes long
          "E2 93 CD BE "+                                       // checksum of payload
          "01 00 00 00 "+                                       // version
          "01 "+                                                // number of transaction inputs
          "6D BD DB 08 5B 1D 8A F7 51 84 F0 BC 01 FA D5 8D "+   // previous output (outpoint)
          "12 66 E9 B6 3B 50 88 19 90 E4 B4 0D 6A EE 36 29 "+
          "00 00 00 00 "+
          "8B "+                                                // script is 139 bytes long
          "48 30 45 02 21 00 F3 58 1E 19 72 AE 8A C7 C7 36 "+   // signature script (scriptSig)
          "7A 7A 25 3B C1 13 52 23 AD B9 A4 68 BB 3A 59 23 "+
          "3F 45 BC 57 83 80 02 20 59 AF 01 CA 17 D0 0E 41 "+
          "83 7A 1D 58 E9 7A A3 1B AE 58 4E DE C2 8D 35 BD "+
          "96 92 36 90 91 3B AE 9A 01 41 04 9C 02 BF C9 7E "+
          "F2 36 CE 6D 8F E5 D9 40 13 C7 21 E9 15 98 2A CD "+
          "2B 12 B6 5D 9B 7D 59 E2 0A 84 20 05 F8 FC 4E 02 "+
          "53 2E 87 3D 37 B9 6F 09 D6 D4 51 1A DA 8F 14 04 "+
          "2F 46 61 4A 4C 70 C0 F1 4B EF F5 "+
          "FF FF FF FF "+                                       // sequence
          "02 "+                                                // 2 Output Transactions
          "40 4B 4C 00 00 00 00 00 "+                           // 0.05 BTC (5000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+   // pk_script
          "A9 D9 EA 1A FB 22 5E 88 AC "+
          "80 FA E9 C7 00 00 00 00 "+                           // 33.54 BTC (3354000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 0E AB 5B EA 43 6A 04 84 CF AB 12 48 5E "+   // pk_script
          "FD A0 B7 8B 4E CC 52 88 AC "+
          "00 00 00 00 "));                                     // lock time
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      TxMessage message = (TxMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"tx");
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getTransaction().getVersion(),1);
      Assert.assertEquals(message.getTransaction().getLockTime(),0);
      Assert.assertEquals(message.getTransaction().getInputs().size(),1);
      Assert.assertEquals(message.getTransaction().getInputs().get(0).getReferencedTxOutIndex(),0);
      Assert.assertEquals(message.getTransaction().getInputs().get(0).getSequence(),0xffffffffl);
      Assert.assertEquals(message.getTransaction().getInputs().get(0).getSignatureScript().length,139);
      Assert.assertEquals(message.getTransaction().getOutputs().size(),2);
      Assert.assertEquals(message.getTransaction().getOutputs().get(0).getValue(),5000000l);
      Assert.assertEquals(message.getTransaction().getOutputs().get(1).getValue(),3354000000l);
      Assert.assertEquals(message.getTransaction().getOutputs().get(0).getScript().length,25);
      Assert.assertEquals(message.getTransaction().getOutputs().get(1).getScript().length,25);
   }

   public void testTxSerialize()
      throws IOException
   {
      // Setup message
      TxIn in1 = new TxIn(
            HexUtil.toByteArray(
                "6D BD DB 08 5B 1D 8A F7 51 84 F0 BC 01 FA D5 8D "+   // previous output (outpoint)
                "12 66 E9 B6 3B 50 88 19 90 E4 B4 0D 6A EE 36 29 "),0,
            HexUtil.toByteArray(
                "48 30 45 02 21 00 F3 58 1E 19 72 AE 8A C7 C7 36 "+   // signature script (scriptSig)
                "7A 7A 25 3B C1 13 52 23 AD B9 A4 68 BB 3A 59 23 "+
                "3F 45 BC 57 83 80 02 20 59 AF 01 CA 17 D0 0E 41 "+
                "83 7A 1D 58 E9 7A A3 1B AE 58 4E DE C2 8D 35 BD "+
                "96 92 36 90 91 3B AE 9A 01 41 04 9C 02 BF C9 7E "+
                "F2 36 CE 6D 8F E5 D9 40 13 C7 21 E9 15 98 2A CD "+
                "2B 12 B6 5D 9B 7D 59 E2 0A 84 20 05 F8 FC 4E 02 "+
                "53 2E 87 3D 37 B9 6F 09 D6 D4 51 1A DA 8F 14 04 "+
                "2F 46 61 4A 4C 70 C0 F1 4B EF F5 "), 0xffffffffl);
      TxOut out1 = new TxOut(5000000l,
            HexUtil.toByteArray(
                "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+   // pk_script
                "A9 D9 EA 1A FB 22 5E 88 AC "));
      TxOut out2 = new TxOut(3354000000l,
            HexUtil.toByteArray(
                "76 A9 14 0E AB 5B EA 43 6A 04 84 CF AB 12 48 5E "+   // pk_script
                "FD A0 B7 8B 4E CC 52 88 AC "));
      List<TxIn> inputs = new ArrayList<TxIn>();
      inputs.add(in1);
      List<TxOut> outputs = new ArrayList<TxOut>();
      outputs.add(out1);
      outputs.add(out2);
      TxMessage tx = new TxMessage(Message.MAGIC_MAIN,new Transaction(1,inputs,outputs,0));
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(tx,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                       // main network magic bytes
          "74 78 00 00 00 00 00 00 00 00 00 00 "+               // "tx" command
          "02 01 00 00 "+                                       // payload is 258 bytes long
          "E2 93 CD BE "+                                       // checksum of payload
          "01 00 00 00 "+                                       // version
          "01 "+                                                // number of transaction inputs
          "6D BD DB 08 5B 1D 8A F7 51 84 F0 BC 01 FA D5 8D "+   // previous output (outpoint)
          "12 66 E9 B6 3B 50 88 19 90 E4 B4 0D 6A EE 36 29 "+
          "00 00 00 00 "+
          "8B "+                                                // script is 139 bytes long
          "48 30 45 02 21 00 F3 58 1E 19 72 AE 8A C7 C7 36 "+   // signature script (scriptSig)
          "7A 7A 25 3B C1 13 52 23 AD B9 A4 68 BB 3A 59 23 "+
          "3F 45 BC 57 83 80 02 20 59 AF 01 CA 17 D0 0E 41 "+
          "83 7A 1D 58 E9 7A A3 1B AE 58 4E DE C2 8D 35 BD "+
          "96 92 36 90 91 3B AE 9A 01 41 04 9C 02 BF C9 7E "+
          "F2 36 CE 6D 8F E5 D9 40 13 C7 21 E9 15 98 2A CD "+
          "2B 12 B6 5D 9B 7D 59 E2 0A 84 20 05 F8 FC 4E 02 "+
          "53 2E 87 3D 37 B9 6F 09 D6 D4 51 1A DA 8F 14 04 "+
          "2F 46 61 4A 4C 70 C0 F1 4B EF F5 "+
          "FF FF FF FF "+                                       // sequence
          "02 "+                                                // 2 Output Transactions
          "40 4B 4C 00 00 00 00 00 "+                           // 0.05 BTC (5000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+   // pk_script
          "A9 D9 EA 1A FB 22 5E 88 AC "+
          "80 FA E9 C7 00 00 00 00 "+                           // 33.54 BTC (3354000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 0E AB 5B EA 43 6A 04 84 CF AB 12 48 5E "+   // pk_script
          "FD A0 B7 8B 4E CC 52 88 AC "+
          "00 00 00 00");                                       // lock time
   }

   public void testBlockSerialize()
      throws IOException
   {
      // Setup message
      TxIn in1 = new TxIn(
            HexUtil.toByteArray(
                "6D BD DB 08 5B 1D 8A F7 51 84 F0 BC 01 FA D5 8D "+   // previous output (outpoint)
                "12 66 E9 B6 3B 50 88 19 90 E4 B4 0D 6A EE 36 29 "),0,
            HexUtil.toByteArray(
                "48 30 45 02 21 00 F3 58 1E 19 72 AE 8A C7 C7 36 "+   // signature script (scriptSig)
                "7A 7A 25 3B C1 13 52 23 AD B9 A4 68 BB 3A 59 23 "+
                "3F 45 BC 57 83 80 02 20 59 AF 01 CA 17 D0 0E 41 "+
                "83 7A 1D 58 E9 7A A3 1B AE 58 4E DE C2 8D 35 BD "+
                "96 92 36 90 91 3B AE 9A 01 41 04 9C 02 BF C9 7E "+
                "F2 36 CE 6D 8F E5 D9 40 13 C7 21 E9 15 98 2A CD "+
                "2B 12 B6 5D 9B 7D 59 E2 0A 84 20 05 F8 FC 4E 02 "+
                "53 2E 87 3D 37 B9 6F 09 D6 D4 51 1A DA 8F 14 04 "+
                "2F 46 61 4A 4C 70 C0 F1 4B EF F5 "), 0xffffffffl);
      TxOut out1 = new TxOut(5000000l,
            HexUtil.toByteArray(
                "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+   // pk_script
                "A9 D9 EA 1A FB 22 5E 88 AC "));
      TxOut out2 = new TxOut(3354000000l,
            HexUtil.toByteArray(
                "76 A9 14 0E AB 5B EA 43 6A 04 84 CF AB 12 48 5E "+   // pk_script
                "FD A0 B7 8B 4E CC 52 88 AC "));
      List<TxIn> inputs = new ArrayList<TxIn>();
      inputs.add(in1);
      List<TxOut> outputs = new ArrayList<TxOut>();
      outputs.add(out1);
      outputs.add(out2);
      Transaction tx = new Transaction(1,inputs,outputs,0);
      List<Transaction> transactions = new ArrayList<Transaction>();
      transactions.add(tx);
      BlockMessage block = new BlockMessage(Message.MAGIC_MAIN,
            new BlockHeader(1,
            HexUtil.toByteArray(
               "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+
               "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "),
            HexUtil.toByteArray(
               "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+
               "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "),
            123000,22,33),
            transactions);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(block,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                       // main network magic bytes
          "62 6C 6F 63 6B 00 00 00 00 00 00 00 "+               // block command
          "53 01 00 00 "+                                       // payload is 307 bytes long
          "C9 B3 C1 8B "+                                       // checksum of payload
          "01 00 00 00 "+                                       // version format of block payload
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // previous block hash
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // root hash of tx
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "7B 00 00 00 "+                                       // timestamp
          "16 00 00 00 "+                                       // difficulty
          "21 00 00 00 "+                                       // nonce
          "01 "+                                                // number of transactions that follow
          "01 00 00 00 "+                                       // version
          "01 "+                                                // number of transaction inputs
          "6D BD DB 08 5B 1D 8A F7 51 84 F0 BC 01 FA D5 8D "+   // previous output (outpoint)
          "12 66 E9 B6 3B 50 88 19 90 E4 B4 0D 6A EE 36 29 "+
          "00 00 00 00 "+
          "8B "+                                                // script is 139 bytes long
          "48 30 45 02 21 00 F3 58 1E 19 72 AE 8A C7 C7 36 "+   // signature script (scriptSig)
          "7A 7A 25 3B C1 13 52 23 AD B9 A4 68 BB 3A 59 23 "+
          "3F 45 BC 57 83 80 02 20 59 AF 01 CA 17 D0 0E 41 "+
          "83 7A 1D 58 E9 7A A3 1B AE 58 4E DE C2 8D 35 BD "+
          "96 92 36 90 91 3B AE 9A 01 41 04 9C 02 BF C9 7E "+
          "F2 36 CE 6D 8F E5 D9 40 13 C7 21 E9 15 98 2A CD "+
          "2B 12 B6 5D 9B 7D 59 E2 0A 84 20 05 F8 FC 4E 02 "+
          "53 2E 87 3D 37 B9 6F 09 D6 D4 51 1A DA 8F 14 04 "+
          "2F 46 61 4A 4C 70 C0 F1 4B EF F5 "+
          "FF FF FF FF "+                                       // sequence
          "02 "+                                                // 2 Output Transactions
          "40 4B 4C 00 00 00 00 00 "+                           // 0.05 BTC (5000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+   // pk_script
          "A9 D9 EA 1A FB 22 5E 88 AC "+
          "80 FA E9 C7 00 00 00 00 "+                           // 33.54 BTC (3354000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 0E AB 5B EA 43 6A 04 84 CF AB 12 48 5E "+   // pk_script
          "FD A0 B7 8B 4E CC 52 88 AC "+
          "00 00 00 00");                                       // lock time
   }

   public void testBlockDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                       // main network magic bytes
          "62 6C 6F 63 6B 00 00 00 00 00 00 00 "+               // block command
          "53 01 00 00 "+                                       // payload is 307 bytes long
          "C9 B3 C1 8B "+                                       // checksum of payload
          "01 00 00 00 "+                                       // version format of block payload
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // previous block hash
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // root hash of tx
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "7B 00 00 00 "+                                       // timestamp
          "16 00 00 00 "+                                       // difficulty
          "21 00 00 00 "+                                       // nonce
          "01 "+                                                // number of transactions that follow
          "01 00 00 00 "+                                       // version
          "01 "+                                                // number of transaction inputs
          "6D BD DB 08 5B 1D 8A F7 51 84 F0 BC 01 FA D5 8D "+   // previous output (outpoint)
          "12 66 E9 B6 3B 50 88 19 90 E4 B4 0D 6A EE 36 29 "+
          "00 00 00 00 "+
          "8B "+                                                // script is 139 bytes long
          "48 30 45 02 21 00 F3 58 1E 19 72 AE 8A C7 C7 36 "+   // signature script (scriptSig)
          "7A 7A 25 3B C1 13 52 23 AD B9 A4 68 BB 3A 59 23 "+
          "3F 45 BC 57 83 80 02 20 59 AF 01 CA 17 D0 0E 41 "+
          "83 7A 1D 58 E9 7A A3 1B AE 58 4E DE C2 8D 35 BD "+
          "96 92 36 90 91 3B AE 9A 01 41 04 9C 02 BF C9 7E "+
          "F2 36 CE 6D 8F E5 D9 40 13 C7 21 E9 15 98 2A CD "+
          "2B 12 B6 5D 9B 7D 59 E2 0A 84 20 05 F8 FC 4E 02 "+
          "53 2E 87 3D 37 B9 6F 09 D6 D4 51 1A DA 8F 14 04 "+
          "2F 46 61 4A 4C 70 C0 F1 4B EF F5 "+
          "FF FF FF FF "+                                       // sequence
          "02 "+                                                // 2 Output Transactions
          "40 4B 4C 00 00 00 00 00 "+                           // 0.05 BTC (5000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+   // pk_script
          "A9 D9 EA 1A FB 22 5E 88 AC "+
          "80 FA E9 C7 00 00 00 00 "+                           // 33.54 BTC (3354000000)
          "19 "+                                                // pk_script is 25 bytes long
          "76 A9 14 0E AB 5B EA 43 6A 04 84 CF AB 12 48 5E "+   // pk_script
          "FD A0 B7 8B 4E CC 52 88 AC "+
          "00 00 00 00"));                                      // lock time
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      BlockMessage message = (BlockMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"block");
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getHeader().getVersion(),1);
      Assert.assertEquals(message.getHeader().getTimestamp(),123000);
      Assert.assertEquals(message.getHeader().getDifficulty(),22);
      Assert.assertEquals(message.getHeader().getNonce(),33);
      Assert.assertEquals(message.getTransactions().get(0).getVersion(),1);
      Assert.assertEquals(message.getTransactions().get(0).getLockTime(),0);
      Assert.assertEquals(message.getTransactions().get(0).getInputs().size(),1);
      Assert.assertEquals(message.getTransactions().get(0).getInputs().get(0).getReferencedTxOutIndex(),0);
      Assert.assertEquals(message.getTransactions().get(0).getInputs().get(0).getSequence(),0xffffffffl);
      Assert.assertEquals(message.getTransactions().get(0).getInputs().get(0).getSignatureScript().length,139);
      Assert.assertEquals(message.getTransactions().get(0).getOutputs().size(),2);
      Assert.assertEquals(message.getTransactions().get(0).getOutputs().get(0).getValue(),5000000l);
      Assert.assertEquals(message.getTransactions().get(0).getOutputs().get(1).getValue(),3354000000l);
      Assert.assertEquals(message.getTransactions().get(0).getOutputs().get(0).getScript().length,25);
      Assert.assertEquals(message.getTransactions().get(0).getOutputs().get(1).getScript().length,25);
   }

   public void testHeadersSerialize()
      throws IOException
   {
      // Setup message
      BlockHeader header = new BlockHeader(1,
            HexUtil.toByteArray(
               "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+
               "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "),
            HexUtil.toByteArray(
               "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+
               "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "),
            123000,22,33);
      List<BlockHeader> headers = new ArrayList<BlockHeader>();
      headers.add(header);
      HeadersMessage headersMessage = new HeadersMessage(Message.MAGIC_MAIN,headers);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(headersMessage,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
          "F9 BE B4 D9 "+                                       // main network magic bytes
          "68 65 61 64 65 72 73 00 00 00 00 00 "+               // "headers" command
          "51 00 00 00 "+                                       // payload is 81 bytes long
          "9A F3 1C 8F "+                                       // checksum of payload
          "01 "+                                                // 1 header only in this message
          "01 00 00 00 "+                                       // version format of block payload
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // previous block hash
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // root hash of tx
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "7B 00 00 00 "+                                       // timestamp
          "16 00 00 00 "+                                       // difficulty
          "21 00 00 00");                                       // nonce
   }

   public void testHeadersDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                       // main network magic bytes
          "68 65 61 64 65 72 73 00 00 00 00 00 "+               // "headers" command
          "51 00 00 00 "+                                       // payload is 81 bytes long
          "9A F3 1C 8F "+                                       // checksum of payload
          "01 "+                                                // 1 header only in this message
          "01 00 00 00 "+                                       // version format of block payload
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // previous block hash
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+   // root hash of tx
          "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "7B 00 00 00 "+                                       // timestamp
          "16 00 00 00 "+                                       // difficulty
          "21 00 00 00"));                                      // nonce
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      HeadersMessage message = (HeadersMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"headers");
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getHeaders().get(0).getVersion(),1);
      Assert.assertEquals(message.getHeaders().get(0).getTimestamp(),123000);
      Assert.assertEquals(message.getHeaders().get(0).getDifficulty(),22);
      Assert.assertEquals(message.getHeaders().get(0).getNonce(),33);
   }

   public void testGetAddrDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "67 65 74 61 64 64 72 00 00 00 00 00 "+  // "getaddr" command
            "00 00 00 00 "+                          // payload is 0 bytes
            "5D F6 E0 E2"));                         // checksum is null
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      GetAddrMessage message = (GetAddrMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"getaddr");
      Assert.assertEquals(message.getLength(),0);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
   }

   public void testGetAddrSerialize()
      throws IOException
   {
      // Setup a verack message
      GetAddrMessage getaddr = new GetAddrMessage(Message.MAGIC_MAIN);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(getaddr,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "67 65 74 61 64 64 72 00 00 00 00 00 "+  // "getaddr" command
            "00 00 00 00 "+                          // Payload is 0 bytes long
            "5D F6 E0 E2");                          // no checksum
   }

   public void testPingDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      ByteArrayBitCoinInputStream input = new ByteArrayBitCoinInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "70 69 6E 67 00 00 00 00 00 00 00 00 "+  // "ping" command
            "00 00 00 00 "+                          // payload is 0 bytes
            "5D F6 E0 E2"));                         // checksum is null
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller();
      PingMessage message = (PingMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"ping");
      Assert.assertEquals(message.getLength(),0);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
   }

   public void testPingSerialize()
      throws IOException
   {
      // Setup a verack message
      PingMessage ping = new PingMessage(Message.MAGIC_MAIN);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller();
      ByteArrayBitCoinOutputStream output = new ByteArrayBitCoinOutputStream();
      marshal.write(ping,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(output.toByteArray()),
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "70 69 6E 67 00 00 00 00 00 00 00 00 "+  // "ping" command
            "00 00 00 00 "+                          // Payload is 0 bytes long
            "5D F6 E0 E2");                          // no checksum
   }

}

