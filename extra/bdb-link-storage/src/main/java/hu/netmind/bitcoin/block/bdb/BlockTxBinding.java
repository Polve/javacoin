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

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import static hu.netmind.bitcoin.block.bdb.BytesBinding.readBytes;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a one-to-many relationship between blocks and transactions
 *
 * @author Alessandro Polverini
 */
public class BlockTxBinding extends TupleBinding<List<byte[]>> {

  @Override
  public void objectToEntry(List<byte[]> txHashes, TupleOutput out) {
    out.writeInt(txHashes.size());
    for (byte[] hash : txHashes){
      out.write(hash);
    }
  }

  @Override
  public List<byte[]> entryToObject(TupleInput in) {
    int numTxs = in.readInt();
    List<byte[]> txHashes = new ArrayList<>(numTxs);
    for (int i=0; i<numTxs; i++)
      txHashes.add(readBytes(in, 32));
    return txHashes;
  }

}
