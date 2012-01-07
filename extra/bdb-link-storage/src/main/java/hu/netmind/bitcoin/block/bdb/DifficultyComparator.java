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

import hu.netmind.bitcoin.block.Difficulty;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import java.util.Comparator;

/**
 * Compares difficulties for a correct sorting order of the relevant index.
 * @author Robert Brautigam
 */
public class DifficultyComparator implements Comparator<byte[]>
{
   private DifficultyBinding binding = new DifficultyBinding();

   public int compare(byte[] b1, byte[] b2)
   {
      Difficulty d1 = binding.entryToObject(new TupleInput(b1));
      Difficulty d2 = binding.entryToObject(new TupleInput(b2));
      return d1.compareTo(d2);
   }
}

