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

package hu.netmind.bitcoin.node.p2p.source;

import hu.netmind.bitcoin.node.p2p.AddressSource;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Collections;
import java.net.InetSocketAddress;

/**
 * This address source delivers the static fallback nodes listed on the
 * official wiki. Note that this may be out of date.
 * @author Robert Brautigam
 */
public class FallbackNodesSource implements AddressSource
{
   private List<InetSocketAddress> addresses;

   public FallbackNodesSource()
   {
      // Read addresses from properties file
      addresses = new ArrayList<InetSocketAddress>();
      ResourceBundle bundle = ResourceBundle.getBundle("fallback-nodes");
      Enumeration<String> keys = bundle.getKeys();
      while ( keys.hasMoreElements() )
         addresses.add(new InetSocketAddress(bundle.getString(keys.nextElement()),8333));
      // Randomize
      Collections.shuffle(addresses);
   }

   public List<InetSocketAddress> getAddresses()
   {
      return addresses;
   }
}

