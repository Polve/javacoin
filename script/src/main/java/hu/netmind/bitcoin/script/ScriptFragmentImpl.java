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
import java.io.ByteArrayInputStream;
import java.io.IOException;
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

   public InstructionInputStream getInstructionInput()
   {
      return new InstructionInputStream(new ByteArrayInputStream(byteArray));
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

