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
 * This factory can be used to create scripts from fragments.
 * @author Robert Brautigam
 */
public interface ScriptFactory
{
   /**
    * Parse a byte array representation of a script fragment.
    */
   ScriptFragment createFragment(byte[] byteArray);

   /**
    * Create a script from script fragments by concatenating them.
    */
   Script createScript(ScriptFragment... fragments);
}

