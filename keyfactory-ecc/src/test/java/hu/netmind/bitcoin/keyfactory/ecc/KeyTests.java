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

import org.testng.Assert;
import org.testng.annotations.Test;
import org.easymock.EasyMock;
import hu.netmind.bitcoin.*;

/**
 * @author Robert Brautigam
 */
@Test
public class KeyTests
{
   public void testCreateCallsStore()
   {
      // Create mock
      KeyStore store = EasyMock.createMock(KeyStore.class);
      store.addKey((KeyImpl) EasyMock.anyObject());
      EasyMock.replay(store);
      // Call create
      KeyFactoryImpl factory = new KeyFactoryImpl(store,Key.Type.MAIN);
      factory.createKey();
      // Check
      EasyMock.verify(store);
   }

   public void testCreateKeyAttibutes()
   {
      long beforeTime = System.currentTimeMillis();
      // Create factory
      KeyStore store = EasyMock.createMock(KeyStore.class);
      KeyFactoryImpl factory = new KeyFactoryImpl(store,Key.Type.MAIN);
      // Create key
      Key key = factory.createKey();
      // Check attributes
      Assert.assertEquals(key.getType(),Key.Type.MAIN);
      Assert.assertTrue(key.getCreationTime() > beforeTime,"key creation time was in the past");
   }

   public void testPublicKeyHashing()
   {
      // Create factory
      KeyStore store = EasyMock.createMock(KeyStore.class);
      KeyFactoryImpl factory = new KeyFactoryImpl(store,Key.Type.MAIN);
      // Create key
      Key key = factory.createKey();
      // Get the public hash 
      byte[] pubHash = key.getPublicKey().getHash();
      // Now create a public key from the public key from public key data only
      PublicKey publicKey = factory.createPublicKey(
            ((KeyImpl.PublicKeyImpl)key.getPublicKey()).getPublicKey());
      // Now this public key should now have the same hash value
      Assert.assertEquals(publicKey.getHash(),pubHash);
   }

   public void testCryptoConcept()
      throws VerificationException
   {
      byte[] data = new byte[] { 1,2,3,4,5,6,7,8,9,-1,-2,-3,-4,-5,-6,-7,-8,-9 };
      // Create factory
      KeyStore store = EasyMock.createMock(KeyStore.class);
      KeyFactoryImpl factory = new KeyFactoryImpl(store,Key.Type.MAIN);
      // Create key
      Key key = factory.createKey();
      // Sign data
      byte[] signature = key.sign(data);
      // Now verify using the public key
      Assert.assertTrue(key.getPublicKey().verify(data,signature),"signature didn't verify correctly");
   }
}

