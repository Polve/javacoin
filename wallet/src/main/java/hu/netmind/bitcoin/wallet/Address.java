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

import hu.netmind.bitcoin.VerificationException;
import hu.netmind.bitcoin.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A quasi human readable representation of the key hash.
 * @author Robert Brautigam
 */
public class Address
{
   private Key.Type keyType;
   private byte[] keyHash;
   private String address;

   /**
    * Create the address object by parsing the human readable address.
    * @param address The human readable string representation of a key hash.
    */
   public Address(String address)
      throws VerificationException
   {
      this.address=address;
      // Parse
      byte[] decoded = Base58.decode(address);
      if ( decoded[0] == 0 )
         keyType = Key.Type.MAIN;
      else if ( decoded[0] == 111 )
         keyType = Key.Type.TEST;
      else
         throw new VerificationException("tried to construct an address with key type "+decoded[0]+", which is not a known type");
      keyHash = new byte[decoded.length-5];
      System.arraycopy(decoded,1,keyHash,0,keyHash.length);
      byte[] interimHash = new byte[decoded.length-4];
      System.arraycopy(decoded,0,keyHash,0,keyHash.length+1);
      // Verify checksum
      try
      {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] checksum1 = digest.digest(interimHash);
         digest.reset();
         byte[] checksum2 = digest.digest(checksum1);
         if ( 
              (checksum2[0] != decoded[decoded.length-4]) ||
              (checksum2[1] != decoded[decoded.length-3]) ||
              (checksum2[2] != decoded[decoded.length-2]) ||
              (checksum2[3] != decoded[decoded.length-1]) )
            throw new VerificationException("address checksum not valid");
      } catch ( NoSuchAlgorithmException e ) {
         throw new VerificationException("could not find SHA-256 algorithm",e);
      }
   }

   /**
    * Create the address by supplying the key hash.
    * @param keyType The version number of the address.
    * @param keyHash The key hash to represent.
    */
   public Address(Key.Type keyType, byte[] keyHash)
      throws VerificationException
   {
      this.keyType=keyType;
      this.keyHash=keyHash;
      // Create string representation
      byte[] composite = new byte[1+keyHash.length+4];
      if ( keyType == Key.Type.MAIN )
         composite[0] = 0;
      else if ( keyType == Key.Type.TEST )
         composite[0] = 111;
      else
         throw new VerificationException("tried to construct an address with key type "+keyType+", which is not a known type");
      System.arraycopy(keyHash,0,composite,1,keyHash.length);
      // Calculate hash and put first 4 bytes to the end of composite
      byte[] interimHash = new byte[keyHash.length+1];
      interimHash[0]=composite[0];
      System.arraycopy(keyHash,0,interimHash,1,keyHash.length);
      try
      {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] checksum1 = digest.digest(interimHash);
         digest.reset();
         byte[] checksum2 = digest.digest(checksum1);
         composite[composite.length-4]=checksum2[0];
         composite[composite.length-3]=checksum2[1];
         composite[composite.length-2]=checksum2[2];
         composite[composite.length-1]=checksum2[3];
      } catch ( NoSuchAlgorithmException e ) {
         throw new VerificationException("could not find SHA-256 algorithm",e);
      }
      // Encode
      address = Base58.encode(composite);
   }

   public String toString()
   {
      return address;
   }

   public byte[] getKeyHash()
   {
      return keyHash;
   }

   public Key.Type getKeyType()
   {
      return keyType;
   }
}

