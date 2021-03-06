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
package it.nibbles.javacoin.chaintester;

import it.nibbles.javacoin.BitcoinException;
import it.nibbles.javacoin.BlockChain;
import it.nibbles.javacoin.ScriptFactory;
import it.nibbles.javacoin.block.BitcoinFactory;
import it.nibbles.javacoin.block.BlockImpl;
import it.nibbles.javacoin.block.ProdnetBitcoinFactory;
import it.nibbles.javacoin.keyfactory.ecc.KeyFactoryImpl;
import it.nibbles.javacoin.net.BlockMessage;
import it.nibbles.javacoin.net.InvMessage;
import it.nibbles.javacoin.net.InventoryItem;
import it.nibbles.javacoin.net.Message;
import it.nibbles.javacoin.net.NetworkMessageFactory;
import it.nibbles.javacoin.net.VerackMessage;
import it.nibbles.javacoin.net.VersionMessage;
import it.nibbles.javacoin.net.p2p.Connection;
import it.nibbles.javacoin.net.p2p.MessageHandler;
import it.nibbles.javacoin.net.p2p.Node;
import it.nibbles.javacoin.net.p2p.source.DNSFallbackNodesSource;
import it.nibbles.javacoin.script.ScriptFactoryImpl;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This application downloads all th blocks available on the Bitcoin network and
 * adds them to the chain.
 *
 * @author Robert Brautigam
 */
public class chaintester
{

  private static Logger logger = LoggerFactory.getLogger(chaintester.class);
  private static long BC_PROTOCOL_VERSION = 32100;
  private Node node = null;
  private BlockChain chain = null;
  //private BDBChainLinkStorage storage = null;
  //private ScriptFactoryImpl scriptFactory = null;
  private BitcoinFactory bitcoinFactory;

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
    } finally
    {
      logger.debug("close...");
      app.close();
    }
  }

  /**
   * Free used resources.
   */
  public void close()
  {
//      if ( storage != null )
//         storage.close();
  }

  /**
   * Initialize and bind components together.
   */
  public void init()
     throws BitcoinException
  {
    // Initialize the chain

    ScriptFactory scriptFactory = new ScriptFactoryImpl(new KeyFactoryImpl(null));
    bitcoinFactory = new ProdnetBitcoinFactory(scriptFactory);
//      storage = new BDBChainLinkStorage(scriptFactory);
//      storage.setDbPath("data");
//      storage.init();
    //chain = new BlockChainImpl(BlockImpl.MAIN_GENESIS, storage,scriptFactory,false);

//      chain = new BlockChainImpl(bitcoinFactory, storage, false);
    // Introduce a small check here that we can read back the genesis block correctly
//      storage.getGenesisLink().getBlock().validate();
//      logger.info("initialized chain, last link height: "+storage.getLastLink().getHeight());
    // Initialize p2p node
    node = new Node(bitcoinFactory.getMessageMagic());
    node.setPort(7321);
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
    } catch (Exception e)
    {
      logger.error("error while starting node or waiting for enter", e);
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
    private NetworkMessageFactory messageFactory = bitcoinFactory.getMessageFactory();

    @Override
    public void onJoin(Connection conn)
       throws IOException
    {
      logger.debug("connected to " + conn.getRemoteAddress() + " (from: " + conn.getLocalAddress() + ")");
      // Send our version information
//         VersionMessage version = new VersionMessage(bitcoinFactory.getMessageMagic(),BC_PROTOCOL_VERSION,0,System.currentTimeMillis()/1000,
//               new NodeAddress(1,(InetSocketAddress) conn.getRemoteAddress()),
//               new NodeAddress(1,new InetSocketAddress(((InetSocketAddress)conn.getLocalAddress()).getAddress(),node.getPort())),
//               123,"NetMind Bitcoin/1.0.0-SNAPSHOT",storage.getLastLink().getHeight());
//         logger.debug("sending version information: "+version);
//         conn.send(version);
    }

    @Override
    public void onLeave(Connection conn)
       throws IOException
    {
      logger.debug("disconnected from " + conn.getRemoteAddress() + " (on local: " + conn.getLocalAddress() + ")");
    }

    @Override
    public void onMessage(Connection conn, Message message)
       throws IOException
    {
      logger.debug("incoming (" + conn.getRemoteAddress() + "): " + message.getClass());
      if (message instanceof VersionMessage)
      {
        VersionMessage version = (VersionMessage) message;
        // Remember highest block we saw advertised
        if (version.getStartHeight() > knownHighestBlock)
          knownHighestBlock = version.getStartHeight();
        // Let's answer version, so we get more messages
        VerackMessage verack = messageFactory.newVerackMessage();
        logger.debug("answering: " + verack);
        conn.send(verack);
      }
      if (message instanceof InvMessage)
      {
        InvMessage invMessage = (InvMessage) message;
        // Received inv message, request the data for all blocks,
        // drop transactions (we don't care)
        List<InventoryItem> items = new LinkedList<InventoryItem>(
           invMessage.getInventoryItems());
        Iterator<InventoryItem> itemIterator = items.iterator();
        while (itemIterator.hasNext())
        {
          InventoryItem item = itemIterator.next();
          if (item.getType() != InventoryItem.TYPE_BLOCK)
            itemIterator.remove();
        }
        // Do the request for all blocks remaining
        if (!items.isEmpty())
        {
          // Determine the last promised block, so we know later when we're finished
          if (highestHashPromised == null)
            highestHashPromised = invMessage.getInventoryItems().get(invMessage.getInventoryItems().size() - 1).getHash();
          conn.send(messageFactory.newGetDataMessage(items));
          logger.debug("sent getdata, waiting for blocks...");
        }
      }
      if (message instanceof BlockMessage)
      {
        try
        {
          BlockImpl block = BlockImpl.createBlock(bitcoinFactory.getScriptFactory(), (BlockMessage) message);
          // Check whether we are finished with the download, even before trying to add
          if (Arrays.equals(highestHashPromised, block.getHash()))
          {
            logger.debug("download finished for batch...");
            // Download stops
            downloading = false;
            highestHashPromised = null;
            highestHashKnownBeforeRequest = null;
          }
          // Now add to chain
          long startTime = System.currentTimeMillis();
          chain.addBlock(block);
          long stopTime = System.currentTimeMillis();
          logger.debug("added block in " + (stopTime - startTime) + " ms");
        } catch (BitcoinException e)
        {
          logger.warn("block could not be added", e);
        }
      }
      // This is a logic to download all blocks. It is driven by event received
      // from other blocks (it could actually run in a separate thread)
      if (!downloading)
      {
//            BlockChainLink link = storage.getLastLink();
//            if ( knownHighestBlock <= link.getHeight() )
//               return; // As far as we know we know everything, so no need to send anything
//            highestHashKnownBeforeRequest = link.getBlock().getHash();
//            downloading = true;
//            // Send the request
//            logger.debug("sending getblocks, we are at "+link.getHeight()+", while known max is: "+knownHighestBlock);
//            List<byte[]> startBlocks = new LinkedList<byte[]>();
//            startBlocks.add(highestHashKnownBeforeRequest);
//            GetBlocksMessage getBlocks = new GetBlocksMessage(bitcoinFactory.getMessageMagic(),BC_PROTOCOL_VERSION,startBlocks,null);
//            node.broadcast(getBlocks);
      }
    }
  }
}
