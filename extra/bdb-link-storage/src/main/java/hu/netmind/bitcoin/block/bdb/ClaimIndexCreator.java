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

import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import com.sleepycat.je.SecondaryMultiKeyCreator;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.block.BitcoinFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Creates a secondary index for a link using all the transaction inputs in the
 * link.
 *
 * @author Robert Brautigam
 */
//public class ClaimIndexCreator implements SecondaryMultiKeyCreator {
public class ClaimIndexCreator extends TupleMultiSecondaryKeyCreator<Claim, StoredLink> {
//  private ClaimBinding keyBinding = new ClaimBinding();
//  private BlockChainHeaderBinding dataBinding;

  public ClaimIndexCreator(BitcoinFactory bitcoinFactory) {
    super(new ClaimBinding(), new LinkBinding(bitcoinFactory));
  }

  @Override
  public List<Claim> createSecondaryKeys(StoredLink link) {
    List<Claim> resultKeys = new LinkedList<>();
    for (Transaction tx : link.getLink().getBlock().getTransactions())
      for (TransactionInput in : tx.getInputs())
        resultKeys.add(new Claim(in.getClaimedTransactionHash(), in.getClaimedOutputIndex()));
    return resultKeys;
  }
//  @Override
//  public void createSecondaryKeys(SecondaryDatabase sd, DatabaseEntry key, DatabaseEntry data, Set<DatabaseEntry> results) {
//    for (Transaction tx : dataBinding.entryToObject(data).getBlock().getTransactions())
//      for (TransactionInput in : tx.getInputs()) {
//        DatabaseEntry entry = new DatabaseEntry();
//        keyBinding.objectToEntry(new Claim(in.getClaimedTransactionHash(), in.getClaimedOutputIndex()), entry);
//        results.add(entry);
//      }
//  }
}
