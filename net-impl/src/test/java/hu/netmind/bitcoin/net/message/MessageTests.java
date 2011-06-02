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

package hu.netmind.bitcoin.net.message;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hu.netmind.bitcoin.net.*;
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
      Addr message = (Addr) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"addr");
      Assert.assertEquals(message.getAddressEntries().size(),1);
      Assert.assertEquals(message.getChecksum(),0x9b3952edl);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Addr.AddressEntry entry = message.getAddressEntries().get(0);
      Assert.assertEquals(entry.getTimestamp(),1000*0x4d1015e2l);
      Assert.assertEquals(entry.getAddress().getServices(),1);
      Assert.assertEquals(entry.getAddress().getAddress().getPort(),8333);
      Assert.assertEquals(entry.getAddress().getAddress().getAddress().getHostAddress(),"10.0.0.1");
   }

   public void testAddrSerialize()
      throws IOException
   {
      // Setup a verack message
      Addr.AddressEntry entry = new AddrImpl.AddressEntryImpl(1000*0x4d1015e2l,
            new NodeAddressImpl(1,new InetSocketAddress(InetAddress.getByName("10.0.0.1"),8333)));
      List<Addr.AddressEntry> entries = new ArrayList<Addr.AddressEntry>();
      entries.add(entry);
      AddrImpl addr = new AddrImpl(Message.MAGIC_MAIN,entries);
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
      Inv message = (Inv) marshal.read(input);
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
      InventoryItemImpl item = new InventoryItemImpl(InventoryItem.TYPE_TX,
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      List<InventoryItem> items = new ArrayList<InventoryItem>();
      items.add(item);
      InvImpl inv = new InvImpl(Message.MAGIC_MAIN,items);
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
      GetData message = (GetData) marshal.read(input);
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
      InventoryItemImpl item = new InventoryItemImpl(InventoryItem.TYPE_TX,
            new byte[] { 0, 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,
                         18,19,20,21,22,23,24,25,26,27,28,29,30,31 });
      List<InventoryItem> items = new ArrayList<InventoryItem>();
      items.add(item);
      GetDataImpl getdata = new GetDataImpl(Message.MAGIC_MAIN,items);
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
      GetBlocks message = (GetBlocks) marshal.read(input);
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
      GetBlocksImpl getblocks = new GetBlocksImpl(Message.MAGIC_MAIN,hashStarts,hashStop);
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
      GetHeaders message = (GetHeaders) marshal.read(input);
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
      GetHeadersImpl getheaders = new GetHeadersImpl(Message.MAGIC_MAIN,hashStarts,hashStop);
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
      Verack message = (Verack) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Message.MAGIC_MAIN);
      Assert.assertEquals(message.getCommand(),"verack");
      Assert.assertEquals(message.getLength(),0);
   }
}

