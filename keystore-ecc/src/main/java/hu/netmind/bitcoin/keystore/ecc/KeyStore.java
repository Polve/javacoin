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

package hu.netmind.bitcoin.keystore.ecc;

import java.util.List;

/**
 * A storage mechanism for storing and retrieving keys.
 * @author Robert Brautigam
 */
public interface KeyStore
{
   /**
    * Add a key to the store.
    */
   void addKey(KeyImpl key);

   /**
    * Get the list of keys in the storage.
    */
   List<KeyImpl> getKeys();

   /**
    * Get the key which has the public hash given.
    * @param pubHash The publich hash of key.
    * @return The key with the given hash, or 
    */
   KeyImpl getKey(byte[] pubHash);
}

