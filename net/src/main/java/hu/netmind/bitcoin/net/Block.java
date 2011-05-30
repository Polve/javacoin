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

package hu.netmind.bitcoin.net;

import java.util.List;

/**
 * @author Robert Brautigam
 */
public interface Block extends ChecksummedMessage
{
   /**
    * Get the block's version information.
    */
   int getVersion();

   /**
    * Get the hash of the previous block.
    */
   byte[] getPrevBlock();

   /**
    * Get the Merkle root hash for all transactions.
    */
   byte[] getRootHash();

   /**
    * Get the timestamp when this block was created.
    */
   long getTimestamp();

   /**
    * Get the difficulty target setting for this block in bits.
    */
   long getDifficulty();

   /**
    * Get the "nonce" value. This is just a "random" value to 
    * get the hash under the specified difficulty level.
    */
   long getNonce();

   /**
    * Get the transactions in this block.
    * @return The list of transactions. This may be and empty list in
    * which case there are no transacations in this block, or it could be
    * null, in which case this information was just left out (probably a
    * header-only message).
    */
   List<Tx> getTransactions();
}

