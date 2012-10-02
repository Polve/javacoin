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

// TODO: Import test scripts from https://bitcointalk.org/index.php?topic=77422.0

package hu.netmind.bitcoin.script;

import it.nibbles.bitcoin.utils.BtcUtil;
import hu.netmind.bitcoin.*;
import org.testng.annotations.Test;
import org.testng.Assert;
import static org.easymock.EasyMock.*;
import org.easymock.Capture;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
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
      TransactionInput txIn = createMock(TransactionInput.class);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(script),keyFactory,0);
      logger.debug("executing script: "+script+", which in bytes is: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script
      return scriptImpl.execute(txIn);
   }

   // Concept tests

   @Test(expectedExceptions=ScriptException.class)
   public void testEmptyScript()
      throws Exception
   {
      execute("");
   }

   public void testInvalidOperation()
      throws Exception
   {
      Assert.assertFalse(execute("OP_1 OP_RESERVED"));
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

   public void testRoll2()
      throws Exception
   {
      Assert.assertTrue(execute("OP_0 OP_1 OP_2 OP_3 OP_2 OP_ROLL OP_1 OP_EQUALVERIFY OP_3 OP_EQUALVERIFY OP_2 OP_EQUALVERIFY OP_NOT"));
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
   
   // "Splice" tests

   public void testSize()
      throws Exception
   {
      Assert.assertTrue(execute("OP_PUSHDATA1 <01 02 03 04> OP_SIZE OP_1SUB OP_1SUB OP_1SUB"));
   }

   public void testOpSize()
      throws Exception
   {
      Assert.assertTrue(execute("CONSTANT <33> OP_SIZE OP_1 OP_EQUAL OP_DROP CONSTANT <33> OP_EQUAL"));
   }

   // Bitwise logic tests

   public void testEqualsNumber()
      throws Exception
   {
      Assert.assertTrue(execute("OP_5 OP_6 OP_1SUB OP_EQUAL"));
   }

   public void testEqualsVerifyNumber()
      throws Exception
   {
      Assert.assertFalse(execute("OP_0 OP_5 OP_6 OP_1SUB OP_EQUALVERIFY"));
   }

   public void testEqualsData()
      throws Exception
   {
      Assert.assertTrue(execute("OP_PUSHDATA1 <01 02 03 04> CONSTANT <01 02 03 04> OP_EQUAL"));
   }

   // Arithmetic tests

   public void testAddSub1()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_1SUB OP_1ADD"));
   }

   public void testNeg()
      throws Exception
   {
      Assert.assertFalse(execute("OP_3 OP_NEGATE OP_1ADD OP_1ADD OP_1ADD"));
   }

   public void testAbs()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1NEGATE OP_ABS"));
   }

   public void testNot()
      throws Exception
   {
      Assert.assertFalse(execute("OP_0 OP_NOT OP_VERIFY OP_2 OP_NOT"));
   }

   public void test0NotEquals()
      throws Exception
   {
      Assert.assertFalse(execute("OP_2 OP_0NOTEQUAL OP_VERIFY OP_0 OP_0NOTEQUAL"));
   }

   public void testAddSub()
      throws Exception
   {
      Assert.assertTrue(execute("OP_5 OP_6 OP_ADD OP_10 OP_SUB"));
   }

   public void testBoolOps()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_1 OP_BOOLOR OP_BOOLAND"));
   }

   public void testNumEquals()
      throws Exception
   {
      Assert.assertTrue(execute("OP_5 OP_6 OP_1SUB OP_NUMEQUAL"));
   }

   public void testNumEqualsVerify()
      throws Exception
   {
      Assert.assertFalse(execute("OP_0 OP_5 OP_6 OP_1SUB OP_NUMEQUALVERIFY"));
   }

   public void testNumNotEquals()
      throws Exception
   {
      Assert.assertTrue(execute("OP_5 OP_6 OP_NUMNOTEQUAL"));
   }

   public void testLessThan()
      throws Exception
   {
      Assert.assertTrue(execute("OP_5 OP_6 OP_LESSTHAN"));
   }

   public void testGreaterThan()
      throws Exception
   {
      Assert.assertFalse(execute("OP_5 OP_6 OP_GREATERTHAN"));
   }

   public void testLessThanEqual()
      throws Exception
   {
      Assert.assertFalse(execute("OP_6 OP_5 OP_LESSTHANOREQUAL"));
   }

   public void testGreaterThanEqual()
      throws Exception
   {
      Assert.assertTrue(execute("OP_5 OP_5 OP_GREATERTHANOREQUAL"));
   }

   public void testMin()
      throws Exception
   {
      Assert.assertFalse(execute("OP_0 OP_1 OP_MIN"));
   }

   public void testMax()
      throws Exception
   {
      Assert.assertTrue(execute("OP_0 OP_1 OP_MAX"));
   }

   public void testWithin()
      throws Exception
   {
      Assert.assertTrue(execute("OP_5 OP_0 OP_10 OP_WITHIN OP_VERIFY OP_4 OP_5 OP_10 OP_WITHIN OP_NOT"));
   }

   public void testWithinRightExclusive()
      throws Exception
   {
      Assert.assertTrue(execute("OP_1 OP_0 OP_1 OP_WITHIN OP_NOT"));
   }

   public void testWithinLeftExclusive()
      throws Exception
   {
      Assert.assertTrue(execute("OP_0 OP_0 OP_1 OP_WITHIN"));
   }

   // Crypto

   public void testRipemd160()
      throws Exception
   {
      // Samples from wikipedia
      Assert.assertTrue(execute(
               "CONSTANT <37 F3 32 F6 8D B7 7B D9 D7 ED D4 96 95 71 AD 67 1C F9 DD 3B> "+
               "CONSTANT <"+HexUtil.toHexString("The quick brown fox jumps over the lazy dog".getBytes())+"> "+
               "OP_RIPEMD160 OP_EQUAL"));
   }

   public void testSha1()
      throws Exception
   {
      // Samples from wikipedia
      Assert.assertTrue(execute(
               "CONSTANT <2F D4 E1 C6 7A 2D 28 FC ED 84 9E E1 BB 76 E7 39 1B 93 EB 12> "+
               "CONSTANT <"+HexUtil.toHexString("The quick brown fox jumps over the lazy dog".getBytes())+"> "+
               "OP_SHA1 OP_EQUAL"));
   }

   public void testSha256()
      throws Exception
   {
      // Samples from wikipedia
      Assert.assertTrue(execute(
               "CONSTANT <D7 A8 FB B3 07 D7 80 94 69 CA 9A BC B0 08 2E 4F 8D 56 51 E4 6D 3C DB 76 2D 02 D0 BF 37 C9 E5 92> "+
               "CONSTANT <"+HexUtil.toHexString("The quick brown fox jumps over the lazy dog".getBytes())+"> "+
               "OP_SHA256 OP_EQUAL"));
   }

   public void testHash160()
      throws Exception
   {
      Assert.assertTrue(execute(
               "CONSTANT <"+HexUtil.toHexString("The quick brown fox jumps over the lazy dog".getBytes())+"> "+
               "OP_DUP OP_SHA256 OP_RIPEMD160 OP_SWAP OP_HASH160 OP_EQUAL"));
   }

   public void testHash256()
      throws Exception
   {
      Assert.assertTrue(execute(
               "CONSTANT <"+HexUtil.toHexString("The quick brown fox jumps over the lazy dog".getBytes())+"> "+
               "OP_DUP OP_SHA256 OP_SHA256 OP_SWAP OP_HASH256 OP_EQUAL"));
   }

   public void testSeparatorOnSecondFragment()
      throws Exception
   {
      testSubscript("CONSTANT <01 02 03 04> CONSTANT <05 06 07 08> OP_NOP OP_CODESEPARATOR OP_CHECKSIG",10,
            "OP_CHECKSIG");
   }

   public void testNoSeparatorSignatureScript()
      throws Exception
   {
      testSubscript("CONSTANT <01 02 03 04> CONSTANT <05 06 07 08> OP_CHECKSIG",10,
            "OP_CHECKSIG");
   }

   public void testSignatureScriptDoesNotContainSignature()
      throws Exception
   {
      testSubscript("CONSTANT <01 02 03 04> CONSTANT <05 06 07 08> OP_CHECKSIG",0,
            "CONSTANT <05 06 07 08> OP_CHECKSIG");
   }

   public void testSeparatorOnFirstFragment()
      throws Exception
   {
      testSubscript("CONSTANT <01 02 03 04> CONSTANT <05 06 07 08> OP_CODESEPARATOR OP_CHECKSIG OP_NOP OP_1",12,
            "OP_CHECKSIG");
   }

   private void testSubscript(String script, int boundary, String expected)
      throws Exception
   {
      // Create mocks
      TransactionInput txIn = createMock(TransactionInput.class);
      Capture<ScriptFragment> subscriptCapture = new Capture<ScriptFragment>();
      expect(txIn.getSignatureHash((SignatureHashType) anyObject(),capture(subscriptCapture))).andReturn(new byte[] { 00, 00 });
      replay(txIn);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey((byte[]) anyObject())).andReturn(createMock(PublicKey.class));
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(script),keyFactory,boundary);
      // Execute
      scriptImpl.execute(txIn);
      // Check results
      Assert.assertEquals(subscriptCapture.getValue().toString(),new ScriptFragmentImpl(toScript(expected)).toString());
   }

   public void testChecksigNoSeparator()
      throws Exception
   {
      // Create data
      byte[] signature = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] pubkey = new byte[] { 44, 42, 53, 12, 3, 1, 1, 1, 1, 1 };
      byte[] hash = new byte[] { 1, 2, 3, 4 };
      // Create transaction mock and return hash
      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(SignatureHashTypeImpl.SIGHASH_ALL), 
               eq(new ScriptFragmentImpl(HexUtil.toByteArray("0A 2C 2A 35 0C 03 01 01 01 01 01 AC")))
               )).andReturn(hash);
      replay(txIn);
      // Create key factory and expect verify call to public key
      PublicKey publicKey = createMock(PublicKey.class);
      expect(publicKey.verify(aryEq(hash),aryEq(signature))).andReturn(true);
      replay(publicKey);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey))).andReturn(publicKey);
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
               "CONSTANT <"+HexUtil.toHexString(signature)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey)+"> "+
               "OP_CHECKSIG"
               ),keyFactory,0);
      logger.debug("executing checksig script in bytes: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script and check
      Assert.assertTrue(scriptImpl.execute(txIn));
      verify(publicKey);
      verify(keyFactory);
   }

   public void testChecksigDefaultHashType()
      throws Exception
   {
      // Create data
      byte[] signature = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] pubkey = new byte[] { 44, 42, 53, 12, 3, 1, 1, 1, 1, 1 };
      byte[] hash = new byte[] { 1, 2, 3, 4 };
      // Create transaction mock and return hash
      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(new SignatureHashTypeImpl(0)), 
               eq(new ScriptFragmentImpl(HexUtil.toByteArray("0A 2C 2A 35 0C 03 01 01 01 01 01 AC")))
               )).andReturn(hash);
      replay(txIn);
      // Create key factory and expect verify call to public key
      PublicKey publicKey = createMock(PublicKey.class);
      expect(publicKey.verify(aryEq(hash),aryEq(signature))).andReturn(true);
      replay(publicKey);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey))).andReturn(publicKey);
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
               "CONSTANT <"+HexUtil.toHexString(signature)+" 00> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey)+"> "+
               "OP_CHECKSIG"
               ),keyFactory,0);
      logger.debug("executing checksig script in bytes: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script and check
      Assert.assertTrue(scriptImpl.execute(txIn));
      verify(publicKey);
      verify(keyFactory);
   }

   public void testChecksigVerifyNoSeparator()
      throws Exception
   {
      // Create data
      byte[] signature = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] pubkey = new byte[] { 44, 42, 53, 12, 3, 1, 1, 1, 1, 1 };
      byte[] hash = new byte[] { 1, 2, 3, 4 };
      // Create transaction mock and return hash
      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(SignatureHashTypeImpl.SIGHASH_ALL), 
               eq(new ScriptFragmentImpl(HexUtil.toByteArray("00 0A 2C 2A 35 0C 03 01 01 01 01 01 AD")))
               )).andReturn(hash);
      replay(txIn);
      // Create key factory and expect verify call to public key
      PublicKey publicKey = createMock(PublicKey.class);
      expect(publicKey.verify(aryEq(hash),aryEq(signature))).andReturn(true);
      replay(publicKey);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey))).andReturn(publicKey);
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript("OP_0 "+
               "CONSTANT <"+HexUtil.toHexString(signature)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey)+"> "+
               "OP_CHECKSIGVERIFY"
               ),keyFactory,0);
      logger.debug("executing checksig verify script in bytes: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script and check
      Assert.assertFalse(scriptImpl.execute(txIn));
      verify(publicKey);
      verify(keyFactory);
   }

   public void testChecksigSeparator()
      throws Exception
   {
      // Create data
      byte[] signature = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] pubkey = new byte[] { 44, 42, 53, 12, 3, 1, 1, 1, 1, 1 };
      byte[] hash = new byte[] { 1, 2, 3, 4 };
      // Create transaction mock and return hash
      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(SignatureHashTypeImpl.SIGHASH_ALL), 
               eq(new ScriptFragmentImpl(HexUtil.toByteArray("0A 2C 2A 35 0C 03 01 01 01 01 01 AC")))
               )).andReturn(hash);
      replay(txIn);
      // Create key factory and expect verify call to public key
      PublicKey publicKey = createMock(PublicKey.class);
      expect(publicKey.verify(aryEq(hash),aryEq(signature))).andReturn(true);
      replay(publicKey);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey))).andReturn(publicKey);
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript("OP_1 OP_NEGATE OP_CODESEPARATOR "+
               "CONSTANT <"+HexUtil.toHexString(signature)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey)+"> "+
               "OP_CHECKSIG OP_CODESEPARATOR"
               ),keyFactory,0);
      logger.debug("executing checksig separator script in bytes: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script and check
      Assert.assertTrue(scriptImpl.execute(txIn));
      verify(publicKey);
      verify(keyFactory);
   }

   public void testMultisigConcept()
      throws Exception
   {
      // Create data
      byte[] signature1 = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] signature2 = new byte[] { 54, 33, 21, 2 };
      byte[] pubkey1 = new byte[] { 2,3,4 };
      byte[] pubkey2 = new byte[] { 5,6,7 };
      byte[] pubkey3 = new byte[] { 1,8,9 };
      byte[] hash = new byte[] { 1, 2, 3, 4, 5 };
      // Create transaction mock and return hash
      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(SignatureHashTypeImpl.SIGHASH_ALL), 
               (ScriptFragment) anyObject()
               )).andReturn(hash).times(3);
      replay(txIn);
      // Create the 3 public keys corresponding to the data
      PublicKey publicKey1 = createMock(PublicKey.class);
      expect(publicKey1.verify(aryEq(hash),aryEq(signature1))).andReturn(true);
      replay(publicKey1);
      PublicKey publicKey2 = createMock(PublicKey.class);
      expect(publicKey2.verify(aryEq(hash),aryEq(signature2))).andReturn(false);
      replay(publicKey2);
      PublicKey publicKey3 = createMock(PublicKey.class);
      expect(publicKey3.verify(aryEq(hash),aryEq(signature2))).andReturn(true);
      replay(publicKey3);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey1))).andReturn(publicKey1);
      expect(keyFactory.createPublicKey(aryEq(pubkey2))).andReturn(publicKey2);
      expect(keyFactory.createPublicKey(aryEq(pubkey3))).andReturn(publicKey3);
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
               "OP_0 "+
               "CONSTANT <"+HexUtil.toHexString(signature2)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(signature1)+" 01> "+
               "OP_2 "+
               "CONSTANT <"+HexUtil.toHexString(pubkey3)+"> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey2)+"> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey1)+"> "+
               "OP_3 "+
               "OP_CHECKMULTISIG"
               ),keyFactory,0);
      // Run the script and check
      Assert.assertTrue(scriptImpl.execute(txIn));
      verify(publicKey1);
      verify(publicKey2);
      verify(publicKey3);
      verify(keyFactory);
   }

   public void testMultisigFail()
      throws Exception
   {
      // Create data
      byte[] signature1 = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] signature2 = new byte[] { 54, 33, 21, 2 };
      byte[] pubkey1 = new byte[] { 2,3,4 };
      byte[] pubkey2 = new byte[] { 5,6,7 };
      byte[] pubkey3 = new byte[] { 1,8,9 };
      byte[] hash = new byte[] { 1, 2, 3, 4, 5 };
      // Create transaction mock and return hash
      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(SignatureHashTypeImpl.SIGHASH_ALL), 
               (ScriptFragment) anyObject()
               )).andReturn(hash).times(3);
      replay(txIn);
      // Create the 3 public keys corresponding to the data
      PublicKey publicKey1 = createMock(PublicKey.class);
      expect(publicKey1.verify(aryEq(hash),aryEq(signature1))).andReturn(true);
      replay(publicKey1);
      PublicKey publicKey2 = createMock(PublicKey.class);
      expect(publicKey2.verify(aryEq(hash),aryEq(signature2))).andReturn(false);
      replay(publicKey2);
      PublicKey publicKey3 = createMock(PublicKey.class);
      expect(publicKey3.verify(aryEq(hash),aryEq(signature2))).andReturn(false);
      replay(publicKey3);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey1))).andReturn(publicKey1);
      expect(keyFactory.createPublicKey(aryEq(pubkey2))).andReturn(publicKey2);
      expect(keyFactory.createPublicKey(aryEq(pubkey3))).andReturn(publicKey3);
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
               "OP_0 "+
               "CONSTANT <"+HexUtil.toHexString(signature2)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(signature1)+" 01> "+
               "OP_2 "+
               "CONSTANT <"+HexUtil.toHexString(pubkey3)+"> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey2)+"> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey1)+"> "+
               "OP_3 "+
               "OP_CHECKMULTISIG"
               ),keyFactory,0);
      // Run the script and check
      Assert.assertFalse(scriptImpl.execute(txIn));
      verify(publicKey1);
      verify(publicKey2);
      verify(publicKey3);
      verify(keyFactory);
   }

   /*
    * http://blockexplorer.com/tx/369970d60ba54bae122be472366938626d2533e2f79cdda407e48eaa3765c68a
    * Input Tx in block 168710: 369970d60ba54bae122be472366938626d2533e2f79cdda407e48eaa3765c68a output 0
    * with script bytes 205a66f70c5d0be21192707d3b511a21e3043840acafd7f160637787d7401fe550
    * Redeemed in block 168910: 3a5e0977cc64e601490a761d83a4ea5be3cd03b0ffb73f5fe8be6507539be76c with script '1'
    */
   public void testRedeemConstantWithConstantBytes()
      throws Exception
   {
      Assert.assertTrue(execute(
               "OP_1 " +
               "CONSTANT <5A 66 F7 0C 5D 0B E2 11 92 70 7D 3B 51 1A 21 E3 04 38 40 AC AF D7 F1 60 63 77 87 D7 40 1F E5 50> "));
   }

   public void testRedeemConstantWithConstant1()
      throws Exception
   {
      Assert.assertTrue(execute(
               "CONSTANT <5A 66 F7 0C 5D 0B E2 11 92 70 7D 3B 51 1A 21 E3 04 38 40 AC AF D7 F1 60 63 77 87 D7 40 1F E5 50> "+
               "CONSTANT <01>"));
   }

   /*
    * http://blockexplorer.com/tx/0f24294a1d23efbb49c1765cf443fba7930702752aba6d765870082fe4f13cae
    * appeared in block 171043
    */
   public void testConstantEqualNumber()
      throws Exception
   {
      Assert.assertTrue(execute(
               "CONSTANT <03> CONSTANT <03> OP_MIN OP_3 OP_EQUAL"));
   }

   /*
    * Test the readLittleEndianInt function
    */
   public void testReadLittleEndianInt()
      throws Exception
   {
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("01")), 1);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("81")), -1);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("7F")), 127);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("FF")), -127);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("1234")), 4660);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("9234")), -4660);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("7FFF")), 32767);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("FFFF")), -32767);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("123456")), 1193046);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("923456")), -1193046);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("7FFFFF")), 8388607);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("FFFFFF")), -8388607);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("12345678")), 305419896);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("92345678")), -305419896);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("7FFFFFFF")), 2147483647);
      Assert.assertEquals(ScriptImpl.readLittleEndianInt(BtcUtil.hexIn("FFFFFFFF")), -2147483647);
   }

   /*
    * Test the readBigEndianInt function
    */
   public void testReadBigEndianInt()
      throws Exception
   {
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("01")), 1);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("81")), -1);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("7F")), 127);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("FF")), -127);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("3412")), 4660);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("3492")), -4660);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("FF7F")), 32767);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("FFFF")), -32767);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("563412")), 1193046);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("563492")), -1193046);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("FFFF7F")), 8388607);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("FFFFFF")), -8388607);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("78563412")), 305419896);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("78563492")), -305419896);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("FFFFFF7F")), 2147483647);
      Assert.assertEquals(ScriptImpl.readBigEndianInt(BtcUtil.hexIn("FFFFFFFF")), -2147483647);
   }

   /*
    * Test the toBigEndianByteArray function
    */
   public void testToBigEndianByteArray() throws ScriptException {
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(0), BtcUtil.hexIn("00"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(1), BtcUtil.hexIn("01"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-1), BtcUtil.hexIn("81"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(127), BtcUtil.hexIn("7F"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-127), BtcUtil.hexIn("FF"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(4660), BtcUtil.hexIn("3412"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-4660), BtcUtil.hexIn("3492"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(32767), BtcUtil.hexIn("FF7F"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-32767), BtcUtil.hexIn("FFFF"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(1193046), BtcUtil.hexIn("563412"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-1193046), BtcUtil.hexIn("563492"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(8388607), BtcUtil.hexIn("FFFF7F"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(305419896), BtcUtil.hexIn("78563412"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-305419896), BtcUtil.hexIn("78563492"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(2147483647), BtcUtil.hexIn("FFFFFF7F"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-2147483647), BtcUtil.hexIn("FFFFFFFF"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(4294967294L), BtcUtil.hexIn("FEFFFFFF00"));
      Assert.assertEquals( ScriptImpl.toBigEndianByteArray(-4294967294L), BtcUtil.hexIn("FEFFFFFF80"));
   }
   
   /*
    * Testnet transaction appearing in block 30301
    * http://blockexplorer.com/testnet/tx/a17b21f52859ed326d1395d8a56d5c7389f5fc83c17b9140a71d7cb86fdf0f5f
    * Last opcode, OP_CHECKMULTISIG has been removed
    */
   public void testTxA17b21f52859ed326d1395d8a56d5c7389f5fc83c17b9140a71d7cb86fdf0f5f()
      throws Exception
   {
      Assert.assertTrue(execute(
              "OP_0 "+
              "CONSTANT <30 46 02 21 00 D7 3F 63 3F 11 4E 0E 0B 32 4D 87 D3 8D 34 F2 29 66 A0 3B 07 28 03 AF A9 9C 94 08 20 1F 6D 6D C6 02 21 00 90 0E 85 BE 52 AD 22 78 D2 4E 7E DB B7 26 93 67 F5 F2 D6 F1 BD 33 8D 01 7C A4 60 00 87 76 61 44 01> "+
              "CONSTANT <30 44 02 20 71 FE F8 AC 0A A6 31 88 17 DB D2 42 BF 51 FB 5B 75 BE 31 2A A3 1E CB 44 A0 AF E7 B4 9F CF 84 03 02 20 4C 22 31 79 A3 83 BB 6F CB 80 31 2A C6 6E 47 33 45 06 5F 7D 91 36 F9 66 2D 86 7A CF 96 C1 2A 42 01> "+
              "OP_2 "+
              "CONSTANT <04 8C 00 6F F0 D2 CF DE 86 45 50 86 AF 5A 25 B8 8C 2B 81 85 8A AB 67 F6 A3 13 2C 88 5A 2C B9 EC 38 E7 00 57 6F D4 6C 7D 72 D7 D2 25 55 EE E3 A1 4E 28 76 C6 43 CD 70 B1 B0 A7 7F BF 46 E6 23 31 AC> "+
              "CONSTANT <04 B6 8E F7 D8 F2 4D 45 E1 77 11 01 E2 69 C0 AA CF 8D 3E D7 EB E1 2B 65 52 17 12 BB A7 68 EF 53 E1 E8 4F FF 3A FB EE 36 0A CE A0 D1 F4 61 C0 13 55 7F 71 D4 26 AC 17 A2 93 C5 EE BF 06 E4 68 25 3E> "+
              "OP_0 "+
              "OP_3 OP_ROLL OP_DUP OP_2 OP_GREATERTHANOREQUAL "+
              "OP_VERIFY OP_3 OP_ROLL OP_SIZE OP_NOT OP_OVER OP_HASH160 "+
              "CONSTANT <80 67 7C 53 92 22 0D B7 36 45 55 33 47 7D 0B C2 FB A6 55 02> "+
              "OP_EQUAL OP_BOOLOR "+
              "OP_VERIFY OP_3 OP_ROLL OP_SIZE OP_NOT OP_OVER OP_HASH160 "+
              "CONSTANT <02 D7 AA 2E 76 D9 06 6F B2 B3 C4 1F F8 83 9A 5C 81 BD CA 19> "+
              "OP_EQUAL OP_BOOLOR "+
              "OP_VERIFY OP_3 OP_ROLL OP_SIZE OP_NOT OP_OVER OP_HASH160 "+
              "CONSTANT <10 03 9C E4 FD B5 D4 EE 56 14 8F E3 93 5B 9B FB BE 4E CC 89> "+
              "OP_EQUAL OP_BOOLOR "+
              "OP_VERIFY OP_3"));
   }

   public String hexConstant(String hex)
   {
      int numBytes = hex.length() / 2;
      String op;
      if (numBytes <= 75)
         op = "CONSTANT";
      else if (numBytes <= 255)
         op = "OP_PUSHDATA1";
      else if (numBytes <= 65535)
         op = "OP_PUSHDATA2";
      else
         op = "OP_PUSHDATA4";
      return op + " <" + BtcUtil.hexOutSpaces(hex) + ">";
   }
   
   @Test(groups="checksig")
   public void testMultisigConcept2()
      throws Exception
   {
      // Create data
      byte[] signature1 = BtcUtil.hexIn(("304502206d2fced5c30e904fee5e9d4b82619c7af05d00e02a3cfda5d9e8af49e526316b022100c630706ad85622dc1d9a994246861aff13b38abfde29d650b9ef6ba97408f1a6"));
      byte[] signature2 = BtcUtil.hexIn(("3045022100ddd1d5aa757da18bc6aa7b378ae9bbaa521d5de71718c06605c0809ee596e253022005f3c729fbb1d9a2ecc4df765696711d24ee9b1a35a2adb748cf2689e53a6cc3"));
      byte[] pubkey1 = BtcUtil.hexIn("04b62fc786fdfccc3afb5d920ed34c56b7506a6670fdc18d64debd84a46f3e040399a6cb8eaca5e5b5443958536c73fedf19606925953f266ab983a988f307c3ac");
      byte[] pubkey2 = BtcUtil.hexIn("047ad29326dfd04f8b0b9d956165bc090f62e93957fc733370bb4234ba4c9506be2939f25b4893f6127e3010ac75f381ee00d5e8788d3c492e37ebb8cd39740a5f");
      byte[] pubkey3 = BtcUtil.hexIn("023f863e30d8ad2adfa1dad3dd06b17f6dc247cee6adc64f2687c2285debd91d85");
      byte[] hash = new byte[] { 1, 2, 3, 4, 5 };
      // Create transaction mock and return hash
      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(SignatureHashTypeImpl.SIGHASH_ALL), 
               (ScriptFragment) anyObject()
               )).andReturn(hash).times(3);
      replay(txIn);
      // Create the 3 public keys corresponding to the data
      PublicKey publicKey1 = createMock(PublicKey.class);
      expect(publicKey1.verify(aryEq(hash),aryEq(signature2))).andReturn(true);
      replay(publicKey1);
      PublicKey publicKey2 = createMock(PublicKey.class);
      expect(publicKey2.verify(aryEq(hash),aryEq(signature1))).andReturn(false);
      replay(publicKey2);
      PublicKey publicKey3 = createMock(PublicKey.class);
      expect(publicKey3.verify(aryEq(hash),aryEq(signature1))).andReturn(true);
      replay(publicKey3);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey1))).andReturn(publicKey3);
      expect(keyFactory.createPublicKey(aryEq(pubkey2))).andReturn(publicKey2);
      expect(keyFactory.createPublicKey(aryEq(pubkey3))).andReturn(publicKey1);
      replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
               "OP_0 "+
               "CONSTANT <"+HexUtil.toHexString(signature1)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(signature2)+" 01> "+
               "OP_2 "+
               "CONSTANT <"+HexUtil.toHexString(pubkey1)+"> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey2)+"> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey3)+"> "+
               "OP_3 "+
               "OP_CHECKMULTISIG"
               ),keyFactory,0);
      // Run the script and check
      logger.info("Esecuzione test funzionante");
      Assert.assertTrue(scriptImpl.execute(txIn));
      verify(publicKey1);
      verify(publicKey2);
      verify(publicKey3);
      verify(keyFactory);
   }

   // http://blockexplorer.com/testnet/tx/838c39e4e03b856344824cb3ef42918c87004493df5d812c9d52e8fa793de409#i633425
   @Test(groups="checksig")
   public void testBip16Multisig2of3() throws Exception
   {
      byte[] signature1 = BtcUtil.hexIn(("304502206d2fced5c30e904fee5e9d4b82619c7af05d00e02a3cfda5d9e8af49e526316b022100c630706ad85622dc1d9a994246861aff13b38abfde29d650b9ef6ba97408f1a6"));
      byte[] signature2 = BtcUtil.hexIn(("3045022100ddd1d5aa757da18bc6aa7b378ae9bbaa521d5de71718c06605c0809ee596e253022005f3c729fbb1d9a2ecc4df765696711d24ee9b1a35a2adb748cf2689e53a6cc3"));
      byte[] pubkey1 = BtcUtil.hexIn("04b62fc786fdfccc3afb5d920ed34c56b7506a6670fdc18d64debd84a46f3e040399a6cb8eaca5e5b5443958536c73fedf19606925953f266ab983a988f307c3ac");
      byte[] pubkey2 = BtcUtil.hexIn("047ad29326dfd04f8b0b9d956165bc090f62e93957fc733370bb4234ba4c9506be2939f25b4893f6127e3010ac75f381ee00d5e8788d3c492e37ebb8cd39740a5f");
      byte[] pubkey3 = BtcUtil.hexIn("023f863e30d8ad2adfa1dad3dd06b17f6dc247cee6adc64f2687c2285debd91d85");
      byte[] hash = new byte[] { 1, 2, 3, 4, 5 };

      ScriptFragment outFragment = new ScriptFragmentImpl(toScript(
         "OP_0 "+
         "CONSTANT <"+HexUtil.toHexString(signature1)+" 01> "+
         "CONSTANT <"+HexUtil.toHexString(signature2)+" 01> "+
         hexConstant("5241"+BtcUtil.hexOut(pubkey1)+"41"+BtcUtil.hexOut(pubkey2)+"21"+BtcUtil.hexOut(pubkey3)+"53ae")));
      
      ScriptFragment inFragment = new ScriptFragmentImpl(toScript(
         "OP_HASH160 " + hexConstant("f5b1f6ca8ba744a395f38ef0fbf6016bcaaf0a54") + " OP_EQUAL"));

      TransactionInput txIn = createMock(TransactionInput.class);
      expect(txIn.getSignatureHash(
               eq(SignatureHashTypeImpl.SIGHASH_ALL), 
               (ScriptFragment) anyObject()
               )).andReturn(hash).times(3);
      replay(txIn);
      PublicKey publicKey1 = createMock(PublicKey.class);
      expect(publicKey1.verify(aryEq(hash),aryEq(signature2))).andReturn(true);
      replay(publicKey1);
      PublicKey publicKey2 = createMock(PublicKey.class);
      expect(publicKey2.verify(aryEq(hash),aryEq(signature1))).andReturn(false);
      replay(publicKey2);
      PublicKey publicKey3 = createMock(PublicKey.class);
      expect(publicKey3.verify(aryEq(hash),aryEq(signature1))).andReturn(true);
      replay(publicKey3);
      KeyFactory keyFactory = createMock(KeyFactory.class);
      expect(keyFactory.createPublicKey(aryEq(pubkey1))).andReturn(publicKey3);
      expect(keyFactory.createPublicKey(aryEq(pubkey2))).andReturn(publicKey2);
      expect(keyFactory.createPublicKey(aryEq(pubkey3))).andReturn(publicKey1);
      replay(keyFactory);
      ScriptFactoryImpl scriptFactory = new ScriptFactoryImpl(keyFactory);
      ScriptImpl verificationScript = (ScriptImpl)scriptFactory.createScript(outFragment, inFragment);
      logger.info("Esecuzione test NON funzionante");
      Assert.assertTrue(verificationScript.isScriptHashType());
      Assert.assertTrue(verificationScript.isSigScriptPushOnly());
      Assert.assertTrue(verificationScript.isValidBip16());
      Assert.assertTrue(verificationScript.execute(txIn));
   }

}
