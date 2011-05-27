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

/**
 * @author Robert Brautigam
 */
public interface TxIn
{
   /**
    * Get the hash of the referenced transaction to use.
    */
   byte[] getReferencedTxHash();

   /**
    * Get the specific output in the referenced transaction to
    * fully use in this transaction.
    */
   int getReferencedTxOutIndex();

   /**
    * Get the signature script for confirming the use of referenced output.
    */
   byte[] getSignatureScript();

   /**
    * Get the version number for replacement purposes.
    */
   int getTxVersion();
}

