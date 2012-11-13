/**
 * Copyright (C) 2012 nibbles.it
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

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.SecondaryDatabase;
import com.sleepycat.je.SecondaryKeyCreator;
import hu.netmind.bitcoin.block.BitcoinFactory;

/**
 * Creates the secondary index for total difficulty. Used to get the top block
 * @author Alessandro Polverini
 */
//public class DifficultyIndexCreator extends TupleSecondaryKeyCreator<Difficulty,StoredLink>
public class DifficultyIndexCreator implements SecondaryKeyCreator
{
  private LongBinding keyBinding = new LongBinding();
  private BlockChainHeaderBinding dataBinding;

   public DifficultyIndexCreator(BitcoinFactory bitcoinFactory)
   {
      // super(new DifficultyBinding(bitcoinFactory),new LinkBinding(bitcoinFactory));
    dataBinding = new BlockChainHeaderBinding(bitcoinFactory);
   }

//  @Override
//   public Difficulty createSecondaryKey(StoredLink link)
//   {
//      return link.getLink().getTotalDifficulty();
//   }

  @Override
  public boolean createSecondaryKey(SecondaryDatabase sd, DatabaseEntry key, DatabaseEntry data, DatabaseEntry result) {
    long totalDifficulty = dataBinding.entryToObject(data).getTotalDifficulty().getDifficulty().longValue();
    keyBinding.objectToEntry(totalDifficulty, result);
    return true;
  }
}
