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

package hu.netmind.bitcoin;

/**
 * A fragment of a script. A fragment alone can not be executed, it has to
 * be completed by other fragments.
 * @author Robert Brautigam
 */
public interface ScriptFragment
{
   /**
    * Get the "subscript" from this fragment of script. A subscript is needed
    * for correctly hashing input (with the script fragment of the output).
    * This method removes the given signatures from the script fragment 
    * (which normally shouldn't be a part of the output script), and all
    * OP_CODESEPARATOR operations. Note that after these operations the
    * fragment might not be a valid script anymore, so it only an array of bytes.
    * @param sigs Any number of signatures to be removed from this fragment.
    * @return The bytes remaining after removing the given signatures and all
    * OP_CODESEPARATOR operations.
    */
   byte[] getSubscript(byte[]... sigs)
      throws ScriptException;


   /**
    * Convert this fragment in byte code.
    */
   byte[] toByteArray();
}

