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

import it.nibbles.javacoin.KeyFactory;
import it.nibbles.javacoin.ScriptFactory;
import it.nibbles.javacoin.ScriptFragment;
import it.nibbles.javacoin.Script;

/**
 * Use this factory to manage scripts.
 * @author Robert Brautigam, Alessandro Polverini
 */
public class ScriptFactoryImpl implements ScriptFactory
{
   private KeyFactory keyFactory = null;

   /**
    * Create a factory with the given key factory. The key factory is used
    * to deserialize public keys and verify signatures.
    */
   public ScriptFactoryImpl(KeyFactory keyFactory)
   {
      this.keyFactory=keyFactory;
   }

   public ScriptFragment createFragment(byte[] byteArray)
   {
      return new ScriptFragmentImpl(byteArray);
   }

   public Script createScript(ScriptFragment sigScript, ScriptFragment pubScript)
   {
      return new ScriptImpl((ScriptFragmentImpl)sigScript, (ScriptFragmentImpl)pubScript, keyFactory);
   }
}

