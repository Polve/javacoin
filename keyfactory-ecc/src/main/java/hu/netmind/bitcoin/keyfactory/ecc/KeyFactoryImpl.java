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

package hu.netmind.bitcoin.keyfactory.ecc;

import hu.netmind.bitcoin.KeyFactory;
import hu.netmind.bitcoin.Key;
import hu.netmind.bitcoin.PublicKey;
import java.util.Observable;
import java.util.List;
import java.util.Collections;

/**
 * A keystore to store EC keys. The keyStore mechanism is pluggable.
 * @author Robert Brautigam
 */
public class KeyFactoryImpl extends Observable implements KeyFactory
{
   private KeyStore keyStore;
   private Key.Type keyType;

   /**
    * Create this keystore with the storage implementation given.
    * @param keyStore The keyStore mechanism to use.
    * @param keyType The type of keys to create, wether for live network or for test network.
    */
   public KeyFactoryImpl(KeyStore keyStore, Key.Type keyType)
   {
      this.keyStore=keyStore;
      this.keyType=keyType;
   }

   /**
    * Generate a new key and store it in the given keyStore.
    */
   public Key createKey()
   {
      KeyImpl key = new KeyImpl(keyType);
      keyStore.addKey(key);
      setChanged();
      notifyObservers(Event.KEY_ADDED); 
      return key;
   }

   /**
    * Create a public key from an already existing representation.
    * This is a public key, and it won't be stored by this factory.
    * @param data The representation of the public key in EC public point.
    */
   public PublicKey createPublicKey(byte[] data)
   {
      return new KeyImpl.PublicKeyImpl(data);
   }

   /**
    * Get the list off all keys in this store.
    */
   public List<Key> getKeys()
   {
      return Collections.unmodifiableList((List<? extends Key>)keyStore.getKeys());
   }

   /**
    * Get the key which has the public hash given.
    * @param pubHash The publich hash of key.
    * @return The key with the given hash, or 
    */
   public Key getKey(byte[] pubHash)
   {
      return keyStore.getKey(pubHash);
   }
}

