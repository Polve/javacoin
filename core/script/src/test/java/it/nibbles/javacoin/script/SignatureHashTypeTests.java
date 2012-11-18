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

package it.nibbles.javacoin.script;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.testng.Assert;
import it.nibbles.javacoin.SignatureHashType;
import it.nibbles.javacoin.SignatureHashType.InputSignatureHashType;
import it.nibbles.javacoin.SignatureHashType.OutputSignatureHashType;

/**
 * @author Robert Brautigam
 */
@Test
public class SignatureHashTypeTests
{
   public void testSighashAll()
   {
      Assert.assertEquals(new SignatureHashTypeImpl(1),SignatureHashTypeImpl.SIGHASH_ALL);
   }

   @DataProvider(name="typeCombinations")
   public Object[][] getTestScenarios()
   {
      return new Object[][] {
         {0x00, InputSignatureHashType.SIGHASH_ALL, OutputSignatureHashType.SIGHASH_ALL},
         {0x01, InputSignatureHashType.SIGHASH_ALL, OutputSignatureHashType.SIGHASH_ALL},
         {0x02, InputSignatureHashType.SIGHASH_ALLOWUPDATE, OutputSignatureHashType.SIGHASH_NONE},
         {0x03, InputSignatureHashType.SIGHASH_ALLOWUPDATE, OutputSignatureHashType.SIGHASH_SINGLE},
         {0x04, InputSignatureHashType.SIGHASH_ALL, OutputSignatureHashType.SIGHASH_ALL},
         {0x25, InputSignatureHashType.SIGHASH_ALL, OutputSignatureHashType.SIGHASH_ALL},
         {0x80, InputSignatureHashType.SIGHASH_ANYONECANPAY, OutputSignatureHashType.SIGHASH_ALL},
         {0x81, InputSignatureHashType.SIGHASH_ANYONECANPAY, OutputSignatureHashType.SIGHASH_ALL},
         {0x82, InputSignatureHashType.SIGHASH_ANYONECANPAY, OutputSignatureHashType.SIGHASH_NONE},
         {0x83, InputSignatureHashType.SIGHASH_ANYONECANPAY, OutputSignatureHashType.SIGHASH_SINGLE},
      };
   }

   @Test(dataProvider="typeCombinations")
   public void testAllAttributes(int value, InputSignatureHashType inputType,
         OutputSignatureHashType outputType)
   {
      SignatureHashTypeImpl type = new SignatureHashTypeImpl(value);
      Assert.assertEquals(type.getValue(),value);
      Assert.assertEquals(type.getInputType(),inputType);
      Assert.assertEquals(type.getOutputType(),outputType);
   }
}

