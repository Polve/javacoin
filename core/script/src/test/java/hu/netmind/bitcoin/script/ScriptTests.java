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
import hu.netmind.bitcoin.ScriptFragment;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import hu.netmind.bitcoin.KeyFactory;
import hu.netmind.bitcoin.PublicKey;
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
      TransactionInput txIn = EasyMock.createMock(TransactionInput.class);
      KeyFactory keyFactory = EasyMock.createMock(KeyFactory.class);
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

   public void testChecksigNoSeparator()
      throws Exception
   {
      // Create data
      byte[] signature = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] pubkey = new byte[] { 44, 42, 53, 12, 3, 1, 1, 1, 1, 1 };
      byte[] hash = new byte[] { 1, 2, 3, 4 };
      // Create transaction mock and return hash
      TransactionInput txIn = EasyMock.createMock(TransactionInput.class);
      EasyMock.expect(txIn.getSignatureHash(
               EasyMock.eq(TransactionInput.SignatureHashType.SIGHASH_ALL), 
               EasyMock.eq(new ScriptFragmentImpl(HexUtil.toByteArray("0A 2C 2A 35 0C 03 01 01 01 01 01 AC")))
               )).andReturn(hash);
      EasyMock.replay(txIn);
      // Create key factory and expect verify call to public key
      PublicKey publicKey = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature))).andReturn(true);
      EasyMock.replay(publicKey);
      KeyFactory keyFactory = EasyMock.createMock(KeyFactory.class);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey))).andReturn(publicKey);
      EasyMock.replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
               "CONSTANT <"+HexUtil.toHexString(signature)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey)+"> "+
               "OP_CHECKSIG"
               ),keyFactory,0);
      logger.debug("executing checksig script in bytes: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script and check
      Assert.assertTrue(scriptImpl.execute(txIn));
      EasyMock.verify(publicKey);
      EasyMock.verify(keyFactory);
   }

   public void testChecksigVerifyNoSeparator()
      throws Exception
   {
      // Create data
      byte[] signature = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] pubkey = new byte[] { 44, 42, 53, 12, 3, 1, 1, 1, 1, 1 };
      byte[] hash = new byte[] { 1, 2, 3, 4 };
      // Create transaction mock and return hash
      TransactionInput txIn = EasyMock.createMock(TransactionInput.class);
      EasyMock.expect(txIn.getSignatureHash(
               EasyMock.eq(TransactionInput.SignatureHashType.SIGHASH_ALL), 
               EasyMock.eq(new ScriptFragmentImpl(HexUtil.toByteArray("00 0A 2C 2A 35 0C 03 01 01 01 01 01 AD")))
               )).andReturn(hash);
      EasyMock.replay(txIn);
      // Create key factory and expect verify call to public key
      PublicKey publicKey = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature))).andReturn(true);
      EasyMock.replay(publicKey);
      KeyFactory keyFactory = EasyMock.createMock(KeyFactory.class);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey))).andReturn(publicKey);
      EasyMock.replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript("OP_0 "+
               "CONSTANT <"+HexUtil.toHexString(signature)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey)+"> "+
               "OP_CHECKSIGVERIFY"
               ),keyFactory,0);
      logger.debug("executing checksig verify script in bytes: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script and check
      Assert.assertFalse(scriptImpl.execute(txIn));
      EasyMock.verify(publicKey);
      EasyMock.verify(keyFactory);
   }

   public void testChecksigSeparator()
      throws Exception
   {
      // Create data
      byte[] signature = new byte[] { 100, 101, 102, 103, 110, 3 };
      byte[] pubkey = new byte[] { 44, 42, 53, 12, 3, 1, 1, 1, 1, 1 };
      byte[] hash = new byte[] { 1, 2, 3, 4 };
      // Create transaction mock and return hash
      TransactionInput txIn = EasyMock.createMock(TransactionInput.class);
      EasyMock.expect(txIn.getSignatureHash(
               EasyMock.eq(TransactionInput.SignatureHashType.SIGHASH_ALL), 
               EasyMock.eq(new ScriptFragmentImpl(HexUtil.toByteArray("0A 2C 2A 35 0C 03 01 01 01 01 01 AC")))
               )).andReturn(hash);
      EasyMock.replay(txIn);
      // Create key factory and expect verify call to public key
      PublicKey publicKey = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature))).andReturn(true);
      EasyMock.replay(publicKey);
      KeyFactory keyFactory = EasyMock.createMock(KeyFactory.class);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey))).andReturn(publicKey);
      EasyMock.replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript("OP_1 OP_NEGATE OP_CODESEPARATOR "+
               "CONSTANT <"+HexUtil.toHexString(signature)+" 01> "+
               "CONSTANT <"+HexUtil.toHexString(pubkey)+"> "+
               "OP_CHECKSIG OP_CODESEPARATOR"
               ),keyFactory,0);
      logger.debug("executing checksig separator script in bytes: "+HexUtil.toHexString(scriptImpl.toByteArray()));
      // Run the script and check
      Assert.assertTrue(scriptImpl.execute(txIn));
      EasyMock.verify(publicKey);
      EasyMock.verify(keyFactory);
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
      TransactionInput txIn = EasyMock.createMock(TransactionInput.class);
      EasyMock.expect(txIn.getSignatureHash(
               EasyMock.eq(TransactionInput.SignatureHashType.SIGHASH_ALL), 
               (ScriptFragment) EasyMock.anyObject()
               )).andReturn(hash).times(3);
      EasyMock.replay(txIn);
      // Create the 3 public keys corresponding to the data
      PublicKey publicKey1 = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey1.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature1))).andReturn(true);
      EasyMock.replay(publicKey1);
      PublicKey publicKey2 = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey2.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature2))).andReturn(false);
      EasyMock.replay(publicKey2);
      PublicKey publicKey3 = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey3.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature2))).andReturn(true);
      EasyMock.replay(publicKey3);
      KeyFactory keyFactory = EasyMock.createMock(KeyFactory.class);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey1))).andReturn(publicKey1);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey2))).andReturn(publicKey2);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey3))).andReturn(publicKey3);
      EasyMock.replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
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
      EasyMock.verify(publicKey1);
      EasyMock.verify(publicKey2);
      EasyMock.verify(publicKey3);
      EasyMock.verify(keyFactory);
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
      TransactionInput txIn = EasyMock.createMock(TransactionInput.class);
      EasyMock.expect(txIn.getSignatureHash(
               EasyMock.eq(TransactionInput.SignatureHashType.SIGHASH_ALL), 
               (ScriptFragment) EasyMock.anyObject()
               )).andReturn(hash).times(3);
      EasyMock.replay(txIn);
      // Create the 3 public keys corresponding to the data
      PublicKey publicKey1 = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey1.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature1))).andReturn(true);
      EasyMock.replay(publicKey1);
      PublicKey publicKey2 = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey2.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature2))).andReturn(false);
      EasyMock.replay(publicKey2);
      PublicKey publicKey3 = EasyMock.createMock(PublicKey.class);
      EasyMock.expect(publicKey3.verify(EasyMock.aryEq(hash),EasyMock.aryEq(signature2))).andReturn(false);
      EasyMock.replay(publicKey3);
      KeyFactory keyFactory = EasyMock.createMock(KeyFactory.class);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey1))).andReturn(publicKey1);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey2))).andReturn(publicKey2);
      EasyMock.expect(keyFactory.createPublicKey(EasyMock.aryEq(pubkey3))).andReturn(publicKey3);
      EasyMock.replay(keyFactory);
      // Create script
      ScriptImpl scriptImpl = new ScriptImpl(toScript(
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
      EasyMock.verify(publicKey1);
      EasyMock.verify(publicKey2);
      EasyMock.verify(publicKey3);
      EasyMock.verify(keyFactory);
   }
}

