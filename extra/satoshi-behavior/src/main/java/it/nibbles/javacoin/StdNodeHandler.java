/**
 * Copyright (C) 2012 nibbles.it
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
package it.nibbles.javacoin;

import it.nibbles.javacoin.net.BlockMessage;
import it.nibbles.javacoin.net.VerackMessage;
import it.nibbles.javacoin.net.Message;
import it.nibbles.javacoin.net.NodeAddress;
import it.nibbles.javacoin.net.AddrMessage;
import it.nibbles.javacoin.net.AlertMessage;
import it.nibbles.javacoin.net.TxMessage;
import it.nibbles.javacoin.net.InvMessage;
import it.nibbles.javacoin.net.NetworkMessageFactory;
import it.nibbles.javacoin.net.InventoryItem;
import it.nibbles.javacoin.net.GetBlocksMessage;
import it.nibbles.javacoin.net.PingMessage;
import it.nibbles.javacoin.net.VersionMessage;
import it.nibbles.javacoin.BitcoinException;
import it.nibbles.javacoin.Block;
import it.nibbles.javacoin.BlockChain;
import it.nibbles.javacoin.block.BitcoinFactory;
import it.nibbles.javacoin.block.BlockChainLink;
import it.nibbles.javacoin.block.BlockChainLinkStorage;
import it.nibbles.javacoin.block.BlockImpl;
import it.nibbles.javacoin.block.TransactionImpl;
import it.nibbles.javacoin.keyfactory.ecc.BitcoinUtil;
import it.nibbles.javacoin.net.p2p.Connection;
import it.nibbles.javacoin.net.p2p.MessageHandler;
import it.nibbles.javacoin.net.p2p.Node;
import it.nibbles.javacoin.net.p2p.NodeStorage;
import it.nibbles.javacoin.utils.BtcUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alessandro Polverini
 */
public class StdNodeHandler implements MessageHandler, Runnable
{

   private static Logger logger = LoggerFactory.getLogger(StdNodeHandler.class);
   private static long BC_PROTOCOL_VERSION = 32100;
   private Node node;
   private BitcoinFactory bitcoinFactory;
   private BlockChain chain;
   private BlockChainLinkStorage storage;
   private NodeStorage nodeStorage;
   private NetworkMessageFactory messageFactory;
   private byte[] highestHashKnownBeforeRequest = null;
   private byte[] highestHashPromised = null;
   private transient Connection downloadingFromPeer = null;
   private int numMessages = 0;

   public StdNodeHandler(Node node, BitcoinFactory bitcoinFactory, BlockChain chain, BlockChainLinkStorage storage, NodeStorage nodeStorage)
   {
      this.node = node;
      this.bitcoinFactory = bitcoinFactory;
      this.chain = chain;
      this.storage = storage;
      this.nodeStorage = nodeStorage;
      this.messageFactory = bitcoinFactory.getMessageFactory();
      node.addHandler(this);
   }

   @Override
   public void onJoin(Connection conn)
           throws IOException
   {
      long ourHeight = chain.getHeight();
      logger.debug("connected to " + conn.getRemoteAddress() + " (from: " + conn.getLocalAddress() + ") sending out height: " + ourHeight);
      // Send our version information
      VersionMessage version = messageFactory.newVersionMessage(BC_PROTOCOL_VERSION, 0, System.currentTimeMillis() / 1000,
              new NodeAddress(1, (InetSocketAddress) conn.getRemoteAddress()),
              new NodeAddress(1, new InetSocketAddress(((InetSocketAddress) conn.getLocalAddress()).getAddress(), node.getPort())),
              //new NodeAddress(1, new InetSocketAddress("127.0.0.1", node.getPort())),
              new Random().nextLong(), "JavaCoin/1.0-DEV", ourHeight);
      conn.send(version);
   }

