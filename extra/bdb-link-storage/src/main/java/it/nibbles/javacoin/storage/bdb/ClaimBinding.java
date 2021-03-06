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
package it.nibbles.javacoin.storage.bdb;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * @author Robert Brautigam
 */
public class ClaimBinding extends TupleBinding<Claim> {

  @Override
  public Claim entryToObject(TupleInput in) {
    return new Claim(BytesBinding.readBytes(in, 32), in.readInt());
  }

  @Override
  public void objectToEntry(Claim claim, TupleOutput out) {
    out.write(claim.getClaimedTransactionHash());
    out.writeInt(claim.getClaimedOutputIndex());
  }
}
