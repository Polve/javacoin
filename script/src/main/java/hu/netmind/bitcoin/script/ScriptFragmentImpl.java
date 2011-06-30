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

import hu.netmind.bitcoin.ScriptFragment;
import hu.netmind.bitcoin.ScriptException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a script fragment using a byte array as backing.
 * @author Robert Brautigam
 */
public class ScriptFragmentImpl implements ScriptFragment
{
   private static final Logger logger = LoggerFactory.getLogger(ScriptFragmentImpl.class);

   private byte[] byteArray;

   ScriptFragmentImpl(byte[] byteArray)
   {
      this.byteArray=byteArray;
   }

   public byte[] toByteArray()
   {
      return byteArray;
   }

   public int hashCode()
   {
      return Arrays.hashCode(byteArray);
   }

   public boolean equals(Object o)
   {
      if ( ! (o instanceof ScriptFragmentImpl) )
         return false;
      if ( o == null )
         return false;
      return Arrays.equals(((ScriptFragmentImpl)o).byteArray,byteArray);
   }

   public InstructionInputStream getInstructionInput()
   {
      return new InstructionInputStream(new ByteArrayInputStream(byteArray));
   }

   public ScriptFragment getSubscript(byte[]... sigs)
      throws ScriptException
   {
      try
      {
         ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
         InstructionOutputStream output = new InstructionOutputStream(byteOutput);
         InstructionInputStream input = getInstructionInput();
         Instruction instruction = null;
         while ( (instruction=input.readInstruction()) != null )
         {
            if ( instruction.getData() == null )
            {
               // There is no parameter, so if this is not an OP_CODESEPARATOR copy
               // instruction to the output
               if ( instruction.getOperation() != Operation.OP_CODESEPARATOR )
                  output.writeInstruction(instruction);
            }
            else
            {
               // If there is data then filter out the sigs (if there are any)
               boolean matches = false;
               if ( sigs != null )
                  for ( int i=0; (i<sigs.length) && (!matches); i++ )
                     matches |= Arrays.equals(sigs[i],instruction.getData());
               // Copy only if it was not matched, but preserve the instruction itself
               // which acutally invalidates the script (you can't parse this)
               output.writeInstruction(instruction, matches);
            }
         }
         return new ScriptFragmentImpl(byteOutput.toByteArray());
      } catch ( IOException e ) {
         throw new ScriptException("exception while generating subscript",e);
      }
   }

   /**
    * Display the script in a readable format.
    */
   public String toString()
   {
      try
      {
         StringBuilder builder = new StringBuilder();
         InstructionInputStream input = getInstructionInput();
         Instruction instruction = null;
         while ( (instruction=input.readInstruction()) != null )
         {
            // Put space after previous instruction
            if ( builder.length() > 0 )
               builder.append(' ');
            // Write out operation
            builder.append(instruction.getOperation());
            // Write out data parameter if it has any
            if ( instruction.getData() != null )
            {
               builder.append(" <");
               for ( int i=0; i<instruction.getData().length; i++ )
               {
                  builder.append(Integer.toString(instruction.getData()[i] & 0xff,16));
                  builder.append(' ');
               }
               builder.setCharAt(builder.length()-1,'>');
            }
         }
         return builder.toString();
      } catch ( IOException e ) {
         logger.error("error with script couldn't generate string representation",e);
         return "[Script error: "+e.getMessage()+"]";
      }
   }
}

