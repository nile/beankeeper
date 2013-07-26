/**
 * Copyright (C) 2009 NetMind Consulting Bt.
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

package hu.netmind.beankeeper.model;

import java.util.Map;
import java.util.List;

/**
 * Information and operations about a given class.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface ClassInfo
{
   /**
    * Get the attribute types for the given superclass specifically.
    */
   Map<String,Class> getAttributeTypes(ClassEntry entry);

   /**
    * Get all classes and superclasses of the source class.
    */
   List<ClassEntry> getClassEntries();

   /**
    * Get the attributes declared in the superclass given.
    */
   List<String> getAttributeNames(ClassEntry entry);

   /**
    * Instantiate an object of this info entity. If the object in question
    * is a dynamic object, it's dynamic name will be set.
    * @param marshalledValues The values for which this object will be created.
    */
   Object newInstance(Map marshalledValues)
      throws InstantiationException, IllegalAccessException;

   /**
    * Get the exact class a member attribute is declared in.
    * @param attributeName The attribute to search for.
    * @return The class the attribute is declared in, which is either the
    * source class, or a superclass, or null if no such attribute is found.
    */
   ClassEntry getAttributeClassEntry(String attributeName);

   /**
    * Get all the attribute names in a list of this class.
    */
   List<String> getAttributeNames();

   /**
    * Get the type of the attribute given.
    * @return The type marker as defined in ClassTracker.
    */
   Class getAttributeType(String attributeName);

   /**
    * Get the attribute value from a given object of this class and 
    * from given attribute.
    */
   Object getAttributeValue(Object obj, String attributeName);

   /**
    * Set an object as value into object given.
    */
   void setAttributeValue(Object obj, String attributeName, Object value);

   /**
    * Get the entry this info is made of. This is the top entry of this
    * class info, that means this info contains this class' and all of
    * it's superclass' information.
    */
   ClassEntry getSourceEntry();

   /**
    * Return whether this class is storable or not.
    * @return True, if it is a primitive type, or a non-abstract class,
    * or has member fields and it's not in a java.* package.
    */
   boolean isStorable();

   /**
    * Return whether this class is a primitive type or not.
    * @return True, if this class is a primitive type (boxed primitive, or
    * Date or String or array).
    */
   boolean isPrimitive();

   /**
    * Return whether class can have attributes or not.
    * @return True, if class will never have attributes. This is true
    * if it's from java.* but not primitive or an interface.
    */
   boolean isEmpty();

   /**
    * Return whether class has dynamic attributes.
    */
   boolean hasDynamicAttributes();

   /**
    * Return whether this class has static (member field) attributes.
    */
   boolean hasStaticAttributes();

   /**
    * Determine whether this class has changed structure. This is
    * only possible if the class is dynamic.
    */
   boolean hasChanged();

   /**
    * Update the structure now. This has no effect, if class did
    * not change. If it has, than after this operation
    * <code>hasChanged()</code> should return false.
    */
   void update();
}


