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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Represents an actual target value for a hash. See <code>Difficulty</code>
 * documentation for details.
 * @author Robert Brautigam, Alessandro Polverini
 */
public class DifficultyTarget implements Comparable<DifficultyTarget>
{
   private BigInteger target;
   private long compressedTarget;

   /**
    * Construct target with the byte representation.
    */
   public DifficultyTarget(byte[] targetBytes)
   {
      this(new BigInteger(1,targetBytes));
   }

   /**
    * Construct a target directly with the exact number.
    */
   public DifficultyTarget(BigInteger target)
   {
      if ( target.signum() < 0 )
         throw new IllegalArgumentException("difficulty target can not be negative");
      this.target=target;
      int shiftValue = target.bitLength()/8+1;
      compressedTarget = (((long)shiftValue)<<24) |
         target.shiftRight(8*(shiftValue-3)).longValue();
   }

   /**
    * Construct target with the compressed representation of a target.
    */
   public DifficultyTarget(long compressedTarget)
   {
      if ( (compressedTarget > 0x21000000l) || (compressedTarget<0) )
         throw new IllegalArgumentException("compressed target out of valid range");
      this.compressedTarget=compressedTarget;
      target = 
         BigInteger.valueOf(compressedTarget & 0x007FFFFFl).
         shiftLeft(8*((int)(compressedTarget >> 24)-3));
   }

   /**
    * Get the target number.
    */
   public BigInteger getTarget()
   {
      return target;
   }

   /**
    * Get the compressed target.
    */
   public long getCompressedTarget()
   {
      return compressedTarget;
   }

   /**
    * Compare one difficulty target with another.
    * @return The result of comparing the two difficulty numbers.
    */
   public int compareTo(DifficultyTarget other)
   {
      return target.compareTo(other.target);
   }

   @Override
   public String toString()
   {
      return "0x"+target.toString(16) +" (0x"+new BigInteger(""+compressedTarget).toString(16) +")";
   }
}

