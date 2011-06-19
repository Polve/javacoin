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

import java.util.List;

/**
 * The key factory can be used to create different kinds of keys, but
 * it also can store/remember all keys that were created.
 * The private
 * keys are the evidence which transactions belong to the owner, so losing
 * the keys amount to losing all the money associated with the key. 
 */
public interface KeyFactory extends Observable
{
   enum Event
   {
      KEY_ADDED,
      KEY_REMOVED,
   };

   /**
    * Create a completely new key that was not used before, and
    * return it as well as store it.
    * @return An unused key.
    */
   Key createKey();

   /**
    * Create a public key from an already existing representation.
    * This is a public key, and it won't be stored by this factory.
    * @param data The representation of the public key. The format of
    * the data is decided by the type of factory.
    */
   PublicKey createKey(byte[] data);

   /**
    * Get the list of all private keys stored in this factory.
    * @return The list of all keys stored.
    */
   List<Key> getKeys();

   /**
    * Get a specific key that has the given hash.
    * @param hash The hash of the key to get.
    * @return The key if found, else null.
    */
   Key getKey(byte[] hash);

}
