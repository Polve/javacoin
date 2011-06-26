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

package hu.netmind.bitcoin;

/**
 * A key capable of being used in BitCoin transactions.
 * @author Robert Brautigam
 */
public interface Key
{
   enum Type
   {
      MAIN,
      TEST
   };

   /**
    * Get the type of key. This indicates which network this key
    * is supposed to be used in.
    */
   Type getType();

   /**
    * Sign a block of data with this private key.
    * @param data The data to sign.
    * @return The signature of the data.
    * @throws VerificationException If the generation of signature fails.
    */
   byte[] sign(byte[] data)
      throws VerificationException;

   /**
    * Get the public key of this private key.
    */
   PublicKey getPublicKey();

   /**
    * Get the time this key was created. This time should be
    * the earliest time when the key might have been used the first
    * time.
    */
   long getCreationTime();
}
