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
import it.nibbles.javacoin.block.BitcoinFactory;
import it.nibbles.javacoin.block.Difficulty;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Robert Brautigam
 */
public class DifficultyBinding extends TupleBinding<Difficulty> {

  private BitcoinFactory factory;

  public DifficultyBinding(BitcoinFactory factory) {
    this.factory = factory;
  }

  @Override
  public Difficulty entryToObject(TupleInput in) {
//    int unscaledBytesLength = in.readInt();
//    byte[] unscaledBytes = BytesBinding.readBytes(in, unscaledBytesLength);
//    int scale = in.readInt();
//    return factory.newDifficulty(new BigDecimal(new BigInteger(unscaledBytes), scale));
    return factory.newDifficulty(new BigDecimal(in.readLong()));
  }

  @Override
  public void objectToEntry(Difficulty difficulty, TupleOutput out) {
//    byte[] unscaledBytes = difficulty.getDifficulty().unscaledValue().toByteArray();
//    int scale = difficulty.getDifficulty().scale();
//    out.writeInt(unscaledBytes.length);
//    out.write(unscaledBytes);
//    out.writeInt(scale);
    out.writeLong(difficulty.getDifficulty().longValue());
  }
}
