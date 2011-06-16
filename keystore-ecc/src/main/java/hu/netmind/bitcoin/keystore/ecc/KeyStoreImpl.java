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

import hu.netmind.bitcoin.KeyStore;
import hu.netmind.bitcoin.Key;
import java.util.Observable;

/**
 * A keystore to store EC keys. The storage mechanism is pluggable.
 * @author Robert Brautigam
 */
public class KeyStoreImpl implements KeyStore extends Observable
{
   private Storage storage;
   private Key.Type keyType;

   /**
    * Create this keystore with the storage implementation given.
    * @param storage The storage mechanism to use.
    * @param keyType The type of keys to create, wither for live network or for test network.
    */
   public KeyStoreImpl(Storage storage, Key.Type keyType)
   {
      this.storage=storage;
      this.keyType=keyType;
   }

   /**
    * Generate a new key and store it in the given storage.
    */
   public Key createKey()
   {
      KeyImpl key = new KeyImpl();
   }
}

