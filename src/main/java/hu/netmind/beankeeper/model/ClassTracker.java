/**
 * Copyright (C) 2006 NetMind Consulting Bt.
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

import hu.netmind.beankeeper.service.Service;
import java.util.Map;
import java.util.List;

/**
 * This class keeps track of different classes and objects. It's main
 * purpose is to implement object reflection based logic, and provide
 * type information.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface ClassTracker extends Service
{
   /**
    * This enumeration represents the class type of a class.
    */
   public enum ClassType
   {
      TYPE_NONE,        // Extremal type
      TYPE_PRIMITIVE,   // It's Java primitive, String, Date or byte array
      TYPE_HANDLED,     // Handled by type handlers (Map, Set, etc.)
      TYPE_OBJECT,      // Plain class with attributes (user implementation)
      TYPE_JAVA,        // Class from the java JDK
      TYPE_RESERVED,    // Class from other reserved places
   }

   /**
    * Get the type id of given class.
    */
   ClassType getType(Class clazz);

   /**
    * Get class info for a class entry.
    */
   ClassInfo getClassInfo(ClassEntry entry);

   /**
    * Get class info for an object and it's class.
    */
   ClassInfo getClassInfo(Class clazz, Object obj);

   /**
    * Get class info from a string.
    */
   ClassInfo getClassInfo(String className, String dynamicName);

   /**
    * Get class information object of given class. Also, if class 
    * information does not exist, it will be created.
    */
   ClassInfo getClassInfo(Class clazz, String dynamicName);

   /**
    * Get all related classes to the given entry. Related class entries are
    * all given class' super- and sub-classes which are all storable.
    * Calling this method on non-storable entry will result in an undefined
    * result.
    */
   List<ClassEntry> getRelatedClassEntries(ClassEntry entry);

   /**
    * Get all subclasses of given entry, including itself.
    */
   List<ClassEntry> getSubClasses(ClassEntry entry);

   /**
    * Get all storable roots for given entry. A storable root for a
    * storable entry is itself. A non-storable entry (such as java.lang.Object)
    * will have potentially a lot of storable roots: All classes in the
    * class hierarchy which are storable, but have non-storable superclasses.
    * So, storable roots are the first storable entry in a class hierarchy
    * path (roots of the storable sub-forest). When a query is received 
    * for a non-stored class, the query will split into queries for all
    * storable roots.
    */
   List<ClassEntry> getStorableRootClassEntries(ClassEntry entry);

   /**
    * Get a class entry for a class id.
    */
   ClassEntry getClassEntry(Integer id);

   /**
    * Get a Class instance for a class name postfix. The given parameter
    * is treated as a postfix for a fully qualified class name. The postfix
    * is considered matching, when it contains whole class of package
    * qualifiers. For example: "book" matches "hu.netmind.beankeeper.Book"
    * class, but does not match "hu.netmind.beankeeper.CookBook". Also
    * "persistence.book" matches "hu.netmind.beankeeper.Book", but 
    * "tence.book" does not match to previous class.<br>
    * If no classes are found null is returned. If more than one matching
    * class is present, then one of them is returned (no guarantees which
    * one is picked).
    * @param postfix The class name postfix.
    * @return The class info for which the postfix applies, or null.
    */
   ClassEntry getMatchingClassEntry(String postfix);

   /**
    * Get the next object id for an object of given entry. This id
    * is guaranteed to be unique across all objects in the database.
    */
   Long getNextId(ClassEntry entry);
}


