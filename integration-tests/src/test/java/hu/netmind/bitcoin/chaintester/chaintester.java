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
import hu.netmind.bitcoin.node.p2p.source.FallbackNodesSource;
import hu.netmind.bitcoin.node.p2p.Node;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import hu.netmind.bitcoin.BlockChain;
import hu.netmind.bitcoin.BitCoinException;
import hu.netmind.bitcoin.block.BlockChainImpl;
import hu.netmind.bitcoin.block.BlockImpl;
import hu.netmind.bitcoin.script.ScriptFactoryImpl;
import hu.netmind.bitcoin.keyfactory.ecc.KeyFactoryImpl;

/**
 * This application downloads all th blocks available on the 
 * BitCoin network and adds them to the chain.
 * @author Robert Brautigam
 */
public class chaintester
{
   private static Logger logger = LoggerFactory.getLogger(chaintester.class);

   private Node node = null;
   private BlockChain chain = null;

   public static void main(String[] argv)
      throws Exception
   {
      chaintester app = new chaintester();
      app.init();
      app.run();
   }

   /**
    * Initialize and bind components together.
    */
   public void init()
      throws BitCoinException
   {
      logger.debug("initializing...");
      // Initialize the chain
      chain = new BlockChainImpl(BlockImpl.MAIN_GENESIS,
            new SimpleSqlStorage("data"),
            new ScriptFactoryImpl(new KeyFactoryImpl(null)),
            false);
      // Initialize p2p node
      node = new Node();
      node.setMinConnections(2);
      node.setMaxConnections(10);
      node.setAddressSource(new FallbackNodesSource());
//      node.addHandler(new DownloaderHandler());
   }

   /**
    * Run the client and listen for new blocks forever.
    */
   public void run()
   {
      logger.debug("starting...");
   }
}

