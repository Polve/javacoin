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

package it.nibbles.javacoin;

import java.util.Observer;

/**
 * An interface for services that can be observed. Unfortunately the Java
 * class of same name is not an interface.
 * @author Robert Brautigam
 */
public interface Observable
{
   /**
    * Add an observer to this class.
    */
   void addObserver(Observer observer);

   /**
    * Remove an observer.
    */
   void deleteObserver(Observer observer);
}

