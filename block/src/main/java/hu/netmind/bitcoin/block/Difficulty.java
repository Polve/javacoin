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

package hu.netmind.bitcoin.block;

import java.math.BigInteger;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This object holds the difficulty value for a block, or the
 * sum of difficulties for multiple blocks.
 * There are three relevant terms and values regarding difficulty
 * in blocks and throughout the bitcoin documentation:
 * <ul>
 *    <li>A "target" is a target difficulty setting for hashes. It is
 *    a 256 bit number which defines the maximum of the hash (the hash
 *    has to be lower than the specified number).
 *    <li>The "difficulty" is defined as maximum_target / target. So it is
 *    1 for the easiest setting, and gets bigger as the difficulty grows.
 *    Difficulty is additive, if a value is twice another value it is
 *    considered twice as difficult to generate (needs twice processor power).</li>
 *    <li>"Compressed target" is a 32 bit value representing a compressed
 *    form of a target.</li>
 * </ul>
 * @author Robert Brautigam
 */
public class Difficulty implements Comparable<Difficulty>
{
   private static Logger logger = LoggerFactory.getLogger(Difficulty.class);
   private static final BigInteger MAX_TARGET =
         new BigInteger("FFFF0000000000000000000000000000000000000000000000000000",16);
   private BigDecimal difficulty;

   /**
    * Construct the difficulty object with the target number.
    */
   public Difficulty(byte[] target)
   {
      BigDecimal targetNumber = new BigDecimal(new BigInteger(1,target));
      if ( targetNumber.equals(BigDecimal.ZERO) )
         difficulty = BigDecimal.ZERO;
      else
         difficulty = new BigDecimal(MAX_TARGET).divide(
               targetNumber,BigDecimal.ROUND_DOWN);
      logger.debug("difficulty created with value: {}",difficulty);
   }

   /**
    * Construct the difficulty with the compressed target number.
    */
   public Difficulty(long compressedTarget)
   {
      this(
         BigInteger.valueOf(compressedTarget & 0x00FFFFFFl).
         shiftLeft(8*((int)(compressedTarget >> 24)-3)).
         toByteArray());
   }

   /**
    * Compare one difficulty with another.
    * @return Positive if this difficulty is greater than the parameter,
    * 0 if equal, or negative if it is less difficulty that the difficulty given.
    */
   public int compareTo(Difficulty other)
   {
      return difficulty.subtract(other.difficulty).signum();
   }

   /**
    * Add another difficulty to this one. The total difficulty represented
    * will be the difficulty required to hit both targets after eachother.
    */
   public void add(Difficulty other)
   {
      difficulty = difficulty.add(other.difficulty);
   }

   /**
    * Get the difficulty vaule.
    */
   public BigDecimal getDifficulty()
   {
      return difficulty;
   }

   public String toString()
   {
      return difficulty.toString();
   }
}



