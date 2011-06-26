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
import org.easymock.EasyMock;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.KeyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the script engine with a black-box approach. We will not see into the current stack
 * but will use the different instructions to check eachother.
 * @author Robert Brautigam
 */
@Test
public class ScriptTests
{
   private static Logger logger = LoggerFactory.getLogger(ScriptTests.class);

   /**
    * This method is used to parse a script and return the byte representation. Format is
    * "OP_CODE" (string) for operations and &lt;hex&gt; for constants separated by spaces. The "hex"
    * data constant must contain two-digit hexadecimal numbers in order of data. For example:
    * OP_DUP OP_HASH160 CONSTANT &lt;1a a0 cd 1c be a6 e7 45 8a 7a ba d5 12 a9 d9 ea 1a fb 22 5e&gt;.
    */
   private byte[] toScript(String script)
      throws IOException
   {
      // Prepare output
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      InstructionOutputStream output = new InstructionOutputStream(byteOutput);
      // Parse next operation
      int currentPosition = 0;
      while ( currentPosition < script.length() )
      {
         // Get operation
         int nextSpace = script.indexOf(" ",currentPosition);
         if ( nextSpace < 0 )
            nextSpace = script.length();
         Operation op = Operation.valueOf(script.substring(currentPosition,nextSpace));
         currentPosition=nextSpace+1; // Skip space
         // Parse data parameter if this operation requires one
         byte[] data = null;
         if ( (op==Operation.CONSTANT) || (op==Operation.OP_PUSHDATA1) || 
               (op==Operation.OP_PUSHDATA2) || (op==Operation.OP_PUSHDATA4) )
         {
            int nextGt = script.indexOf(">",currentPosition);
            data = HexUtil.toByteArray(script.substring(currentPosition+1,nextGt));
            currentPosition = nextGt+2; // Skip '>' and next space too
         }
         // Write instruction
         output.writeInstruction(new Instruction(op,data));
      }
      return byteOutput.toByteArray();
   }

   /**
    * Create a script an execute.
    */
   private boolean execute(String script)
      throws ScriptException, IOException
   {
      // Create mocks
      Transaction tx = EasyMock.createMock(Transaction.class);
      TransactionInput txIn = EasyMock.createMock(TransactionInput.class);
      KeyFactory keyFactory = EasyMock.createMock(KeyFactory.class);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(script),keyFactory,0);
      logger.debug("executing script: "+script+", which in bytes is: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script
      return scriptImpl.execute(tx,txIn);
   }

   // Concept tests

   @Test(expectedExceptions=ScriptException.class)
   public void testEmptyScript()
      throws Exception
   {
      execute("");
   }

   // Constants tests

   public void testFalseResult()
      throws Exception
   {
      Assert.assertFalse(execute("OP_0"));
   }

   public void testTrueResult()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1"));
   }

   public void testNegative()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1NEGATE OP_1ADD OP_1ADD"));
   }

   public void testOtherConstants()
      throws Exception
   {
      for ( int i=2; i<=16; i++ )
      {
         StringBuilder script = new StringBuilder("OP_"+i);
         for ( int o=0; o<i-1; o++ )
            script.append(" OP_1SUB");
         Assert.assertTrue(execute(script.toString()));
      }
   }

   public void testConstant()
      throws Exception
   {
      Assert.assertTrue(execute("CONSTANT <01 02 03 04> OP_SIZE OP_1SUB OP_1SUB OP_1SUB"));
   }

   public void testPush1()
      throws Exception
   {
      Assert.assertTrue(execute("OP_PUSHDATA1 <01 02 03 04> OP_SIZE OP_1SUB OP_1SUB OP_1SUB"));
   }

   public void testPush2()
      throws Exception
   {
      Assert.assertTrue(execute("OP_PUSHDATA2 <01 02 03 04> OP_SIZE OP_1SUB OP_1SUB OP_1SUB"));
   }

   public void testPush4()
      throws Exception
   {
      Assert.assertTrue(execute("OP_PUSHDATA4 <01 02 03 04> OP_SIZE OP_1SUB OP_1SUB OP_1SUB"));
   }

   // Flow control tests

   public void testNop()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_NOP"));
   }

   public void testIfConcept()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_IF OP_0 OP_ENDIF"));
   }

   public void testIfElseExecutes()
      throws Exception
   {
      Assert.assertFalse(execute("OP_0 OP_1 OP_IF OP_NOP OP_ELSE OP_1 OP_ENDIF"));
   }

   public void testIfNoElse()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_IF OP_0 OP_ENDIF"));
   }

   @Test(expectedExceptions=ScriptException.class)
   public void testIfNoEndif()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_IF OP_0"));
   }

   public void testNotif()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_0 OP_NOTIF OP_0 OP_ENDIF"));
   }

   public void testVerify()
      throws Exception
   {
      Assert.assertFalse(execute("OP_0 OP_1 OP_VERIFY"));
   }

   public void testReturn()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_RETURN"));
   }

   // Stack tests

   public void testAltstack()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_0 OP_TOALTSTACK OP_VERIFY OP_FROMALTSTACK"));
   }

   public void testIfdup()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_IFDUP OP_TOALTSTACK OP_IFDUP OP_VERIFY"));
   }

   public void testDepth()
      throws Exception
   {
      Assert.assertTrue(execute("OP_0 OP_DEPTH"));
   }

   public void testDrop()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_DROP"));
   }

   public void testDup()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_DUP OP_VERIFY"));
   }

   public void testNip()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_1 OP_NIP OP_VERIFY"));
   }

   public void testOver()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_OVER OP_VERIFY OP_DROP"));
   }

   public void testPick()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_0 OP_0 OP_3 OP_PICK OP_VERIFY OP_DEPTH OP_1SUB OP_1SUB OP_1SUB"));
   }

   public void testRoll()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_0 OP_0 OP_3 OP_ROLL OP_VERIFY OP_DEPTH OP_1SUB OP_1SUB"));
   }

   public void testRot()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_0 OP_1 OP_ROT OP_VERIFY OP_VERIFY"));
   }

   public void testSwap()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_0 OP_SWAP OP_VERIFY"));
   }

   public void testTuck()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_0 OP_TUCK OP_1ADD OP_VERIFY OP_VERIFY"));
   }

   public void test2Drop()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_0 OP_2DROP"));
   }

   public void test2Dup()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_2DUP OP_DEPTH OP_1SUB OP_1SUB OP_1SUB"));
   }

   public void test2Over()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_2 OP_3 OP_4 OP_2OVER OP_DROP"));
   }

   public void test2Rot()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_2 OP_3 OP_4 OP_5 OP_6 OP_2ROT OP_DROP"));
   }

   public void test2Swap()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_2 OP_3 OP_4 OP_2SWAP OP_DROP"));
   }
}

