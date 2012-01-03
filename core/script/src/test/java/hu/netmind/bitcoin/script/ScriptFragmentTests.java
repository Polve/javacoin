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

package hu.netmind.bitcoin.script;

import org.testng.annotations.Test;
import org.testng.Assert;
import java.io.IOException;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.ScriptFragment;

/**
 * @author Robert Brautigam
 */
@Test
public class ScriptFragmentTests
{
   public void testParsingRealSignatureScript()
      throws IOException
   {
      // This is a signature script copied from wiki
      ScriptFragmentImpl fragment = new ScriptFragmentImpl(HexUtil.toByteArray(
               "48 30 45 02 21 00 F3 58 1E 19 72 AE 8A C7 C7 36 "+
               "7A 7A 25 3B C1 13 52 23 AD B9 A4 68 BB 3A 59 23 "+
               "3F 45 BC 57 83 80 02 20 59 AF 01 CA 17 D0 0E 41 "+
               "83 7A 1D 58 E9 7A A3 1B AE 58 4E DE C2 8D 35 BD "+
               "96 92 36 90 91 3B AE 9A 01 41 04 9C 02 BF C9 7E "+
               "F2 36 CE 6D 8F E5 D9 40 13 C7 21 E9 15 98 2A CD "+
               "2B 12 B6 5D 9B 7D 59 E2 0A 84 20 05 F8 FC 4E 02 "+
               "53 2E 87 3D 37 B9 6F 09 D6 D4 51 1A DA 8F 14 04 "+
               "2F 46 61 4A 4C 70 C0 F1 4B EF F5"));
      // Now see what this adds up to
      Assert.assertEquals(fragment.toString(),
            "CONSTANT <30 45 2 21 0 f3 58 1e 19 72 ae 8a c7 c7 36 7a 7a 25 3b c1 13 52 23 ad b9 a4 68 bb 3a 59 23 3f 45 bc 57 83 80 2 20 59 af 1 ca 17 d0 e 41 83 7a 1d 58 e9 7a a3 1b ae 58 4e de c2 8d 35 bd 96 92 36 90 91 3b ae 9a 1> CONSTANT <4 9c 2 bf c9 7e f2 36 ce 6d 8f e5 d9 40 13 c7 21 e9 15 98 2a cd 2b 12 b6 5d 9b 7d 59 e2 a 84 20 5 f8 fc 4e 2 53 2e 87 3d 37 b9 6f 9 d6 d4 51 1a da 8f 14 4 2f 46 61 4a 4c 70 c0 f1 4b ef f5>");
   }

   public void testParsingRealPubScript()
   {
      // This is a pub script copied from wiki
      ScriptFragmentImpl fragment = new ScriptFragmentImpl(HexUtil.toByteArray(
               "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+
               "A9 D9 EA 1A FB 22 5E 88 AC"));
      // Now see what this adds up to
      Assert.assertEquals(fragment.toString(),
            "OP_DUP OP_HASH160 CONSTANT <1a a0 cd 1c be a6 e7 45 8a 7a ba d5 12 a9 d9 ea 1a fb 22 5e> OP_EQUALVERIFY OP_CHECKSIG");
   }

   public void testSubscriptConcept()
      throws ScriptException
   {
      // This is a script which doesn't have a sig, but it has other constants in it
      ScriptFragmentImpl fragment = new ScriptFragmentImpl(HexUtil.toByteArray(
               "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+
               "A9 D9 EA 1A FB 22 5E 88 AC"));
      // Remove the pub key hash (pretend it's a sig)
      ScriptFragment subscript = fragment.getSubscript( 
            HexUtil.toByteArray("1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 A9 D9 EA 1A FB 22 5E"));
      // The result should be the script without this part
      Assert.assertEquals(HexUtil.toHexString(subscript.toByteArray()),"76 A9 88 AC");
   }

   public void testNotComplexScript()
      throws ScriptException
   {
      ScriptFragmentImpl fragment = new ScriptFragmentImpl(HexUtil.toByteArray(
               "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+
               "A9 D9 EA 1A FB 22 5E 88 AC"));
      Assert.assertFalse(fragment.isComputationallyExpensive());
   }

   public void testComplexScript()
      throws ScriptException
   {
      ScriptFragmentImpl fragment = new ScriptFragmentImpl(HexUtil.toByteArray(
               "76 A9 14 1A A0 CD 1C BE A6 E7 45 8A 7A BA D5 12 "+
               "A9 D9 EA 1A FB 22 5E 88 AC AC"));
      Assert.assertTrue(fragment.isComputationallyExpensive());
   }
}

