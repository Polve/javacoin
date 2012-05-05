/**
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package hu.netmind.bitcoin.keyfactory.ecc;

import hu.netmind.bitcoin.PublicKey;
import hu.netmind.bitcoin.VerificationException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

/**
 *
 * @author Alessandro Polverini
 */
public class BitcoinUtil
{

   private static final byte[] satoshiPubKeyBytes = toByteArray(
      "04 fc 97 02 84 78 40 aa f1 95 de 84 42 eb ec ed f5 b0 95 cd "
      + "bb 9b c7 16 bd a9 11 09 71 b2 8a 49 e0 ea d8 56 4f f0 db 22 "
      + "20 9e 03 74 78 2c 09 3b b8 99 69 2d 52 4e 9d 6a 69 56 e7 c5 "
      + "ec bc d6 82 84");
   private static final KeyFactoryImpl factory = new KeyFactoryImpl(null);
   public static final PublicKey satoshiPubKey = factory.createPublicKey(satoshiPubKeyBytes);

   /**
    * Helper method to convert strings like: "F0 A4 32 05", to actual byte array
    */
   public static byte[] toByteArray(String hexString)
   {
      StringTokenizer tokenizer = new StringTokenizer(hexString, " ");
      byte[] result = new byte[tokenizer.countTokens()];
      for (int i = 0; i < result.length; i++)
      {
         result[i] = (byte) Integer.valueOf(tokenizer.nextToken(), 16).intValue();
      }
      return result;
   }

   /**
    * Helper method to convert strings like: "F0 A4 32 05", to actual byte array
    */
   public static byte[] doubleDigest(byte[] input)
      throws NoSuchAlgorithmException
   {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] firstHash = digest.digest(input);
      digest.reset();
      return digest.digest(firstHash);
   }

   public static boolean verifyDoubleDigestSignature(PublicKey pubKey, byte[] message, byte[] signature)
   {
      try
      {
         return pubKey.verify(doubleDigest(message), signature);
      } catch (NoSuchAlgorithmException ex)
      {
         // Should not happen
         return false;
      } catch (VerificationException ex)
      {
         return false;
      }
   }

   public static boolean verifyDoubleDigestSatoshiSignature(byte[] message, byte[] signature)
   {
      return verifyDoubleDigestSignature(satoshiPubKey, message, signature);
   }
}
