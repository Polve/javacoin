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

package hu.netmind.bitcoin.wallet;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.easymock.EasyMock;
import hu.netmind.bitcoin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;
import java.util.Arrays;
import java.math.BigInteger;

/**
 * @author Robert Brautigam
 */
@Test
public class AddressTests
{
   private static final Logger logger = LoggerFactory.getLogger(AddressTests.class);

   public void testBase58Symmetry()
   {
      // Generate a random 20 byte array
      byte[] keyHash = new byte[20];
      Random rnd = new Random();
      rnd.nextBytes(keyHash);
      logger.debug("will encode random bytes: "+Arrays.toString(keyHash));
      // Generate string
      String encoded = Base58.encode(keyHash);
      logger.debug("encoded to: "+encoded);
      // Now decode back
      byte[] keyHashDecoded = Base58.decode(encoded);
      logger.debug("decoded to: "+Arrays.toString(keyHashDecoded));
      // Check
      Assert.assertEquals(keyHashDecoded.length,keyHash.length);
      Assert.assertEquals(keyHashDecoded,keyHash);
   }

   public void testSymmetry()
      throws VerificationException
   {
      // Generate a random 20 byte key hash
      byte[] keyHash = new byte[20];
      Random rnd = new Random();
      rnd.nextBytes(keyHash);
      // Create an address
      Address encode = new Address(Address.Type.MAIN,keyHash);
      logger.debug("generated address: "+encode.toString()+
            ", hashkey was: "+new BigInteger(1,keyHash).toString(16));
      // Now parse it back
      Address decode = new Address(encode.toString());
      // Check
      Assert.assertEquals(decode.getKeyType(),encode.getKeyType());
      Assert.assertEquals(decode.getKeyHash(),encode.getKeyHash());
   }

   public void testExampleAddress()
      throws VerificationException
   {
      byte[] exampleBytes = new byte[]
         { (byte)0x42, (byte)0xbd, (byte)0x6b, (byte)0x9e, (byte)0xeb, (byte)0x1d, (byte)0xa0, (byte)0x15, (byte)0x04, (byte)0xfe, 
           (byte)0xfe, (byte)0x01, (byte)0x4e, (byte)0x16, (byte)0x41, (byte)0x52, (byte)0x46, (byte)0xc0, (byte)0xf6, (byte)0x6f  };
      String exampleEncoded = "175tWpb8K1S7NmH4Zx6rewF9WQrcZv2456";
      // Check encoding
      Address encoded = new Address(Address.Type.MAIN,exampleBytes);
      Assert.assertEquals(encoded.toString(),exampleEncoded);
      // Check decoding
      Address decoded = new Address(exampleEncoded);
      Assert.assertEquals(decoded.getKeyHash(),exampleBytes);
   }
}

