/**
 * Copyright (C) 2012 NetMind Consulting Bt.
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

package hu.netmind.bitcoin.script;

import hu.netmind.bitcoin.SignatureHashType;

/**
 * Implementation of the signature hash type which extracts the
 * different values and flags from the value encoded.
 * @author Robert Brautigam
 */
public class SignatureHashTypeImpl implements SignatureHashType
{
   public static final int SIGHASH_ALL_CODE = 0x01;
   public static final int SIGHASH_NONE_CODE = 0x02;
   public static final int SIGHASH_SINGLE_CODE = 0x03;
   public static final int SIGHASH_ANYONECANPAY_CODE = 0x80;

   public static final SignatureHashTypeImpl SIGHASH_ALL = new SignatureHashTypeImpl(SIGHASH_ALL_CODE);

   private int value;
   private InputSignatureHashType inputType;
   private OutputSignatureHashType outputType;

   /**
    * Construct with the value encoded.
    * @param value The value encoded in the following way: the lower 5
    * bits encode 2=NONE, 3=SINGLE, all else ALL. Highest bit means
    * ANYONECANPAY. All other bits are ignored.
    */
   public SignatureHashTypeImpl(int value)
   {
      this.value=value;
      if ( (value & 0x1f) == SIGHASH_NONE_CODE )
      {
         inputType = InputSignatureHashType.SIGHASH_ALLOWUPDATE;
         outputType = OutputSignatureHashType.SIGHASH_NONE;
      }
      else if ( (value & 0x1f) == SIGHASH_SINGLE_CODE )
      {
         inputType = InputSignatureHashType.SIGHASH_ALLOWUPDATE;
         outputType = OutputSignatureHashType.SIGHASH_SINGLE;
      }
      else
      {
         inputType = InputSignatureHashType.SIGHASH_ALL;
         outputType = OutputSignatureHashType.SIGHASH_ALL;
      }
      if ( (value & SIGHASH_ANYONECANPAY_CODE) != 0 )
         inputType = InputSignatureHashType.SIGHASH_ANYONECANPAY;
   }

   public int getValue()
   {
      return value;
   }

   public InputSignatureHashType getInputType()
   {
      return inputType;
   }

   public OutputSignatureHashType getOutputType()
   {
      return outputType;
   }

   public int hashCode()
   {
      return value;
   }

   public boolean equals(Object obj)
   {
      if ( obj == null )
         return false;
      if ( !(obj instanceof SignatureHashTypeImpl) )
         return false;
      return value == ((SignatureHashTypeImpl) obj).value;
   }

   public String toString()
   {
      return Integer.toString(value,16)+" "+inputType+"/"+outputType;
   }
}

