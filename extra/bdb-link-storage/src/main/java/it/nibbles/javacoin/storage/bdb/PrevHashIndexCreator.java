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
package it.nibbles.javacoin.storage.bdb;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import hu.netmind.bitcoin.block.BitcoinFactory;
import hu.netmind.bitcoin.block.BlockChainLink;

/**
 * Creates a secondary index from a link using the previous hash. This means the
 * index will hold the next links for a given hash.
 *
 * @author Alessandro Polverini
 */
//public class PrevHashIndexCreator extends TupleSecondaryKeyCreator<byte[],StoredLink>
public class PrevHashIndexCreator implements SecondaryKeyCreator {

  private BytesBinding keyBinding = new BytesBinding();
  private BlockChainHeaderBinding dataBinding;

  public PrevHashIndexCreator(BitcoinFactory bitcoinFactory) {
    dataBinding = new BlockChainHeaderBinding(bitcoinFactory);
  }

  @Override
  public boolean createSecondaryKey(SecondaryDatabase sd, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) {
    BlockChainLink storedBlockHeader = dataBinding.entryToObject(data);
    if (storedBlockHeader.getHeight() == 1)
      return false;
    keyBinding.objectToEntry(storedBlockHeader.getBlock().getPreviousBlockHash(), result);
    return true;
  }
}
