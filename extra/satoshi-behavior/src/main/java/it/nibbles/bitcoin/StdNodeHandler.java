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
package it.nibbles.bitcoin;

import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.Block;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.block.BlockChainLinkStorage;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.keyfactory.ecc.BitcoinUtil;
import hu.netmind.bitcoin.net.*;
import hu.netmind.bitcoin.net.p2p.Connection;
import hu.netmind.bitcoin.net.p2p.MessageHandler;
import hu.netmind.bitcoin.net.p2p.Node;
import hu.netmind.bitcoin.net.p2p.NodeStorage;
import it.nibbles.bitcoin.utils.BtcUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
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
   private ScriptFactory scriptFactory;
   private long messageMagic;
   private BlockChain chain;
   private BlockChainLinkStorage storage;
   private byte[] highestHashKnownBeforeRequest = null;
   private byte[] highestHashPromised = null;
   private transient Connection downloadingFromPeer = null;
   private int numMessages = 0;

   public StdNodeHandler(Node node, ScriptFactory scriptFactory, long messageMagic, BlockChain chain, BlockChainLinkStorage storage)
   {
      this.node = node;
      this.scriptFactory = scriptFactory;
      this.messageMagic = messageMagic;
      this.chain = chain;
      this.storage = storage;
      node.addHandler(this);
   }

   @Override
   public void onJoin(Connection conn)
           throws IOException
   {
      long ourHeight = chain.getHeight();
      logger.debug("connected to " + conn.getRemoteAddress() + " (from: " + conn.getLocalAddress() + ") sending out height: " + ourHeight);
      // Send our version information
      VersionMessage version = new VersionMessage(messageMagic, BC_PROTOCOL_VERSION, 0, System.currentTimeMillis() / 1000,
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
         VerackMessage verack = new VerackMessage(messageMagic);
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
               if (storage instanceof NodeStorage)
                  ((NodeStorage) storage).storeNodeAddress(entry.getAddress());
               else
                  logger.debug("Entry " + entry + " not stored because no service capability");
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
            conn.send(new GetDataMessage(messageMagic, items));
            logger.debug("Reply to INV using getdata -- highestHashPromised: " + BtcUtil.hexOut(highestHashPromised));
         }
      } else if (message instanceof TxMessage)
         try
         {
            long startTime = System.currentTimeMillis();
            TransactionImpl tx = TransactionImpl.createTransaction(scriptFactory, ((TxMessage) message).getTx());
            tx.validate();
            long diffTime = System.currentTimeMillis() - startTime;
            logger.debug("New transaction {} validated in {} ms", BtcUtil.hexOut(tx.getHash()), diffTime);
         } catch (BitCoinException ex)
         {
            logger.error("Can't create transaction from tx message: " + message);
         }
      else if (message instanceof BlockMessage)
      {
         BlockImpl block = null;
         try
         {
            block = BlockImpl.createBlock(scriptFactory, (BlockMessage) message);
            logger.debug("Inserting block {} created {}", BtcUtil.hexOut(block.getHash()), new Date(block.getCreationTime()));
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
         } catch (BitCoinException e)
         {
            logger.warn("block could not be added, marking peer " + conn + " as unreliable", e);
            if (peerData != null && block != null)
               peerData.newBadBlock(block);
         }
      } else if (message instanceof PingMessage)
      {
         logger.debug("Ping message: " + message);
         conn.send(new PingMessage(messageMagic));
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
            GetBlocksMessage getBlocks = new GetBlocksMessage(messageMagic, BC_PROTOCOL_VERSION, startBlocks, null);
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