   @Override
   public void onLeave(Connection conn)
           throws IOException
   {
      if (conn == downloadingFromPeer)
         downloadingFromPeer = null;
      logger.debug("disconnected from " + conn.getRemoteAddress() + " (on local: " + conn.getLocalAddress() + ") removed: " + conn);
   }

   @Override
   public synchronized void onMessage(Connection conn, Message message)
           throws IOException
   {
      numMessages++;
      PeerData peerData = (PeerData) conn.getSessionAttribute("peerData");
      if (peerData == null)
      {
         peerData = new PeerData();
         conn.setSessionAttribute("peerData", peerData);
      }
      peerData.newMessage(message);
      logger.debug("[#" + numMessages + "] incoming (" + conn.getRemoteAddress() + "): " + message.getClass() + " currPeer: " + peerData);
      if (message instanceof AlertMessage)
      {
         AlertMessage alertMessage = (AlertMessage) message;
         logger.info("ALERT id=" + alertMessage.getId() + " message='" + alertMessage.getMessage() + "' comment='" + alertMessage.getComment()
                 + "' signatureVerified: " + BitcoinUtil.verifyDoubleDigestSatoshiSignature(alertMessage.getAlertPayload(), alertMessage.getSignature()));
      } else if (message instanceof VersionMessage)
      {
         VersionMessage version = (VersionMessage) message;
         // Let's answer version, so we get more messages
         VerackMessage verack = messageFactory.newVerackMessage();
         logger.debug("Answering verack to VersionMessage: " + version);
         conn.send(verack);
      } else if (message instanceof VerackMessage)
      {
         VerackMessage verack = (VerackMessage) message;
         logger.debug("Version ack: " + verack);
      } else if (message instanceof AddrMessage)
      {
         AddrMessage addr = (AddrMessage) message;
         String peers = "AddrMessage[" + addr + "] peers: ";
         for (AddrMessage.AddressEntry entry : addr.getAddressEntries())
         {
            if ((entry.getAddress().getServices() & 1) != 0)
               if (nodeStorage != null)
                  nodeStorage.storeNodeAddress(entry.getAddress());
               else
                  logger.debug("Entry " + entry + " not stored because no nodeStorage provided");
            peers += entry.getAddress() + " ";
         }
         logger.debug(peers);
      } else if (message instanceof InvMessage)
      {
         InvMessage invMessage = (InvMessage) message;
         // Received inv message, request the data for all blocks,
         List<InventoryItem> items = new LinkedList<>(invMessage.getInventoryItems());
         Iterator<InventoryItem> itemIterator = items.iterator();
         while (itemIterator.hasNext())
         {
            InventoryItem item = itemIterator.next();
            if (item.getType() == InventoryItem.TYPE_TX)
            {
               //logger.debug("Inv nuova transazione: " + BtcUtil.hexOut(item.getHash()));
            } else if (item.getType() == InventoryItem.TYPE_BLOCK)
               //logger.debug("Inv nuovo blocco: " + BtcUtil.hexOut(item.getHash()));
               // Determine the last promised block, so we know later when we're finished
               highestHashPromised = item.getHash();
            else
            {
               logger.debug("Item inventory sconosciuto: " + item.getType() + " " + BtcUtil.hexOut(item.getHash()));
               itemIterator.remove();
            }
         }
         // Do the request for all blocks remaining
         if (!items.isEmpty())
         {
            conn.send(messageFactory.newGetDataMessage(items));
            logger.debug("Reply to INV using getdata -- highestHashPromised: " + BtcUtil.hexOut(highestHashPromised));
         }
      } else if (message instanceof TxMessage)
         try
         {
            long startTime = System.currentTimeMillis();
            TransactionImpl tx = TransactionImpl.createTransaction(bitcoinFactory.getScriptFactory(), ((TxMessage) message).getTx());
            tx.validate();
            long diffTime = System.currentTimeMillis() - startTime;
            logger.debug("New transaction {} validated in {} ms", BtcUtil.hexOut(tx.getHash()), diffTime);
         } catch (BitcoinException ex)
         {
            logger.error("Can't create transaction from tx message: " + message);
         }
      else if (message instanceof BlockMessage)
      {
         BlockImpl block = null;
         try
         {
            block = BlockImpl.createBlock(bitcoinFactory.getScriptFactory(), (BlockMessage) message);
            logger.debug("Received block {} created {}", BtcUtil.hexOut(block.getHash()), new Date(block.getCreationTime()));
            // Check whether we are finished with the download, even before trying to add
            if (Arrays.equals(highestHashPromised, block.getHash()))
            {
               // Download stops
               logger.debug("download from " + downloadingFromPeer + " finished for batch...");
               downloadingFromPeer = null;
               highestHashPromised = null;
               highestHashKnownBeforeRequest = null;
            }
            // Now try to add to chain
            long startTime = System.currentTimeMillis();
            chain.addBlock(block);
            long stopTime = System.currentTimeMillis();
            logger.debug("Block " + BtcUtil.hexOut(block.getHash()) + " with " + block.getTransactions().size() + " transactions added in " + (stopTime - startTime) + " ms ");
         } catch (BitcoinException e)
         {
            logger.warn("block could not be added, marking peer " + conn + " as unreliable", e);
            if (peerData != null && block != null)
               peerData.newBadBlock(block);
         }
      } else if (message instanceof PingMessage)
      {
         logger.debug("Ping message: " + message);
         conn.send(messageFactory.newPingMessage());
      } else
         logger.debug("[#" + numMessages + "] unhandled message (" + conn.getRemoteAddress() + "): " + message.getClass());

      if (downloadingFromPeer == null && peerData.numBadBlocks() == 0)
      {
         BlockChainLink lastStoredLink = storage.getLastLink();
         if (peerData.getVersion().getStartHeight() > lastStoredLink.getHeight())
         {
            downloadingFromPeer = conn;
            highestHashKnownBeforeRequest = lastStoredLink.getBlock().getHash();
            List<byte[]> startBlocks = chain.buildBlockLocator();
            logger.debug("We are at " + lastStoredLink.getHeight() + " / " + BtcUtil.hexOut(highestHashKnownBeforeRequest)
                    + ", while known max is: " + peerData.getVersion().getStartHeight() + " Sending getblocks to " + downloadingFromPeer);
            GetBlocksMessage getBlocks = messageFactory.newGetBlocksMessage(BC_PROTOCOL_VERSION, startBlocks, null);
            conn.send(getBlocks);
         }
      }
   }

