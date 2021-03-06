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

package it.nibbles.javacoin.net;

import it.nibbles.javacoin.net.VerackMessage;
import it.nibbles.javacoin.net.BlockMessage;
import it.nibbles.javacoin.net.BlockHeader;
import it.nibbles.javacoin.net.AlertMessage;
import it.nibbles.javacoin.net.GetBlocksMessage;
import it.nibbles.javacoin.net.BitcoinInputStream;
import it.nibbles.javacoin.net.TxIn;
import it.nibbles.javacoin.net.GetDataMessage;
import it.nibbles.javacoin.net.BitcoinOutputStream;
import it.nibbles.javacoin.net.GetHeadersMessage;
import it.nibbles.javacoin.net.GetAddrMessage;
import it.nibbles.javacoin.net.PingMessage;
import it.nibbles.javacoin.net.InvMessage;
import it.nibbles.javacoin.net.Tx;
import it.nibbles.javacoin.net.InventoryItem;
import it.nibbles.javacoin.net.HeadersMessage;
import it.nibbles.javacoin.net.MessageMarshaller;
import it.nibbles.javacoin.net.VersionMessage;
import it.nibbles.javacoin.net.HexUtil;
import it.nibbles.javacoin.net.TxOut;
import it.nibbles.javacoin.net.AddrMessage;
import it.nibbles.javacoin.net.NodeAddress;
import it.nibbles.javacoin.net.TxMessage;
import it.nibbles.javacoin.Constants;
import it.nibbles.javacoin.utils.BtcUtil;
import it.nibbles.javacoin.keyfactory.ecc.BitcoinUtil;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "00 00 00 00 "+                          // Payload is 0 bytes long
            "5D F6 E0 E2")));                        // Checksum (mandatory from Feb 20, 2012)
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      VerackMessage message = (VerackMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"verack");
      Assert.assertEquals(message.getLength(),0);
   }

   public void testVerackSerialize()
      throws IOException
   {
      // Setup a verack message
      VerackMessage verack = new VerackMessage(Constants.PRODNET_MESSAGE_MAGIC);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(verack,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "00 00 00 00 "+                          // Payload is 0 bytes long
            "5D F6 E0 E2");                          // Checksum (mandatory from Feb 20, 2012)
   }

   public void testVersionDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                                                   // Main network magic bytes
          "76 65 72 73 69 6F 6E 00 00 00 00 00 "+                                           // "version" command
          "55 00 00 00 "+                                                                   // Payload is 85 bytes long
          "D2 95 6D 8A "+                                                                   // checksum of payload
          "9C 7C 00 00 "+                                                                   // 31900 (version 0.3.19)
          "01 00 00 00 00 00 00 00 "+                                                       // 1 (NODE_NETWORK services)
          "E6 15 10 4D 00 00 00 00 "+                                                       // Mon Dec 20 21:50:14 EST 2010
          "01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 01 DA F6 "+ // Sender address info - see Network Address
          "01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 02 20 8D "+ // Recipient address info - see Network Address
          "DD 9D 20 2C 3A B4 57 13 "+                                                       // Node random unique ID
          "00 "+                                                                            // "" sub-version string (string is 0 bytes long)
          "55 81 01 00 ")));                                                                // Last block sending node has is block #98645
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      VersionMessage message = (VersionMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"version");
      Assert.assertEquals(message.getLength(),85);
      Assert.assertEquals(message.getVersion(),31900);
      Assert.assertEquals(message.getServices(),1);
      // Check timestamp
      Assert.assertEquals(message.getTimestamp(),1000l*0x4D1015e6l);
      // Check sender and recipient
      Assert.assertEquals(message.getReceiverAddress().getServices(),1);
      Assert.assertEquals(message.getReceiverAddress().getAddress().getPort(),56054);
      Assert.assertEquals(message.getReceiverAddress().getAddress().getAddress().getHostAddress(),"10.0.0.1");
      Assert.assertEquals(message.getSenderAddress().getServices(),1);
      Assert.assertEquals(message.getSenderAddress().getAddress().getPort(),8333);
      Assert.assertEquals(message.getSenderAddress().getAddress().getAddress().getHostAddress(),"10.0.0.2");
      // Other stuff
      Assert.assertEquals(message.getNonce(),0x1357B43A2C209DDDl);
      Assert.assertEquals(message.getSecondaryVersion(),"");
      Assert.assertEquals(message.getStartHeight(),98645);
   }

   public void testVersionSerialize()
      throws IOException
   {
      // Setup a verack message
      VersionMessage version = new VersionMessage(Constants.PRODNET_MESSAGE_MAGIC,31900,1,1000l*0x4D1015e6l,
            new NodeAddress(1,new InetSocketAddress(InetAddress.getByName("10.0.0.1"),56054)),
            new NodeAddress(1,new InetSocketAddress(InetAddress.getByName("10.0.0.2"),8333)),
            0x1357B43A2C209DDDl,"",98645);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(version,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
          "F9 BE B4 D9 "+                                                                   // Main network magic bytes
          "76 65 72 73 69 6F 6E 00 00 00 00 00 "+                                           // "version" command
          "55 00 00 00 "+                                                                   // Payload is 85 bytes long
          "D2 95 6D 8A "+                                                                   // checksum of payload
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "61 64 64 72 00 00 00 00 00 00 00 00 "+             // "addr"
          "1F 00 00 00 "+                                     // payload is 31 bytes long
          "ED 52 39 9B "+                                     // checksum of payload
          "01 "+                                              // 1 address in this message
          "E2 15 10 4D "+                                     // Mon Dec 20 21:50:10 EST 2010 (only when version is >= 31402)
          "01 00 00 00 00 00 00 00 "+                         // 1 (NODE_NETWORK service - see version message)
          "00 00 00 00 00 00 00 00 00 00 FF FF 0A 00 00 01 "+ // IPv4: 10.0.0.1, IPv6: ::ffff:10.0.0.1 (IPv4-mapped IPv6 address)
          "20 8D")));                                         // port 8333
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      marshal.setVersion(39010);
      AddrMessage message = (AddrMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
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
      AddrMessage addr = new AddrMessage(Constants.PRODNET_MESSAGE_MAGIC,entries);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      marshal.setVersion(39010);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(addr,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "69 6E 76 00 00 00 00 00 00 00 00 00 "+             // "inv"
          "25 00 00 00 "+                                     // payload is 36 bytes long
          "41 01 8A 30 "+                                     // checksum
          "01 "+                                              // number of items
          "01 00 00 00 "+                                     // type 1 (tx)
          "00 01 02 03 04 05 06 07 08 09 0A "+                // hash of tx
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F")));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      InvMessage message = (InvMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"inv");
      Assert.assertEquals(message.getInventoryItems().size(),1);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      InventoryItem item = message.getInventoryItems().get(0);
      Assert.assertEquals(item.getType(),1);
      Assert.assertEquals(item.getHash(),new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
   }

   public void testInvSerialize()
      throws IOException
   {
      // Setup an inv message
      InventoryItem item = new InventoryItem(InventoryItem.TYPE_TX,
            new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
      List<InventoryItem> items = new ArrayList<InventoryItem>();
      items.add(item);
      InvMessage inv = new InvMessage(Constants.PRODNET_MESSAGE_MAGIC,items);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(inv,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 64 61 74 61 00 00 00 00 00 "+             // "getdata"
          "25 00 00 00 "+                                     // payload is 36 bytes long
          "41 01 8A 30 "+                                     // checksum
          "01 "+                                              // number of items
          "01 00 00 00 "+                                     // type 1 (tx)
          "00 01 02 03 04 05 06 07 08 09 0A "+                // hash of tx
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F")));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      GetDataMessage message = (GetDataMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"getdata");
      Assert.assertEquals(message.getInventoryItems().size(),1);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      InventoryItem item = message.getInventoryItems().get(0);
      Assert.assertEquals(item.getType(),1);
      Assert.assertEquals(item.getHash(),new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
   }

   public void testGetDataSerialize()
      throws IOException
   {
      // Setup message
      InventoryItem item = new InventoryItem(InventoryItem.TYPE_TX,
            new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
      List<InventoryItem> items = new ArrayList<InventoryItem>();
      items.add(item);
      GetDataMessage getdata = new GetDataMessage(Constants.PRODNET_MESSAGE_MAGIC,items);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(getdata,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 62 6C 6F 63 6B 73 00 00 00 "+             // "getblocks"
          "45 00 00 00 "+                                     // payload length
          "B7 B6 9A 31 "+                                     // checksum
          "01 02 03 04 "+                                     // message version
          "01 "+                                              // 1 start hash
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A "+                // end hash
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F")));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      GetBlocksMessage message = (GetBlocksMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"getblocks");
      Assert.assertEquals(message.getMessageVersion(),0x04030201l);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getHashStarts().size(),1);
      Assert.assertEquals(message.getHashStarts().get(0),new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
      Assert.assertEquals(message.getHashStop(),new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
   }

   public void testGetBlocksSerialize()
      throws IOException
   {
      // Setup message
      List<byte[]> hashStarts = new ArrayList<byte[]>();
      hashStarts.add(
            new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
      byte[] hashStop = 
            new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 };
      GetBlocksMessage getblocks = new GetBlocksMessage(Constants.PRODNET_MESSAGE_MAGIC,0x04030201l,hashStarts,hashStop);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(getblocks,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 62 6C 6F 63 6B 73 00 00 00 "+             // "getblocks"
          "45 00 00 00 "+                                     // payload length
          "B7 B6 9A 31 "+                                     // checksum
          "01 02 03 04 "+                                     // message version
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "67 65 74 68 65 61 64 65 72 73 00 00 "+             // "getheaders"
          "41 00 00 00 "+                                     // payload 65 bytes long
          "56 4A 69 A5 "+                                     // checksum
          "01 "+                                              // 1 start hash
          "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F "+
          "00 01 02 03 04 05 06 07 08 09 0A "+                // end hash
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F")));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      GetHeadersMessage message = (GetHeadersMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"getheaders");
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getHashStarts().size(),1);
      Assert.assertEquals(message.getHashStarts().get(0),new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
      Assert.assertEquals(message.getHashStop(),new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
   }

   public void testGetHeadersSerialize()
      throws IOException
   {
      // Setup message
      List<byte[]> hashStarts = new ArrayList<byte[]>();
      hashStarts.add(
            new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 });
      byte[] hashStop = 
            new byte[] 
            { 31,30,29,28,27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1,0 };
      GetHeadersMessage getheaders = new GetHeadersMessage(Constants.PRODNET_MESSAGE_MAGIC,hashStarts,hashStop);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(getheaders,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network
            "61 61 61 00 00 00 00 00 00 00 00 00 "+  // "aaa" command (unrecognized)
            "10 00 00 00 "+                          // payload is 16 bytes
            "12 34 56 78 "+                          // checksum
            "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F "+ // junk payload
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "00 00 00 00 "+                          // Payload is 0 bytes long
            "5D F6 E0 E2")));                        // Checksum
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      VerackMessage message = (VerackMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"verack");
      Assert.assertEquals(message.getLength(),0);
   }

   public void testTxDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
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
          "00 00 00 00 ")));                                    // lock time
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      TxMessage message = (TxMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"tx");
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      Assert.assertEquals(message.getTx().getVersion(),1);
      Assert.assertEquals(message.getTx().getLockTime(),0);
      Assert.assertEquals(message.getTx().getInputs().size(),1);
      Assert.assertEquals(message.getTx().getInputs().get(0).getReferencedTxOutIndex(),0);
      Assert.assertEquals(message.getTx().getInputs().get(0).getSequence(),0xffffffffl);
      Assert.assertEquals(message.getTx().getInputs().get(0).getSignatureScript().length,139);
      Assert.assertEquals(message.getTx().getOutputs().size(),2);
      Assert.assertEquals(message.getTx().getOutputs().get(0).getValue(),5000000l);
      Assert.assertEquals(message.getTx().getOutputs().get(1).getValue(),3354000000l);
      Assert.assertEquals(message.getTx().getOutputs().get(0).getScript().length,25);
      Assert.assertEquals(message.getTx().getOutputs().get(1).getScript().length,25);
   }

   public void testTxSerialize()
      throws IOException
   {
      // Setup message
      TxIn in1 = new TxIn(
            HexUtil.toByteArray(
                "29 36 EE 6A 0D B4 E4 90 19 88 50 3B B6 E9 66 12 "+   // previous output (outpoint)
                "8D D5 FA 01 BC F0 84 51 F7 8A 1D 5B 08 DB BD 6D "),0,
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
      TxMessage tx = new TxMessage(Constants.PRODNET_MESSAGE_MAGIC,new Tx(1,inputs,outputs,0));
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(tx,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
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
                "29 36 EE 6A 0D B4 E4 90 19 88 50 3B B6 E9 66 12 "+   // previous output (outpoint)
                "8D D5 FA 01 BC F0 84 51 F7 8A 1D 5B 08 DB BD 6D "),0,
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
      Tx tx = new Tx(1,inputs,outputs,0);
      List<Tx> transactions = new ArrayList<Tx>();
      transactions.add(tx);
      BlockMessage block = new BlockMessage(Constants.PRODNET_MESSAGE_MAGIC,
            new BlockHeader(1,
            HexUtil.toByteArray(
               "1F 1E 1D 1C 1B 1A 19 18 17 16 15 14 13 12 11 10 "+
               "0F 0E 0D 0C 0B 0A 09 08 07 06 05 04 03 02 01 00"),
            HexUtil.toByteArray(
               "1F 1E 1D 1C 1B 1A 19 18 17 16 15 14 13 12 11 10 "+
               "0F 0E 0D 0C 0B 0A 09 08 07 06 05 04 03 02 01 00"),
            123000,22,33),
            transactions);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(block,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
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
          "00 00 00 00")));                                     // lock time
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      BlockMessage message = (BlockMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
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
               "1F 1E 1D 1C 1B 1A 19 18 17 16 15 14 13 12 11 10 "+
               "0F 0E 0D 0C 0B 0A 09 08 07 06 05 04 03 02 01 00"),
            HexUtil.toByteArray(
               "1F 1E 1D 1C 1B 1A 19 18 17 16 15 14 13 12 11 10 "+
               "0F 0E 0D 0C 0B 0A 09 08 07 06 05 04 03 02 01 00"),
            123000,22,33);
      List<BlockHeader> headers = new ArrayList<BlockHeader>();
      headers.add(header);
      HeadersMessage headersMessage = new HeadersMessage(Constants.PRODNET_MESSAGE_MAGIC,headers);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(headersMessage,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
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
          "21 00 00 00")));                                     // nonce
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      HeadersMessage message = (HeadersMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
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
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "67 65 74 61 64 64 72 00 00 00 00 00 "+  // "getaddr" command
            "00 00 00 00 "+                          // payload is 0 bytes
            "5D F6 E0 E2")));                        // checksum is null
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      GetAddrMessage message = (GetAddrMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"getaddr");
      Assert.assertEquals(message.getLength(),0);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
   }

   public void testGetAddrSerialize()
      throws IOException
   {
      // Setup a verack message
      GetAddrMessage getaddr = new GetAddrMessage(Constants.PRODNET_MESSAGE_MAGIC);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(getaddr,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "67 65 74 61 64 64 72 00 00 00 00 00 "+  // "getaddr" command
            "00 00 00 00 "+                          // Payload is 0 bytes long
            "5D F6 E0 E2");                          // no checksum
   }

   public void testPingDeserialize()
      throws IOException
   {
      // Sample taken from bitcoin wiki
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "70 69 6E 67 00 00 00 00 00 00 00 00 "+  // "ping" command
            "00 00 00 00 "+                          // payload is 0 bytes
            "5D F6 E0 E2")));                        // checksum is null
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      PingMessage message = (PingMessage) marshal.read(input);
      // Check
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"ping");
      Assert.assertEquals(message.getLength(),0);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
   }

   public void testPingSerialize()
      throws IOException
   {
      // Setup a verack message
      PingMessage ping = new PingMessage(Constants.PRODNET_MESSAGE_MAGIC);
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(ping,output);
      // Check output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "70 69 6E 67 00 00 00 00 00 00 00 00 "+  // "ping" command
            "00 00 00 00 "+                          // Payload is 0 bytes long
            "5D F6 E0 E2");                          // no checksum
   }

   public void testAlertDeserialize()
      throws IOException
   {
      // First alert message sent on production network:
      // "See bitcoin.org/feb20 if you have trouble connecting after 20 February"
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "61 6C 65 72 74 00 00 00 00 00 00 00 "+  // "alert" command
            "B2 00 00 00 "+                          // Payload is 178 bytes long
            "4F E6 8F E9 "+                          // checksum
            "73 01 00 00 00 37 66 40 4F 00 00 00 00 B3 05 43 4F 00 00 "+
            "00 00 F2 03 00 00 F1 03 00 00 00 10 27 00 00 48 EE 00 00 "+
            "00 64 00 00 00 00 46 53 65 65 20 62 69 74 63 6F 69 6E 2E "+
            "6F 72 67 2F 66 65 62 32 30 20 69 66 20 79 6F 75 20 68 61 "+
            "76 65 20 74 72 6F 75 62 6C 65 20 63 6F 6E 6E 65 63 74 69 "+
            "6E 67 20 61 66 74 65 72 20 32 30 20 46 65 62 72 75 61 72 "+
            "79 00 47 30 45 02 21 00 83 89 DF 45 F0 70 3F 39 EC 8C 1C "+
            "C4 2C 13 81 0F FC AE 14 99 5B B6 48 34 02 19 E3 53 B6 3B "+
            "53 EB 02 20 09 EC 65 E1 C1 AA EE C1 FD 33 4C 6B 68 4B DE "+
            "2B 3F 57 30 60 D5 B7 0C 3A 46 72 33 26 E4 E8 A4 F1"
            )));
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      AlertMessage message = (AlertMessage) marshal.read(input);
      // Check header
      System.out.println("Signature: "+BtcUtil.hexOut(message.getSignature())+" len: "+message.getSignature().length);
      Assert.assertEquals(message.getMagic(),Constants.PRODNET_MESSAGE_MAGIC);
      Assert.assertEquals(message.getCommand(),"alert");
      Assert.assertEquals(message.getLength(),178);
      Assert.assertTrue(message.verify(),"message could not be verified, checksum error");
      // Check message fields
      Assert.assertEquals(message.getVersion(),1);
      Assert.assertEquals(message.getRelayUntil(),1329620535);
      Assert.assertEquals(message.getExpiration(),1329792435);
      Assert.assertEquals(message.getId(),1010);
      Assert.assertEquals(message.getCancel(),1009);
      Assert.assertEquals(message.getMinVer(),10000);
      Assert.assertEquals(message.getMaxVer(),61000);
      Assert.assertEquals(message.getPriority(),100);
      Assert.assertEquals(message.getMessage(),"See bitcoin.org/feb20 if you have trouble connecting after 20 February");
      Assert.assertTrue(BitcoinUtil.verifyDoubleDigestSatoshiSignature(message.getAlertPayload(), message.getSignature()));
   }
   
  /* For reference, this is the second alert message sent on production network:
              "01 00 00 00 F6 FA 63 4F 00 00 00 00 F2 2A 45 51 00 00 00 00 "+
              "F3 03 00 00 F2 03 00 00 00 50 C3 00 00 7C C4 00 00 00 88 13 "+
              "00 00 00 4A 55 52 47 45 4E 54 3A 20 73 65 63 75 72 69 74 79 "+
              "20 66 69 78 20 66 6F 72 20 42 69 74 63 6F 69 6E 2D 51 74 20 "+
              "6F 6E 20 57 69 6E 64 6F 77 73 3A 20 68 74 74 70 3A 2F 2F 62 "+
              "69 74 63 6F 69 6E 2E 6F 72 67 2F 63 72 69 74 66 69 78 00"
  */

   // TODO: Create a test key and use it to sign an AlertMessage and do unit testing
   public void testAlertSerialize()
      throws IOException
   {
      // Setup the message
      AlertMessage alert = new AlertMessage(Constants.PRODNET_MESSAGE_MAGIC,"See bitcoin.org/feb20 if you have trouble connecting after 20 February");
      alert.setVersion(1);
      alert.setRelayUntil(1329620535);
      alert.setExpiration(1329792435);
      alert.setId(1010);
      alert.setCancel(1009);
      alert.setMinVer(10000);
      alert.setMaxVer(61000);
      alert.setPriority(100);
      alert.setSignature(new byte[] { 0x33, 0x44 });
      // Serialize it
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      BitcoinOutputStream output = new BitcoinOutputStream(byteOutput);
      marshal.write(alert,output);
      
      // Deserialize it and check that all fields are correctly retained
      AlertMessage deserAlert = (AlertMessage) marshal.read(new BitcoinInputStream(new ByteArrayInputStream(byteOutput.toByteArray())));
      Assert.assertEquals(deserAlert.getVersion(), 1);
      Assert.assertEquals(deserAlert.getRelayUntil(), 1329620535);
      Assert.assertEquals(deserAlert.getExpiration(), 1329792435);
      Assert.assertEquals(deserAlert.getId(), 1010);
      Assert.assertEquals(deserAlert.getCancel(), 1009);
      Assert.assertEquals(deserAlert.getMinVer(), 10000);
      Assert.assertEquals(deserAlert.getMaxVer(), 61000);
      Assert.assertEquals(deserAlert.getPriority(), 100);

      // Check complete output
      Assert.assertEquals(HexUtil.toHexString(byteOutput.toByteArray()),
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "61 6C 65 72 74 00 00 00 00 00 00 00 "+  // "alert" command
            "77 00 00 00 "+                          // Payload is 119 bytes long
            "C1 C5 92 D9 "+                          // checksum
            "73 01 00 00 00 37 66 40 4F 00 00 00 00 B3 05 43 4F 00 00 "+
            "00 00 F2 03 00 00 F1 03 00 00 00 10 27 00 00 48 EE 00 00 "+
            "00 64 00 00 00 00 46 53 65 65 20 62 69 74 63 6F 69 6E 2E "+
            "6F 72 67 2F 66 65 62 32 30 20 69 66 20 79 6F 75 20 68 61 "+
            "76 65 20 74 72 6F 75 62 6C 65 20 63 6F 6E 6E 65 63 74 69 "+
            "6E 67 20 61 66 74 65 72 20 32 30 20 46 65 62 72 75 61 72 "+
            "79 00 02 33 44"
         );
   }
   
   // Obsolete, should be removed?
   /*
   public void testFutureProofMessageParsing()
      throws IOException
   {
      // Prepare a verack message that is longer than normal
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "05 00 00 00 "+                          // Payload is 5 bytes long (0 normally)
            "01 02 03 04 05 "+                       // "payload"
            "F9 BE B4 D9 "+                          // Main network magic bytes (next verack)
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "00 00 00 00 ")));                       // Payload is 5 bytes long (0 normally)
      // Unmarshall 2 messages
      MessageMarshaller marshal = new MessageMarshaller();
      VerackMessage message = (VerackMessage) marshal.read(input);
      message = (VerackMessage) marshal.read(input);
   }

   public void testFutureProofChecksum()
      throws IOException
   {
      // This is an alert with extra bytes
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "F9 BE B4 D9 "+                          // Main network magic bytes
            "61 6C 65 72 74 00 00 00 00 00 00 00 "+  // "alert" command
            "0D 00 00 00 "+                          // Payload is 10 bytes long + 3 bytes extra
            "81 0D 54 F8 "+                          // checksum
            "06 "+                                   // length of message
            "41 6C 65 72 74 21 "+                    // message "Alert!"
            "02 "+                                   // length of signature
            "4D 65 "+                                // signature "Me"
            "01 02 03")));                           // 3 extra bytes
      // Unmarshall 2 messages
      MessageMarshaller marshal = new MessageMarshaller();
      AlertMessage message = (AlertMessage) marshal.read(input);
      Assert.assertTrue(message.verify(),"could not verify message");
   }
   */

   @Test(expectedExceptions=IOException.class)
   public void testWrongMagicDeserialize()
      throws IOException
   {
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
            "11 22 33 44 "+                          // Wrong magic
            "76 65 72 61 63 6B 00 00 00 00 00 00 "+  // "verack" command
            "00 00 00 00")));                        // Payload is 0 bytes long
      // Unmarshall
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      VerackMessage message = (VerackMessage) marshal.read(input);
   }

   @Test(expectedExceptions=IOException.class)
   public void testWrongChecksumDeserialize()
      throws IOException
   {
      BitcoinInputStream input = new BitcoinInputStream(new ByteArrayInputStream(HexUtil.toByteArray(
          "F9 BE B4 D9 "+                                     // Main network magic bytes
          "69 6E 76 00 00 00 00 00 00 00 00 00 "+             // "inv"
          "25 00 00 00 "+                                     // payload is 36 bytes long
          "41 01 8A 31 "+                                     // wrong checksum
          "01 "+                                              // number of items
          "01 00 00 00 "+                                     // type 1 (tx)
          "00 01 02 03 04 05 06 07 08 09 0A "+                // hash of tx
          "0B 0C 0D 0E 0F 10 11 12 13 14 15 "+
          "16 17 18 19 1A 1B 1C 1D 1E 1F")));
      // Try to deserialize
      MessageMarshaller marshal = new MessageMarshaller(Constants.PRODNET_MESSAGE_MAGIC);
      InvMessage message = (InvMessage) marshal.read(input);
   }
}

