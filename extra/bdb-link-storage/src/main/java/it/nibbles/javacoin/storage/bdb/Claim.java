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

package it.nibbles.javacoin.storage.bdb;

/**
 * Represents one claim on a given output.
 * @author Robert Brautigam
 */
public class Claim
{
   private byte[] claimedTransactionHash;
   private int claimedOutputIndex;

   public Claim(byte[] claimedTransactionHash, int claimedOutputIndex)
   {
      this.claimedTransactionHash=claimedTransactionHash;
      this.claimedOutputIndex=claimedOutputIndex;
   }

   public byte[] getClaimedTransactionHash()
   {
      return claimedTransactionHash;
   }
   public int getClaimedOutputIndex()
   {
      return claimedOutputIndex;
   }

}

