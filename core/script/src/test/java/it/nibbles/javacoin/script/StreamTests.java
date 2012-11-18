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

package it.nibbles.javacoin.script;

import org.testng.annotations.Test;
import org.testng.Assert;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * @author Robert Brautigam
 */
@Test
public class StreamTests
{
   public void testStreamConcept()
      throws IOException
   {
      byte[] data = HexUtil.toByteArray("01 02 03 04 05 06 07 08");
      // Generate a script which just contains every operation
      ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
      InstructionOutputStream output = new InstructionOutputStream(byteOutput);
      for ( Operation op : Operation.values() )
      {
         if ( (op==Operation.CONSTANT) || (op==Operation.OP_PUSHDATA1) ||
              (op==Operation.OP_PUSHDATA2) || (op==Operation.OP_PUSHDATA4) )
            output.writeInstruction(new Instruction(op,data));
         else
            output.writeInstruction(new Instruction(op,null));
      }
      // Now with the help of the input stream copy it to another output
      InstructionInputStream input = new InstructionInputStream(
            new ByteArrayInputStream(byteOutput.toByteArray()));
      ByteArrayOutputStream byteOutput2 = new ByteArrayOutputStream();
      InstructionOutputStream output2 = new InstructionOutputStream(byteOutput2);
      Instruction instruction = null;
      while ( (instruction=input.readInstruction()) != null )
         output2.writeInstruction(instruction);
      // Compare the result is exactly the same
      Assert.assertEquals(byteOutput2.toByteArray(),byteOutput.toByteArray());
   }
}

