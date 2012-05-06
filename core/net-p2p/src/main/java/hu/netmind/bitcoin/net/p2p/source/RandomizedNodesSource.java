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
package hu.netmind.bitcoin.net.p2p.source;

import hu.netmind.bitcoin.net.p2p.AddressSource;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract nodes source takes a prepared list of addresses, and each time
 * asked provides a shuffled version of the list thereby making sure that nodes
 * are used randomly.
 *
 * @author Robert Brautigam, Alessandro Polverini
 */
public abstract class RandomizedNodesSource implements AddressSource
{

   protected Logger logger = LoggerFactory.getLogger(this.getClass());
   protected int defaultPort = 8333;
   private List<InetSocketAddress> addresses;

   public RandomizedNodesSource()
   {
   }

   public RandomizedNodesSource(int defaultPort)
   {
      this.defaultPort = defaultPort;
   }

   /**
    * Implement this method to collect the initial list of addresses. This will
    * be called only once when the source is initializing.
    */
   protected abstract List<InetSocketAddress> getInitialAddresses();

   /**
    * Implement address source method to always shuffle list before giving it
    * back. The list returned is unmodifiable.
    */
   public List<InetSocketAddress> getAddresses()
   {
      if (addresses == null)
         addresses = getInitialAddresses();
      Collections.shuffle(addresses);
      return Collections.unmodifiableList(addresses);
   }

   @Override
   public String toString()
   {
      List<InetSocketAddress> list = getAddresses();
      if (list == null)
         return "AddressSource[null]";
      StringBuilder sb = new StringBuilder("AddressSource[");
      for (InetSocketAddress addr : list)
         sb.append(addr.toString()).append(" ");
      return sb.append("]").toString();
   }
}
