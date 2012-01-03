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
 * The public key part of a created key pair.
 * @author Robert Brautigam
 */
public interface PublicKey
{
   /**
    * Return the hash of this key.
    */
   byte[] getHash();

   /**
    * Verify that a block of data was signed with the private counterpart
    * of this public key.
    * @param data The data that was supposed to be signed.
    * @param signature The signature for that data.
    * @return True if the signature is a correct signature of the given data
    * for the private key counterpart of this public key, false otherwise.
    * @throws VerificationException If the signature is in the wrong format, or
    * other algorithmical or data problems arise. Note: if the signature is merely not
    * correct, this exception is not thrown, only false is returned.
    */
   boolean verify(byte[] data, byte[] signature)
      throws VerificationException;

}

