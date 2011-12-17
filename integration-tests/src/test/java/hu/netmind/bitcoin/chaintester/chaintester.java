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

package hu.netmind.bitcoin.chaintester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import hu.netmind.bitcoin.node.p2p.source.DNSFallbackNodesSource;
import hu.netmind.bitcoin.node.p2p.Node;
import hu.netmind.bitcoin.node.p2p.MessageHandler;
import hu.netmind.bitcoin.node.p2p.Connection;
import hu.netmind.bitcoin.node.p2p.VersionMessage;
import hu.netmind.bitcoin.node.p2p.VerackMessage;
import hu.netmind.bitcoin.node.p2p.GetBlocksMessage;
import hu.netmind.bitcoin.node.p2p.Message;
import hu.netmind.bitcoin.node.p2p.NodeAddress;
import hu.netmind.bitcoin.node.p2p.BlockMessage;
import hu.netmind.bitcoin.node.p2p.InvMessage;
import hu.netmind.bitcoin.node.p2p.InventoryItem;
import hu.netmind.bitcoin.node.p2p.GetDataMessage;
import hu.netmind.bitcoin.node.p2p.Tx;
import hu.netmind.bitcoin.node.p2p.TxOut;
import hu.netmind.bitcoin.node.p2p.TxIn;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.block.BlockChainImpl;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.block.TransactionImpl;
import hu.netmind.bitcoin.block.TransactionOutputImpl;
import hu.netmind.bitcoin.block.TransactionInputImpl;
import hu.netmind.bitcoin.block.BlockChainLink;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Arrays;

/**
 * This application downloads all th blocks available on the 
 * BitCoin network and adds them to the chain.
 * @author Robert Brautigam
 */
public class chaintester
{
   private static Logger logger = LoggerFactory.getLogger(chaintester.class);
   private static long BC_PROTOCOL_VERSION = 32100;

   private Node node = null;
   private BlockChain chain = null;
   private SimpleSqlStorage storage = null;
   private ScriptFactoryImpl scriptFactory = null;

   public static void main(String[] argv)
      throws Exception
   {
      chaintester app = new chaintester();
      try
      {
         logger.debug("init...");
         app.init();
         logger.debug("run...");
         app.run();
      } finally {
         logger.debug("close...");
         app.close();
      }
   }

   /**
    * Free used resources.
    */
   public void close()
   {
      if ( storage != null )
         storage.close();
   }

   /**
    * Initialize and bind components together.
    */
   public void init()
      throws BitCoinException
   {
      // Initialize the chain
      scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
      storage = new SimpleSqlStorage(scriptFactory,"data");
      chain = new BlockChainImpl(BlockImpl.MAIN_GENESIS,
            storage,scriptFactory,false);
      // Introduce a small check here that we can read back the genesis block correctly
      storage.getGenesisLink().getBlock().validate();
      logger.info("initialized chain, last link height: "+storage.getLastLink().getHeight());
      // Initialize p2p node
      node = new Node();
      node.setMinConnections(1);
      node.setMaxConnections(1);
      node.setAddressSource(new DNSFallbackNodesSource());
      node.addHandler(new DownloaderHandler());
   }

   /**
    * Run the client and listen for new blocks forever.
    */
   public void run()
   {
      try
      {
         // Start the node
         node.start();
         // Wait for keypress to end
         System.in.read();
      } catch ( Exception e ) {
         logger.error("error while starting node or waiting for enter",e);
      }
   }

   /**
    * This handler simply tries to download all missing blocks.
    */
   public class DownloaderHandler implements MessageHandler
   {
      private long knownHighestBlock = 0;
      private byte[] highestHashKnownBeforeRequest = null;
      private byte[] highestHashPromised = null;
      private boolean downloading = false;

      public void onJoin(Connection conn)
         throws IOException
      {
         logger.debug("connected to "+conn.getRemoteAddress()+" (from: "+conn.getLocalAddress()+")");
         // Send our version information
         VersionMessage version = new VersionMessage(Message.MAGIC_MAIN,BC_PROTOCOL_VERSION,0,System.currentTimeMillis()/1000,
               new NodeAddress(1,(InetSocketAddress) conn.getRemoteAddress()),
               new NodeAddress(1,new InetSocketAddress(((InetSocketAddress)conn.getLocalAddress()).getAddress(),node.getPort())),
               123,"NetMind BitCoin/1.0.0-SNAPSHOT",storage.getLastLink().getHeight());
         logger.debug("sending version information: "+version);
         conn.send(version);
      }

      public void onLeave(Connection conn)
         throws IOException
      {
         logger.debug("disconnected from "+conn.getRemoteAddress()+" (on local: "+conn.getLocalAddress()+")");
      }