   @Override
   public void run()
   {
      try
      {
         node.start();
         logger.info("StdNodeHandler node startup");
      } catch (IOException e)
      {
         logger.error("IOException: " + e.getMessage(), e);
      }

   }

   public void stop()
   {
      logger.info("StdNodeHandler stopping node...");
      node.stop();
   }

   protected class PeerData
   {

      private VersionMessage version;
      //private Connection connection;
      private long timeOfLastReceivedMessage;
      // Number of messages received after initial Version
      private int numReceivedMessages;
      private Map<byte[], Block> badBlocks = new HashMap<>();

      public VersionMessage getVersion()
      {
         return version;
      }

      public void newMessage(Message m)
      {
         if (m instanceof VersionMessage)
            this.version = (VersionMessage) m;
         timeOfLastReceivedMessage = System.currentTimeMillis();
         numReceivedMessages++;
      }

      public void newBadBlock(Block b)
      {
         badBlocks.put(b.getHash(), b);
      }

      public int numReceivedMessages()
      {
         return numReceivedMessages;
      }

      public long timeOfLastMessageReceived()
      {
         return timeOfLastReceivedMessage;
      }

      public int numBadBlocks()
      {
         return badBlocks.size();
      }

      @Override
      public String toString()
      {
         return "PeerData[numFailedBlocks: " + badBlocks.size() + " messages: " + numReceivedMessages
                 + " timeOfLastMessage: " + new Date(timeOfLastReceivedMessage) + "]";
      }
   }
}
