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

import hu.netmind.bitcoin.ScriptException;
import hu.netmind.bitcoin.Script;
import hu.netmind.bitcoin.Transaction;
import hu.netmind.bitcoin.TransactionInput;
import java.io.IOException;
import java.util.Stack;

/**
 * Implements running a script.
 * @author Robert Brautigam
 */
public class ScriptImpl extends ScriptFragmentImpl implements Script
{
   ScriptImpl(byte[] script)
   {
      super(script);
   }

   private byte[] popData(Stack stack, String reason)
      throws ScriptException
   {
      if ( stack.empty() )
         throw new ScriptException(reason+", but stack was empty");
      if ( ! (stack.peek() instanceof byte[]) )
         throw new ScriptException(reason+", but top item in stack is not a byte array but: "+stack.peek());
      return (byte[]) stack.pop();
   }

   private int popInt(Stack stack, String reason)
      throws ScriptException
   {
      if ( stack.empty() )
         throw new ScriptException(reason+", but stack was empty");
      if ( ! (stack.peek() instanceof Number) )
         throw new ScriptException(reason+", but top item in stack is not a number but: "+stack.peek());
      return ((Number) stack.pop()).intValue();
   }

   private boolean popBoolean(Stack stack, String reason)
      throws ScriptException
   {
      int top = popInt(stack,reason);
      if ( (top!=0) && (top!=1) )
         throw new ScriptException(reason+", but top item was no 0 or 1 but: "+top);
      return top == 1;
   }

