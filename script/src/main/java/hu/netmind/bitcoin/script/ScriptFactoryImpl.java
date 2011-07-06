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

import hu.netmind.bitcoin.KeyFactory;
import hu.netmind.bitcoin.ScriptFactory;
import hu.netmind.bitcoin.ScriptFragment;
import hu.netmind.bitcoin.Script;

/**
 * Use this factory to manage scripts.
 * @author Robert Brautigam
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
      // Copy together the two scripts
      byte[] scriptBytes = new byte[sigScript.toByteArray().length+pubScript.toByteArray().length];
      System.arraycopy(sigScript.toByteArray(),0,scriptBytes,0,sigScript.toByteArray().length);
      System.arraycopy(pubScript.toByteArray(),0,scriptBytes,sigScript.toByteArray().length,pubScript.toByteArray().length);
      // Create script
      return new ScriptImpl(scriptBytes,keyFactory);
   }
}

