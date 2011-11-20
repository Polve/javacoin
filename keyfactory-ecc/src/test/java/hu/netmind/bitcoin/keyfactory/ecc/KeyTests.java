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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

/**
 * @author Robert Brautigam
 */
@Test
public class KeyTests
{
   private static Logger logger = LoggerFactory.getLogger(KeyTests.class);

   public void testCreateCallsStore()
   {
      // Create mock
      KeyStore store = EasyMock.createMock(KeyStore.class);
      store.addKey((KeyImpl) EasyMock.anyObject());
      EasyMock.replay(store);
      // Call create
      KeyFactoryImpl factory = new KeyFactoryImpl(store);
      factory.createKey();
      // Check
      EasyMock.verify(store);
   }

   public void testCreateKeyAttibutes()
   {
      long beforeTime = System.currentTimeMillis();
      // Create factory
      KeyStore store = EasyMock.createMock(KeyStore.class);
      KeyFactoryImpl factory = new KeyFactoryImpl(store);
      // Create key
      Key key = factory.createKey();
      // Check attributes
      Assert.assertTrue(key.getCreationTime() > beforeTime,"key creation time was in the past");
   }

   public void testPublicKeyHashing()
   {
      // Create factory
      KeyStore store = EasyMock.createMock(KeyStore.class);
      KeyFactoryImpl factory = new KeyFactoryImpl(store);
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
      KeyFactoryImpl factory = new KeyFactoryImpl(store);
      // Create key
      Key key = factory.createKey();
      // Print values to debug
      byte[] publicKey = ((KeyImpl.PublicKeyImpl) key.getPublicKey()).getPublicKey();
      byte[] publicKeyHash =  key.getPublicKey().getHash();
      logger.debug("key public key: "+Arrays.toString(publicKey)+" ("+(publicKey.length)+" bytes)");
      logger.debug("key public key hash: "+Arrays.toString(publicKeyHash)+" ("+(publicKeyHash.length)+" bytes)");
      // Sign data
      byte[] signature = key.sign(data);
      // Now verify using the public key
      Assert.assertTrue(key.getPublicKey().verify(data,signature),"signature didn't verify correctly");
   }

   public void testVerificationOnRealData()
      throws VerificationException
   {
      // Getting sample data from forum entry:
      // http://forum.bitcoin.org/index.php?topic=2957.0
      //byte[] data = HexUtil.toByteArray("90 37 75 25 E0 5B D7 1C E8 BA 41 3A 84 FD AE A2 99 76 67 32 F1 65 FA B2 8A 69 D3 0C 83 33 7F 9B");
      byte[] data = HexUtil.toByteArray("E8 A8 75 B4 A6 B2 3E 50 7C DA D5 6D 1D 74 28 5F 22 FE C0 5B FD 6B E2 F7 37 92 3C 43 FC C2 39 87");
      byte[] signature = HexUtil.toByteArray(
            "30 46 02 21 00 F5 74 6B 0B 25 4F 5A 37 E7 52 "+
            "51 45 9C 7A 23 B6 DF CB 86 8A C7 46 7E DD 9A 6F "+
            "DD 1D 96 98 71 BE 02 21 00 88 94 8A EA 29 B6 91 "+
            "61 CA 34 1C 49 C0 26 86 A8 1D 8C BB 73 94 0F 91 "+
            "7F A0 ED 71 54 68 6D 3E 5B");
      byte[] publicKey = HexUtil.toByteArray(
            "04 47 D4 90 56 "+
            "1F 39 6C 8A 9E FC 14 48 6B C1 98 88 4B A1 83 79 "+
            "BC AC 2E 0B E2 D8 52 51 34 AB 74 2F 30 1A 9A CA "+
            "36 60 6E 5D 29 AA 23 8A 9E 29 93 00 31 50 42 3D "+
            "F6 92 45 63 64 2D 4A FE 9B F4 FE 28");
      // Now check that this signature is valid
      KeyStore store = EasyMock.createMock(KeyStore.class);
      KeyFactoryImpl factory = new KeyFactoryImpl(store);
      PublicKey key = factory.createPublicKey(publicKey);
      Assert.assertTrue(key.verify(data,signature),"couldn't verify real live data");
   }
}

