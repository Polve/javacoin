/**
 * Copyright (C) 2012 NetMind Consulting Bt.
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

package hu.netmind.bitcoin.block.bdb;

import hu.netmind.bitcoin.block.BitcoinFactory;

/**
 * Creates a secondary index from a link using the previous hash. This means
 * the index will hold the next links for a given hash.
 * @author Robert Brautigam
 */
public class NextHashIndexCreator extends TupleSecondaryKeyCreator<byte[],StoredLink>
{
   public NextHashIndexCreator(BitcoinFactory bitcoinFactory)
   {
      super(new BytesBinding(),new LinkBinding(bitcoinFactory));
   }

  @Override
   public byte[] createSecondaryKey(StoredLink link)
   {
      return link.getLink().getBlock().getPreviousBlockHash();
   }
}

