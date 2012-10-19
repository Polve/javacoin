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

package hu.netmind.bitcoin.block.bdb;

import hu.netmind.bitcoin.block.Difficulty;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import hu.netmind.bitcoin.block.BitcoinFactory;
import java.io.Serializable;
import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * @author Robert Brautigam
 */
public class DifficultyBinding extends TupleBinding<Difficulty>
{
  private BitcoinFactory factory;

  public DifficultyBinding(BitcoinFactory factory) {
    this.factory = factory;
  }
  
  @Override
   public Difficulty entryToObject(TupleInput in)
   {
      int unscaledBytesLength = in.readInt();
      byte[] unscaledBytes = BytesBinding.readBytes(in,unscaledBytesLength);
      int scale = in.readInt();
      return factory.newDifficulty(new BigDecimal(new BigInteger(unscaledBytes),scale));
   }

  @Override
   public void objectToEntry(Difficulty difficulty, TupleOutput out)
   {
      byte[] unscaledBytes = difficulty.getDifficulty().unscaledValue().toByteArray();
      int scale = difficulty.getDifficulty().scale();
      out.writeInt(unscaledBytes.length);
      out.write(unscaledBytes);
      out.writeInt(scale);
   }
}

