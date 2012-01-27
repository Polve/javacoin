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
import org.testng.annotations.DataProvider;
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

   @DataProvider(name="verificationData")
   public Object[][] getVerificationData()
   {
      return new Object[][] {
         { // From discussion https://bitcointalk.org/index.php?topic=2957.0
            "87 39 C2 FC 43 3C 92 37 F7 E2 6B FD 5B C0 FE 22 5F 28 74 1D 6D D5 DA 7C 50 3E B2 A6 B4 75 A8 E8 ",
            "30 46 02 21 00 F5 74 6B 0B 25 4F 5A 37 E7 52 51 45 9C 7A 23 B6 DF CB 86 8A C7 46 7E DD 9A 6F DD 1D 96 98 71 BE 02 21 00 88 94 8A EA 29 B6 91 61 CA 34 1C 49 C0 26 86 A8 1D 8C BB 73 94 0F 91 7F A0 ED 71 54 68 6D 3E 5B ",
            "04 47 D4 90 56 1F 39 6C 8A 9E FC 14 48 6B C1 98 88 4B A1 83 79 BC AC 2E 0B E2 D8 52 51 34 AB 74 2F 30 1A 9A CA 36 60 6E 5D 29 AA 23 8A 9E 29 93 00 31 50 42 3D F6 92 45 63 64 2D 4A FE 9B F4 FE 28 ",
         },
         { // From block 140493 (this is somehow different?)
            "B4 5C 68 0F 32 F9 36 4F 52 55 CC 15 EF 7C AD 87 9D BD E9 06 2D 7F B8 DB 0F E5 6E 24 58 23 A7 8F ",
            "30 44 02 20 6B 5C 3B 1C 86 74 8D CF 32 8B 9F 3A 65 E1 00 85 AF CF 5D 1A F5 B4 09 70 D8 CE 3A 93 55 E0 6B 5B 02 20 CD BD C2 3E 6D 36 18 E4 70 56 FC CC 60 C5 F7 3D 1A 54 21 86 70 51 97 E5 79 1E 97 F0 E6 58 2A 32 ",
            "04 F2 5E C4 95 FA 21 AD 14 D6 9F 45 BF 27 71 29 48 8C FB 1A 33 9A BA 1F ED 3C 50 99 BB 6D 8E 97 16 49 1A 14 05 0F BC 0B 2F ED 29 63 DC 1E 56 26 4B 3A DF 52 A8 1B 95 32 22 A2 18 0D 48 B5 4D 1E 18 ",
         },
         { // First tx from block 140000
            "57 5D E2 E4 50 37 D6 F4 53 A0 A0 E9 0E 72 01 FF 2B 35 64 55 A0 1F 29 E2 60 79 18 3E 53 64 F2 F8 ",
            "30 45 02 21 00 C2 8D FB 4E 7E 31 E7 34 A1 0F 22 8B E6 4F 19 5E 53 73 C7 24 2E 71 C8 5D 45 8F C5 67 5C 71 A2 17 02 20 1A 4B 19 C4 40 97 54 41 25 B7 AE BB EE B2 A7 C2 64 34 68 44 70 F4 58 41 F1 D7 43 49 91 CF 4E 8D ",
            "04 6D 0B D0 1A E8 3C BC BD 34 5B 16 B3 CA F8 07 4B 7E 53 3B F5 C7 3F D0 9D B0 B4 DC CC 11 29 E9 77 35 F6 B4 CE C2 78 B0 8A F4 6B 88 F5 12 6B 3C B5 F1 2E 42 10 AC 6E DB 38 49 D8 4C BE 13 34 A2 7E ",
         },
         { // First tx from block 140492
            "D1 88 BC 85 2C 47 F5 D3 72 FB E0 1A 79 79 83 8C 02 19 D2 F0 70 4D F2 78 87 01 F7 F2 75 CF C8 35 ",
            "30 44 02 20 6B 65 3E 26 2D DD 3B B7 B5 BE 79 F7 42 EF 00 20 D5 00 C8 B7 68 C6 34 FF E7 BC 0A C9 86 B0 ED A7 02 20 1A 72 E4 FC A9 C0 A9 17 07 A5 2C 4A E3 F4 E4 10 0E AE 0B B9 9A BB 2C 76 B9 7F F4 44 14 B9 89 B6 ",
            "04 7F 43 8D 26 57 AE 30 0F 90 D5 EA 4C 32 C5 09 7D 41 B6 50 B7 A3 A9 E6 BD 38 EC B5 8F 4B C4 43 B6 0E 1D 4C 64 40 77 2A 1E A3 3E 83 4B 7E B9 63 5A 6C 3A 84 00 FE 1D 79 DD 69 66 7C 81 04 A3 1E EB ",
         },
         { // First tx from block 140493
            "F3 42 CD 2C 96 B9 14 3D 62 0D 16 DC CD A3 B0 C5 C7 2C C3 04 8B 61 16 17 D0 7E D2 1B 40 27 4C F3 ",
            "30 44 02 20 6C 40 55 06 51 DB C7 4C E8 E0 32 7F 9D D1 23 FE 8D 8C FE EC 99 83 26 6D D6 15 F0 60 A9 3F 28 62 02 20 73 96 00 12 CF 73 3B 07 C1 0E 1C 9E 81 26 38 80 30 BE 32 50 92 53 62 85 63 AC 75 22 50 EA 27 37 01 ",
            "04 60 06 5D F9 A0 15 8A E5 D7 94 4C 72 95 D1 92 CA 9E 47 DD 3F 14 0C E9 3D EF E1 78 2A 84 44 3A BC BA 0A F7 77 E8 16 2A 2A 2A 99 50 49 55 FB E7 1D 98 6E DA 00 67 FA 1A 9A 1C 5B F3 0F 61 9D 87 57 ",
         },
      };
   }

   
   @Test(dataProvider="verificationData")
   public void testVerificationOnRealData(String hexData, String hexSignature, String hexPublicKey)
      throws VerificationException
   {
      byte[] data = HexUtil.toByteArray(hexData);
      byte[] reverseData = new byte[data.length];
      for ( int i=0; i<data.length; i++ )
         reverseData[data.length-i-1]=data[i];
      byte[] signature = HexUtil.toByteArray(hexSignature);
      byte[] publicKey = HexUtil.toByteArray(hexPublicKey);
      // Now check that this signature is valid
      KeyStore store = EasyMock.createMock(KeyStore.class);
      KeyFactoryImpl factory = new KeyFactoryImpl(store);
      PublicKey key = factory.createPublicKey(publicKey);
      Assert.assertTrue(key.verify(reverseData,signature),"couldn't verify real live data");
   }

}