   /**
    * Execute the script and provide the output decision whether
    * spending of the given txIn is authorized.
    * @param tx The transaction which contains the input for spending.
    * @param txIn The input to verify.
    * @return True if spending is approved by this script, false otherwise.
    * @throws ScriptException If script can not be executed, or is an invalid script.
    */
   public boolean execute(Transaction tx, TransactionInput txIn)
      throws ScriptException
   {
      // Create the script input
      InstructionInputStream input = getInstructionInput();
      // Create script runtime
      Stack stack = new Stack();
      Stack altStack = new Stack();
      // Run the script
      try
      {
         Instruction instruction = null;
         while ( (instruction=input.readInstruction()) != null )
         {
            switch ( instruction.getOperation() )
            {
               case CONSTANT:
               case OP_PUSHDATA1:
               case OP_PUSHDATA2:
               case OP_PUSHDATA3:
                  // These instruction all push data to stack
                  stack.push(instruction.getData());
                  break;
               case OP_0:
                  stack.push(0);
                  break;
               case OP_1NEGATE:
                  stack.push(-1);
                  break;
               case OP_1:
                  stack.push(1);
                  break;
               case OP_2:
                  stack.push(2);
                  break;
               case OP_3:
                  stack.push(3);
                  break;
               case OP_4:
                  stack.push(4);
                  break;
               case OP_5:
                  stack.push(5);
                  break;
               case OP_6:
                  stack.push(6);
                  break;
               case OP_7:
                  stack.push(7);
                  break;
               case OP_8:
                  stack.push(8);
                  break;
               case OP_9:
                  stack.push(9);
                  break;
               case OP_10:
                  stack.push(10);
                  break;
               case OP_11:
                  stack.push(11);
                  break;
               case OP_12:
                  stack.push(12);
                  break;
               case OP_13:
                  stack.push(13);
                  break;
               case OP_14:
                  stack.push(14);
                  break;
               case OP_15:
                  stack.push(15);
                  break;
               case OP_16:
                  stack.push(16);
                  break;
               case OP_NOP:
                  // Nothing
                  break;
               case OP_IF:
               case OP_NOTIF:
                  boolean condition = popBoolean(stack,"executing OP_IF");
                  // Determine when to skip the body of if
                  if ( 
                        ((instruction.getOperation()==Operation.OP_IF) && (!condition)) ||
                        ((instruction.getOperation()==Operation.OP_NOTIF) && (condition)) )
                  {
                     // We need to skip to next OP_ELSE or OP_ENDIF whichever comes first
                     while ( (instruction != null ) &&
                             (instruction.getOperation()!=Operation.OP_ELSE) &&
                             (instruction.getOperation()!=Operation.OP_ENDIF) )
                        instruction = input.readInstruction();
                     if ( instruction == null )
                        throw new ScriptException("executing OP_IF, did not found closing OP_ENDIF");
                  }
                  break;
               case OP_ELSE:
                  // If we reached this, that means the body of the OP_IF or OP_NOTIF executed
                  // so that means we need to skip to OP_ENDIF
                  while ( (instruction != null ) &&
                        (instruction.getOperation()!=Operation.OP_ENDIF) )
                     instruction = input.readInstruction();
                  if ( instruction == null )
                     throw new ScriptException("executing OP_ELSE (skipping ELSE body), did not found closing OP_ENDIF");
                  break;
               case OP_ENDIF:
                  // If we are executing this, that means the body of OP_ELSE ran, so just
                  // do nothing
                  break;
               case OP_VERIFY:
                  condition = popBoolean(stack,"executing OP_VERIFY");
                  if ( ! condition )
                     return false; // Script fails
                  break;
               case OP_RETURN:
                  return false; // Fail script
               case OP_TOALTSTACK:
                  altStack.push(stack.pop());
                  break;
               case OP_FROMALTSTACK:
                  stack.push(altStack.pop());
                  break;
               case OP_IFDUP:
                  // Duplicate true on stack (leave false)
                  condition = popBoolean(stack,"executing OP_IFDUP");
                  if ( condition )
                  {
                     stack.push(1);
                     stack.push(1);
                  } 
                  else
                  {
                     stack.push(0);
                  }
                  break;
               case OP_DEPTH:
                  stack.push(stack.size());
                  break;
               case OP_DROP:
                  stack.pop();
                  break;
               case OP_DUP:
                  stack.push(stack.peek());
                  break;
               case OP_NIP:
                  // Removes second item
                  Object top = stack.pop();
                  stack.pop();
                  stack.push(top);
                  break;
               case OP_OVER:
                  top = stack.pop();
                  Object under = stack.peek();
                  stack.push(top);
                  stack.push(under);
                  break;
               case OP_PICK:
                  // Copy nth deep item on top
                  int depth = popInt(stack,"executing OP_PICK");
                  if ( depth < 0 )
                     throw new ScriptException("tried to OP_PICK negative index: "+depth);
                  if ( depth >= stack.size() )
                     throw new ScriptException("tried to OP_PICK deeper than stack: "+depth+" vs. "+stack.size());
                  stack.push(stack.get(stack.size()-1-depth));
                  break;
               case OP_ROLL:
                  // Move nth deep item to top
                  depth = popInt(stack,"executing OP_ROLL");
                  if ( depth < 0 )
                     throw new ScriptException("tried to OP_ROLL negative index: "+depth);
                  if ( depth >= stack.size() )
                     throw new ScriptException("tried to OP_ROLL deeper than stack: "+depth+" vs. "+stack.size());
                  stack.push(stack.remove(stack.size()-1-depth));
                  break;
               case OP_ROT:
                  Object x3 = stack.pop();
                  Object x2 = stack.pop();
                  Object x1 = stack.pop();
                  stack.push(x2);
                  stack.push(x3);
                  stack.push(x1);
                  break;
               case OP_SWAP:
                  x2 = stack.pop();
                  x1 = stack.pop();
                  stack.push(x2);
                  stack.push(x1);
                  break;
               case OP_TUCK:
                  x2 = stack.pop();
                  x1 = stack.pop();
                  stack.push(x2);
                  stack.push(x1);
                  stack.push(x2);
                  break;
               case OP_2DROP:
                  stack.pop();
                  stack.pop();
                  break;
               case OP_2DUP:
                  x2 = stack.pop();
                  x1 = stack.pop();
                  stack.push(x1);
                  stack.push(x2);
                  stack.push(x1);
                  stack.push(x2);
                  break;
               case OP_3DUP:
                  x3 = stack.pop();
                  x2 = stack.pop();
                  x1 = stack.pop();
                  stack.push(x1);
                  stack.push(x2);
                  stack.push(x3);
                  stack.push(x1);
                  stack.push(x2);
                  stack.push(x3);
                  break;
               case OP_2OVER:
                  Object x4 = stack.pop();
                  x3 = stack.pop();
                  x2 = stack.pop();
                  x1 = stack.pop();
                  stack.push(x1);
                  stack.push(x2);
                  stack.push(x3);
                  stack.push(x4);
                  stack.push(x1);
                  stack.push(x2);
                  break;
               case OP_2ROT:
                  Object x6 = stack.pop();
                  Object x5 = stack.pop();
                  x4 = stack.pop();
                  x3 = stack.pop();
                  x2 = stack.pop();
                  x1 = stack.pop();
                  stack.push(x3);
                  stack.push(x4);
                  stack.push(x5);
                  stack.push(x6);
                  stack.push(x1);
                  stack.push(x2);
                  break;
               case OP_2SWAP:
                  x4 = stack.pop();
                  x3 = stack.pop();
                  x2 = stack.pop();
                  x1 = stack.pop();
                  stack.push(x3);
                  stack.push(x4);
                  stack.push(x1);
                  stack.push(x2);
                  break;
               case OP_CAT:
                  throw new ScriptException("OP_CAT is disabled");
               case OP_SUBSTR:
                  throw new ScriptException("OP_SUBSTR is disabled");
               case OP_LEFT:
                  throw new ScriptException("OP_LEFT is disabled");
               case OP_RIGHT:
                  throw new ScriptException("OP_RIGHT is disabled");
               case OP_SIZE:
                  byte[] data = popData(stack,"executing OP_SIZE");
                  stack.push(data.length);
               case OP_INVERT:
                  throw new ScriptException("OP_INVERT is disabled");
               case OP_AND:
                  throw new ScriptException("OP_AND is disabled");
               case OP_OR:
                  throw new ScriptException("OP_OR is disabled");
               case OP_XOR:
                  throw new ScriptException("OP_XOR is disabled");
               case OP_EQUAL:
               case OP_EQUALVERIFY:
                  int a = popInt(stack,"executing OP_EQUAL(VERIFY)");
                  int b = popInt(stack,"executing OP_EQUAL(VERIFY)");
                  if ( instruction.getOperation()==Operation.OP_EQUALVERIFY )
                  {
                     // If VERIFY is called exit on false, and DON'T leave true in stack
                     if ( a != b )
                        return false;
                  }
                  else
                  {
                     // Put result on stack
                     stack.push( (a==b)?1:0 );
                  }
                  break;
               case OP_1ADD:
                  a = popInt(stack,"executing OP_1ADD");
                  stack.push( (a+1) );
                  break;
               case OP_1SUB:
                  a = popInt(stack,"executing OP_1SUB");
                  stack.push( (a-1) );
                  break;
               case OP_2MUL:
                  throw new ScriptException("OP_2MUL is disabled");
               case OP_2DIV:
                  throw new ScriptException("OP_2DIV is disabled");
               case OP_NEGATE:
                  a = popInt(stack,"executing OP_NEGATE");
                  stack.push( (-a) );
                  break;
               case OP_ABS:
                  a = popInt(stack,"executing OP_ABS");
                  if ( a < 0 )
                     stack.push( (-a) );
                  else
                     stack.push( (a) );
                  break;
               case OP_NOT:
               case OP_0NOTEQUAL:
                  a = popInt(stack,"executing OP_NOT/OP_0NOTEQUAL");
                  if ( a == 0 )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_ADD:
                  a = popInt(stack,"executing OP_ADD");
                  b = popInt(stack,"executing OP_ADD");
                  stack.push( (a+b) );
                  break;
               case OP_SUB:
                  a = popInt(stack,"executing OP_SUB");
                  b = popInt(stack,"executing OP_SUB");
                  stack.push( (a-b) );
                  break;
               case OP_MUL:
                  throw new ScriptException("OP_MUL is disabled");
               case OP_DIV:
                  throw new ScriptException("OP_DIV is disabled");
               case OP_MOD:
                  throw new ScriptException("OP_MOD is disabled");
               case OP_LSHIFT:
                  throw new ScriptException("OP_LSHIFT is disabled");
               case OP_RSHIFT:
                  throw new ScriptException("OP_RSHIFT is disabled");
               case OP_BOOLAND:
                  a = popInt(stack,"executing OP_BOOLAND");
                  b = popInt(stack,"executing OP_BOOLAND");
                  if ( (a!=0) && (b!=0) )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_BOOLOR:
                  a = popInt(stack,"executing OP_BOOLOR");
                  b = popInt(stack,"executing OP_BOOLOR");
                  if ( (a!=0) || (b!=0) )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_NUMEQUAL:
                  a = popInt(stack,"executing OP_NUMEQUAL");
                  b = popInt(stack,"executing OP_NUMEQUAL");
                  if ( a == b )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_NUMEQUALVERIFY:
                  a = popInt(stack,"executing OP_NUMEQUALVERIFY");
                  b = popInt(stack,"executing OP_NUMEQUALVERIFY");
                  if ( a != b )
                     return false; // Abort
                  break;
               case OP_NUMNOTEQUAL:
                  a = popInt(stack,"executing OP_NUMNOTEQUAL");
                  b = popInt(stack,"executing OP_NUMNOTEQUAL");
                  if ( a != b )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_LESSTHAN:
                  a = popInt(stack,"executing OP_LESSTHAN");
                  b = popInt(stack,"executing OP_LESSTHAN");
                  if ( a < b )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_GREATERTHAN:
                  a = popInt(stack,"executing OP_GREATERTHAN");
                  b = popInt(stack,"executing OP_GREATERTHAN");
                  if ( a > b )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_LESSTHANOREQUAL:
                  a = popInt(stack,"executing OP_LESSTHANOREQUAL");
                  b = popInt(stack,"executing OP_LESSTHANOREQUAL");
                  if ( a <= b )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_GREATERTHANOREQUAL:
                  a = popInt(stack,"executing OP_GREATERTHANOREQUAL");
                  b = popInt(stack,"executing OP_GREATERTHANOREQUAL");
                  if ( a >= b )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_MIN:
                  a = popInt(stack,"executing OP_MIN");
                  b = popInt(stack,"executing OP_MIN");
                  if ( a < b )
                     stack.push(a);
                  else
                     stack.push(b);
                  break;
               case OP_MAX:
                  a = popInt(stack,"executing OP_MAX");
                  b = popInt(stack,"executing OP_MAX");
                  if ( a > b )
                     stack.push(a);
                  else
                     stack.push(b);
                  break;
               case OP_WITHIN:
                  a = popInt(stack,"executing OP_WITHIN");
                  int min = popInt(stack,"executing OP_WITHIN");
                  int max = popInt(stack,"executing OP_WITHIN");
                  if ( (a>=min) && (a<=max) )
                     stack.push(1);
                  else
                     stack.push(0);
                  break;
               case OP_RIPEMD160:
                  break;
               case OP_SHA1:
                  break;
               case OP_SHA256:
                  break;
               case OP_HASH160:
                  break;
               case OP_HASH256:
                  break;
               case OP_CODESEPARATOR:
                  break;
               case OP_CHECKSIG:
                  break;
               case OP_CHECKSIGVERIFY:
                  break;
               case OP_CHECKMULTISIG:
                  break;
               case OP_CHECKMULTISIGVERIFY:
                  break;
               case OP_PUBKEYHASH:
                  throw new ScriptException("OP_PUBKEYHASH is a pseudo-word, should not be in a script");
               case OP_PUBKEY:
                  throw new ScriptException("OP_PUBKEY is a pseudo-word, should not be in a script");
               case OP_INVALIDOPCODE:
                  throw new ScriptException("OP_INVALIDOPCODE is a pseudo-word, should not be in a script");
               case OP_RESERVED:
               case OP_VER:
               case OP_VERIF:
               case OP_VERNOTIF:
               case OP_RESERVED1:
               case OP_RESERVED2:
                  // Transaction is invalid
                  return false;
               case OP_NOP1:
               case OP_NOP2:
               case OP_NOP3:
               case OP_NOP4:
               case OP_NOP5:
               case OP_NOP6:
               case OP_NOP7:
               case OP_NOP8:
               case OP_NOP9:
               case OP_NOP10:
                  // Ignore NOPs
                  break;
               default:
                  throw new ScriptException("unhandled operation encountered: "+instruction.getOperation());
            }
         }
      } catch ( IOException e ) {
         throw new ScriptException("error reading instructions",e);
      }
      // Determine whether it was successful (top item is TRUE)
      return popBoolean(stack,"determining script result");
   }

}