      public void onMessage(Connection conn, Message message)
         throws IOException
      {
         logger.debug("incoming ("+conn.getRemoteAddress()+"): "+message.getClass());
         if ( message instanceof VersionMessage )
         {
            VersionMessage version = (VersionMessage) message;
            // Remember highest block we saw advertised
            if ( version.getStartHeight() > knownHighestBlock )
               knownHighestBlock = version.getStartHeight();
            // Let's answer version, so we get more messages
            VerackMessage verack = new VerackMessage(Message.MAGIC_MAIN);
            logger.debug("answering: "+verack);
            conn.send(verack);
         }
         if ( message instanceof InvMessage )
         {
            InvMessage invMessage = (InvMessage) message;
            // Received inv message, request the data for all blocks,
            // drop transactions (we don't care)
            List<InventoryItem> items = new LinkedList<InventoryItem>(
                  invMessage.getInventoryItems());
            Iterator<InventoryItem> itemIterator = items.iterator();
            while ( itemIterator.hasNext() )
            {
               InventoryItem item = itemIterator.next();
               if ( item.getType() != InventoryItem.TYPE_BLOCK )
                  itemIterator.remove();
            }
            // Determine the last promised block, so we know later when we're finished
            if ( highestHashPromised == null )
               highestHashPromised = invMessage.getInventoryItems().get(invMessage.getInventoryItems().size()-1).getHash();
            // Do the request for all blocks remaining
            if ( ! items.isEmpty() )
               conn.send(new GetDataMessage(Message.MAGIC_MAIN,items));
         }
         if ( message instanceof BlockMessage )
         {
            try
            {
               BlockMessage blockMessage = (BlockMessage) message;
               // Convert block from message to block impl
               List<Transaction> txs = new LinkedList<Transaction>();
               for ( Tx tx : blockMessage.getTransactions() )
               {
                  // First outs
                  List<TransactionOutputImpl> outs = new LinkedList<TransactionOutputImpl>();
                  for ( TxOut txOut : tx.getOutputs() )
                  {
                     TransactionOutputImpl out = new TransactionOutputImpl(txOut.getValue(),
                           scriptFactory.createFragment(txOut.getScript()));
                     outs.add(out);
                  }
                  // Then ins
                  List<TransactionInputImpl> ins = new LinkedList<TransactionInputImpl>();
                  for ( TxIn txIn : tx.getInputs() )
                  {
                     TransactionInputImpl in = new TransactionInputImpl(txIn.getReferencedTxHash(),
                           (int)txIn.getReferencedTxOutIndex(),scriptFactory.createFragment(
                              txIn.getSignatureScript()),txIn.getSequence());
                     ins.add(in);
                  }
                  // Create tx
                  TransactionImpl transaction = new TransactionImpl(ins,outs,tx.getLockTime());
                  txs.add(transaction);
               }
               BlockImpl block = new BlockImpl(txs,blockMessage.getHeader().getTimestamp(),
                     blockMessage.getHeader().getNonce(), blockMessage.getHeader().getDifficulty(),
                     blockMessage.getHeader().getPrevBlock(),blockMessage.getHeader().getRootHash());
               // Now add to chain
               long startTime = System.currentTimeMillis();
               chain.addBlock(block);
               long stopTime = System.currentTimeMillis();
               logger.debug("added block in "+(stopTime-startTime)+" ms");
               // Check whether we are finished with the download
               if ( Arrays.equals(highestHashPromised,block.getHash()) )
               {
                  logger.debug("download finished for batch...");
                  // Download stops
                  downloading = false;
                  highestHashPromised = null;
                  highestHashKnownBeforeRequest = null;
               }
            } catch ( BitCoinException e ) {
               logger.warn("block could not be added",e);
            }
         }
         // This is a logic to download all blocks. It is driven by event received
         // from other blocks (it could actually run in a separate thread)
         if ( ! downloading )
         {
            BlockChainLink link = storage.getLastLink();
            if ( knownHighestBlock <= link.getHeight() )
               return; // As far as we know we know everything, so no need to send anything
            highestHashKnownBeforeRequest = link.getBlock().getHash();
            downloading = true;
            // Send the request
            logger.debug("sending getblocks, we are at "+link.getHeight()+", while known max is: "+knownHighestBlock);
            List<byte[]> startBlocks = new LinkedList<byte[]>();
            startBlocks.add(highestHashKnownBeforeRequest);
            GetBlocksMessage getBlocks = new GetBlocksMessage(Message.MAGIC_MAIN,BC_PROTOCOL_VERSION,startBlocks,null);
            node.broadcast(getBlocks);
         }
      }
   }
}

