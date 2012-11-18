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

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import hu.netmind.bitcoin.block.BitcoinFactory;

/**
 * Creates the secondary index for the height of the block
 *
 * @author Alessandro Polverini
 */
//public class HeightIndexCreator extends TupleSecondaryKeyCreator<Long,StoredLink>
public class HeightIndexCreator implements SecondaryKeyCreator {

  private IntegerBinding keyBinding = new IntegerBinding();
  private BlockChainHeaderBinding dataBinding;

  public HeightIndexCreator(BitcoinFactory bitcoinFactory) {
//      super(new LongBinding(),new LinkBinding(bitcoinFactory));
    dataBinding = new BlockChainHeaderBinding(bitcoinFactory);
  }

  @Override
  public boolean createSecondaryKey(SecondaryDatabase sd, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) {
    int height = dataBinding.entryToObject(data).getHeight();
    keyBinding.objectToEntry(height, result);
    return true;
  }
}
