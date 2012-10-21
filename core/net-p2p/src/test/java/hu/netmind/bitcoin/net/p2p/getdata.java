/**
 * Copyright (C) 2011 NetMind Consulting Bt.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package hu.netmind.bitcoin.net.p2p;

import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.ProdnetBitcoinFactory;
import hu.netmind.bitcoin.block.Testnet2BitcoinFactory;
import it.nibbles.bitcoin.utils.BtcUtil;
import hu.netmind.bitcoin.net.p2p.source.FallbackNodesSource;
import hu.netmind.bitcoin.net.BitCoinOutputStream;
import hu.netmind.bitcoin.net.GetDataMessage;
import hu.netmind.bitcoin.net.Message;
import hu.netmind.bitcoin.net.VersionMessage;
import hu.netmind.bitcoin.net.VerackMessage;
import hu.netmind.bitcoin.net.NodeAddress;
import hu.netmind.bitcoin.net.BlockMessage;
import hu.netmind.bitcoin.net.TxMessage;
import hu.netmind.bitcoin.net.InventoryItem;
import hu.netmind.bitcoin.net.MessageMarshaller;
import hu.netmind.bitcoin.net.p2p.source.DNSFallbackNodesSource;
import hu.netmind.bitcoin.net.p2p.source.RandomizedNodesSource;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/**
 * Execute a getdata with the given parameters.
 *
 * @author Robert Brautigam
 */
public class getdata
{

   static BitcoinFactory bitcoinFactory;

   public static void main(String[] argv)
      throws Exception
   {
      // Get parameters
      if (argv.length < 2)
      {
         System.out.println("USAGE: getdata testnet|prodnet <type> <hash numbers>*");
         return;
      }
      final boolean isTestnet = "testnet".equalsIgnoreCase(argv[0]);
      bitcoinFactory = isTestnet ? new Testnet2BitcoinFactory(null) : new ProdnetBitcoinFactory(null);
      final int type = Integer.parseInt(argv[1]);
//      StringBuilder builder = new StringBuilder(argv[1]);
//      for ( int i=2; i<argv.length; i++ )
//         builder.append(" "+argv[i]);
//      final byte[] hash = HexUtil.toByteArray(builder.toString());
      final byte[] hash = BtcUtil.hexIn(argv[2]);
      //final long messageMagic = bitcoinFactory.getMessageMagic();

      System.out.println("BitCoin getdata request " + type + " " + BtcUtil.hexOut(hash) + " on " + (isTestnet ? "testnet" : "prodnet"));
      // Initialize node
      final Node node = new Node(bitcoinFactory.getMessageMagic());
      node.setMinConnections(1);
      node.setMaxConnections(1);
      if (isTestnet)
         node.setAddressSource(new LocalhostTestnetNodeSource());
      else
         node.setAddressSource(new DNSFallbackNodesSource());
      node.addHandler(new MessageHandler()
      {
         @Override
         public void onJoin(Connection conn) throws IOException
         {
            System.out.println("Connected to " + conn.getRemoteAddress() + " (from: " + conn.getLocalAddress() + ")");
            // Send our version information
            VersionMessage version = bitcoinFactory.getMessageFactory().newVersionMessage(
               32100, 1, System.currentTimeMillis() / 1000,
               new NodeAddress(1, (InetSocketAddress) conn.getRemoteAddress()),
               new NodeAddress(1, new InetSocketAddress(((InetSocketAddress) conn.getLocalAddress()).getAddress(), node.getPort())),
               123, "", 0);
            System.out.println("Sending handshake version: " + version);
            conn.send(version);
         }

         public void onLeave(Connection conn)
         {
            System.out.println("Disconnected from " + conn.getRemoteAddress() + " (on local: " + conn.getLocalAddress() + ")");
         }

         public void onMessage(Connection conn, Message message)
            throws IOException
         {
            System.out.println("Incoming (" + conn.getRemoteAddress() + "): " + message);
            if (message instanceof VersionMessage)
            {
               // Let's answer version, so we get more messages
               VerackMessage verack = bitcoinFactory.getMessageFactory().newVerackMessage();
               System.out.println("Answering: " + verack);
               conn.send(verack);
            }
            if (message instanceof VerackMessage)
            {
               // Send our request
               InventoryItem item = new InventoryItem(type, hash);
               List<InventoryItem> items = new ArrayList<InventoryItem>();
               items.add(item);
               GetDataMessage getdataMessage = bitcoinFactory.getMessageFactory().newGetDataMessage(items);
               System.out.println("Sending a request to get data: " + getdataMessage);
               conn.send(getdataMessage);
            }
            if ((message instanceof TxMessage) || (message instanceof BlockMessage))
            {
               if (message instanceof BlockMessage)
               {
                  BlockMessage blockMessage = (BlockMessage) message;
                  System.out.println("Block with " + blockMessage.getTransactions().size() + " transactions");
               }
               // Serialize it and display
               ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
               BitCoinOutputStream output = new BitCoinOutputStream(byteOutput);
               MessageMarshaller marshaller = new MessageMarshaller(bitcoinFactory.getMessageMagic());
               marshaller.write(message, output);
               System.out.println("Got data:\n" + BtcUtil.hexOut(byteOutput.toByteArray()));
               // We got our answer, stop node
               System.out.println("Answer received, exiting.");
               conn.close();
            }
         }
      });
      // Start node, then wait indefinitly
      node.start();
      // Wait indefinitly
      Object waitObject = new Object();
      synchronized (waitObject)
      {
         waitObject.wait();
      }
   }

   public static class LocalhostTestnetNodeSource extends RandomizedNodesSource
   {

      @Override
      protected List<InetSocketAddress> getInitialAddresses()
      {
         List<InetSocketAddress> list = new ArrayList<>();
         list.add(new InetSocketAddress("127.0.0.1", 18333));
         return list;
      }
   }
}
