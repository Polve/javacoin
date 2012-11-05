package hu.netmind.bitcoin.block;

import java.util.Arrays;

/**
 *
 * @author Alessandro Polverini <alex@nibbles.it>
 */
public class HashWrapper
{

   byte[] hash;

   public HashWrapper(byte[] hash)
   {
      this.hash = hash;
   }

   @Override
   public int hashCode()
   {
      int len = hash.length;
      switch (len)
      {
         case 0:
            return 0;
         case 1:
            return hash[0];
         case 2:
            return hash[1] + hash[0] * 256;
         default:
            return hash[len - 1] + hash[len - 2] * 256 + hash[len - 3] * 256 * 256;
      }
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      final HashWrapper other = (HashWrapper) obj;
      if (!Arrays.equals(this.hash, other.hash))
         return false;
      return true;
   }
}
