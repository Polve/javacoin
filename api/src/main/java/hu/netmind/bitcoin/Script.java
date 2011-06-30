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
 * A script which can be run to authorize the spending of a
 * transaction output by a transaction input. It is a concatenation
 * of the signature script in the transaction input and the script in the
 * transaction output.
 * @author Robert Brautigam
 */
public interface Script extends ScriptFragment
{
   /**
    * Execute the script and provide the output decision whether
    * spending of the given txIn is authorized.
    * @param txIn The input to verify with this script.
    * @return True if spending is approved by this script, false otherwise.
    * @throws ScriptException If script can not be executed, or is an invalid script.
    */
   boolean execute(TransactionInput txIn)
      throws ScriptException;
}

