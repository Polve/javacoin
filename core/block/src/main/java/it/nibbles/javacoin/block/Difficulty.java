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
package it.nibbles.javacoin.block;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object holds the difficulty value for a block, or the sum of
 * difficulties for multiple blocks. There are three relevant terms and values
 * regarding difficulty in blocks and throughout the bitcoin documentation: <ul>
 * <li>A "target" is a target difficulty setting for hashes. It is a 256 bit
 * number which defines the maximum of the hash (the hash has to be lower than
 * the specified number). <li>The "difficulty" is defined as maximum_target /
 * target. So it is 1 for the easiest setting, and gets bigger as the difficulty
 * grows. Difficulty is additive, if a value is twice another value it is
 * considered twice as difficult to generate (needs twice processor power).</li>
 * <li>"Compressed target" is a 32 bit value representing a compressed form of a
 * target.</li> </ul>
 *
 * @author Robert Brautigam, Alessandro Polverini
 */
public class Difficulty implements Comparable<Difficulty>, Serializable
{

   private static Logger logger = LoggerFactory.getLogger(Difficulty.class);
   private Difficulty MIN_VALUE;
   private BigDecimal difficulty;
   private DifficultyTarget maxTarget;

   public Difficulty(BigDecimal difficulty, DifficultyTarget maxTarget)
   {
      this.difficulty = difficulty;
      this.maxTarget = maxTarget;
   }

   public Difficulty(DifficultyTarget target, DifficultyTarget maxTarget)
   {
      if (target.getTarget().equals(BigInteger.ZERO))
         difficulty = BigDecimal.ZERO;
      else
         difficulty = new BigDecimal(maxTarget.getTarget()).divide(new BigDecimal(target.getTarget()), BigDecimal.ROUND_DOWN);
      logger.debug("difficulty created with value: {}", difficulty);
   }

   /**
    * Compare one difficulty with another.
    *
    * @return Positive if this difficulty is greater than the parameter, 0 if
    * equal, or negative if it is less difficulty that the difficulty given.
    */
   public int compareTo(Difficulty other)
   {
      return difficulty.subtract(other.difficulty).signum();
   }

   /**
    * Add another difficulty to this one. The total difficulty represented will
    * be the difficulty required to hit both targets after eachother.
    */
   public Difficulty add(Difficulty other)
   {
      return new Difficulty(difficulty.add(other.difficulty), maxTarget);
   }

   /**
    * Get the difficulty value.
    */
   public BigDecimal getDifficulty()
   {
      return difficulty;
   }

   public Difficulty getMinValue()
   {
      if (MIN_VALUE == null)
         MIN_VALUE = new Difficulty(maxTarget, maxTarget);
      return MIN_VALUE;
   }

   @Override
   public String toString()
   {
      return difficulty.toString();
   }
}
