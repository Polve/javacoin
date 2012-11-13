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
package hu.netmind.bitcoin.block.bdb;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import java.util.List;
import java.util.Set;

/**
 * Creates a secondary index from a block using all his transaction hashes.
 *
 * @author Alessandro Polverini
 */
public class TxBlockIndexCreator implements SecondaryMultiKeyCreator {

  private BytesBinding keyBinding = new BytesBinding();
  private BlockTxBinding dataBinding = new BlockTxBinding();

//  public TxBlockIndexCreator(BitcoinFactory bitcoinFactory) {
//    dataBinding = new BlockChainHeaderBinding(bitcoinFactory);
//  }
//
//  @Override
//  public List<byte[]> createSecondaryKeys(StoredLink link) {
//    List<byte[]> resultKeys = new LinkedList<>();
//    for (Transaction tx : link.getLink().getBlock().getTransactions())
//      resultKeys.add(tx.getHash());
//    return resultKeys;
//  }
//
  @Override
  public void createSecondaryKeys(SecondaryDatabase sd, DatabaseEntry key, DatabaseEntry data, Set<DatabaseEntry> results) {
    List<byte[]> txHashes = dataBinding.entryToObject(data);
    for (byte[] hash : txHashes) {
      DatabaseEntry entry = new DatabaseEntry();
      keyBinding.objectToEntry(hash, entry);
      results.add(entry);
    }
  }
}
