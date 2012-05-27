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
package hu.netmind.bitcoin.chaintester;

import hu.netmind.bitcoin.block.jdbc.JdbcChainLinkStorage;
import hu.netmind.bitcoin.net.NodeAddress;
import hu.netmind.bitcoin.net.p2p.AddressSource;
import hu.netmind.bitcoin.net.p2p.source.RandomizedNodesSource;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 * This address source delivers the DNS based fallback nodes as listed in the
 * official client.
 *
 * @author Robert Brautigam
 */
public class StorageFallbackNodesSource extends RandomizedNodesSource
{

   private JdbcChainLinkStorage storage;
   private int maxSources = 100;
   private AddressSource fallbackSource;

   public StorageFallbackNodesSource(JdbcChainLinkStorage storage)
   {
      super();
      this.storage = storage;
   }

   public StorageFallbackNodesSource(JdbcChainLinkStorage storage, int defaultPort)
   {
      super(defaultPort);
      this.storage = storage;
   }

   public void setMaxSources(int maxSources)
   {
      this.maxSources = maxSources;
   }

   public void setFallbackSource(AddressSource fallbackSource)
   {
      this.fallbackSource = fallbackSource;
   }

   @Override
   public List<InetSocketAddress> getInitialAddresses()
   {
      List<InetSocketAddress> addresses = new LinkedList<>();
      List<NodeAddress> nodes = storage.loadNodeAddesses(maxSources);
      for (NodeAddress node : nodes)
         addresses.add(node.getAddress());
      logger.debug("Added {} nodes previously stored on db", addresses.size());
      if (fallbackSource != null)
         addresses.addAll(fallbackSource.getAddresses());
      return addresses;
   }
}
