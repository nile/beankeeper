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

package hu.netmind.beankeeper.model.impl;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.model.*;

/**
 * Information and transformations on a given class.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ClassInfoImpl implements ClassInfo
{
   private static Logger logger = Logger.getLogger(ClassInfoImpl.class);
  
   private ClassEntry sourceEntry;
   private Map handlersByAttribute;
   private Map<ClassEntry,StrictClassHandler> handlersByEntry;
   private ArrayList classEntries;

   private ClassTracker classTracker = null; 

   public ClassInfoImpl(ClassTracker classTracker, ClassEntry sourceEntry)
   {
      this.classTracker=classTracker;
      this.sourceEntry=sourceEntry;
      // Assemble the attribute list
      handlersByAttribute = new HashMap();
      handlersByEntry = new HashMap<ClassEntry,StrictClassHandler>();
      classEntries = new ArrayList();
      // Assemble the attribute list
      ClassEntry localEntry = sourceEntry;
      while ( localEntry != null )
      {
         if ( logger.isDebugEnabled() )
            logger.debug("analyzing entry for class info: "+localEntry);
         // Add to all class entries, if it is storable
         if ( isStorable(localEntry) )
         {
            if ( logger.isDebugEnabled() )
               logger.debug("entry is storable, adding to class entries: "+classEntries);
            classEntries.add(localEntry);
         }
         // Determine handler for strict class, there are
         // four kinds of handlers:
         // - Null handler: has no attributes ever
         // - Static handler: attributes are determined from reflection
         // - Dynamic handler: attributes are determined with call to dynamic object
         // - Primitive handler: primitive types
         StrictClassHandler handler;
         if ( isEmpty(localEntry) )
            handler = new StrictNullHandler(localEntry);
         else if ( hasDynamicAttributes(localEntry) )
            handler = new StrictDynamicHandler(classTracker,localEntry);
         else if ( isPrimitive(localEntry) )
            handler = new StrictPrimitiveHandler(localEntry);
         else
            handler = new StrictStaticHandler(localEntry);
         logger.debug("handler for entry will be: "+handler);
         // Add this class' attribute to sets
         Map strictAttributeTypes = handler.getAttributeTypes();
         Iterator attributeTypeIterator = strictAttributeTypes.keySet().iterator();
         while ( attributeTypeIterator.hasNext() )
         {
            String attributeName = attributeTypeIterator.next().toString().toLowerCase();
            handlersByAttribute.put(attributeName,handler);
         }
         handlersByEntry.put(localEntry,handler);
         // Get super
         localEntry = localEntry.getSuperEntry();
      }
      logger.debug("analized class: "+sourceEntry+", attributes: "+getAttributeNames());
   }

   private StrictClassHandler getHandler(ClassEntry entry)
   {
      return (StrictClassHandler) handlersByEntry.get(entry);
   }
   
   private StrictClassHandler getHandler(String attributeName)
   {
      return (StrictClassHandler) handlersByAttribute.get(attributeName.toLowerCase());
   }
   
   /**
    * Get the attribute types for a strict class.
    */
   public Map getAttributeTypes(ClassEntry entry)
   {
      return getHandler(entry).getAttributeTypes();
   }

   /**
    * Get all classes and superclasses of the source class.
    */
   public List getClassEntries()
   {
      return classEntries;
   }

   /**
    * Get the attributes declared in the class given.
    */
   public List getAttributeNames(ClassEntry entry)
   {
      return getHandler(entry).getAttributeNames();
   }

   /**
    * Instantiate an object of this info entity. If the object in question
    * is a dynamic object, it's dynamic name will be set.
    * @param marshalledValues The values for which this object will be created.
    */
   public Object newInstance(Map marshalledValues)
      throws InstantiationException, IllegalAccessException
   {
      // If primitive type, then construct with primitive value
      if ( isPrimitive() )
         return ((StrictPrimitiveHandler)getHandler(getSourceEntry())).newInstance(marshalledValues);
      // It's a custom object, create it
      Object result = sourceEntry.getSourceClass().newInstance();
      // If it's a dynamic object, then set it's dynamic name
      if ( DynamicObject.class.isAssignableFrom(sourceEntry.getSourceClass()) )
         ((DynamicObject) result).setPersistenceDynamicName(sourceEntry.getDynamicName());
      return result;
   }

   /**
    * Get the exact class a member attribute is declared in.
    * @param attributeName The attribute to search for.
    * @return The class the attribute is declared in, which is either the
    * source class, or a superclass, or null if no such attribute is found.
    */
   public ClassEntry getAttributeClassEntry(String attributeName)
   {
      return getHandler(attributeName).getSourceEntry();
   }

   /**
    * Get the attribute names in a list of this class.
    */
   public List getAttributeNames()
   {
      return new ArrayList(handlersByAttribute.keySet());
   }

   /**
    * Get the type of the attribute given.
    * @return The type marker as defined in ClassTracker.
    */
   public Class getAttributeType(String attributeName)
   {
      StrictClassHandler handler = getHandler(attributeName);
      if ( handler == null )
         return null;
      return (Class) handler.getAttributeTypes().get(attributeName.toLowerCase());
   }

   /**
    * Get the attribute value from a given object of this class and 
    * from given attribute.
    */
   public Object getAttributeValue(Object obj, String attributeName)
   {
      return getHandler(attributeName).getAttributeValue(obj,attributeName);
   }

   /**
    * Set an object as value into object given.
    */
   public void setAttributeValue(Object obj, String attributeName, Object value)
   {
      getHandler(attributeName).setAttributeValue(obj,attributeName,value);
   }

   /**
    * Return whether the information changed in info class.
    */
   public boolean hasChanged()
   {
      Iterator handlerIterator = handlersByEntry.values().iterator();
      while ( handlerIterator.hasNext() )
      {
         StrictClassHandler handler = (StrictClassHandler) handlerIterator.next();
         if ( handler.hasChanged() )
            return true;
      }
      return false;
   }

   /**
    * Update, so that this class info reflects the current model.
    */
   public void update()
   {
      // Update all handlers and update attribute mapping
      Map handlersByAttributeNew = new HashMap();
      for ( StrictClassHandler handler : handlersByEntry.values() )
      {
         // Update the handler first
         handler.update();
         // Go through attribute names, and link it in
         for ( String attributeName : (List<String>)handler.getAttributeNames() )
            handlersByAttributeNew.put(attributeName,handler);
      }
      // Switch to new mapping
      handlersByAttribute = handlersByAttributeNew;
   }

   public ClassEntry getSourceEntry()
   {
      return sourceEntry;
   }

   public int hashCode()
   {
      return sourceEntry.getFullName().hashCode();
   }

   private boolean isPrimitive(ClassEntry entry)
   {
      return classTracker.getType(entry.getSourceClass()) == ClassTracker.ClassType.TYPE_PRIMITIVE;
   }
  
   public boolean isPrimitive()
   {
      return isPrimitive(getSourceEntry());
   }
  
   public boolean isEmpty(ClassEntry entry)
   {
      if ( isPrimitive(entry) )
         return false; // Primitive classes are saved
      if ( (entry.getSourceClass().isInterface()) || (entry.getSourceClass().getName().startsWith("java")) )
         return true; // Intefaces or java.** classes will never have attributes
      return false;
   }

   public boolean isEmpty()
   {
      return isEmpty(getSourceEntry());
   }

   private boolean isStorable(ClassEntry entry)
   {
      if ( isPrimitive(entry) )
         return true; // Primitive classes are saved
      if ( (entry.getSourceClass().isInterface()) || 
            (entry.getSourceClass().getName().startsWith("java")) )
         return false; // Intefaces or java.** classes are non-storable
      if ( (Modifier.isAbstract(entry.getSourceClass().getModifiers())) && (!hasStaticAttributes(entry)) )
         return false; // Abstract superclasses which have no attributes
      return true;
   }
   
   public boolean isStorable()
   {
      return isStorable(getSourceEntry());
   }

   public boolean hasDynamicAttributes(ClassEntry entry)
   {
      return StrictDynamicHandler.getPersistenceAttributeTypes(entry) != null;
   }

   public boolean hasDynamicAttributes()
   {
      return hasDynamicAttributes(getSourceEntry());
   }

   private boolean hasStaticAttributes(ClassEntry entry)
   {
      return StrictStaticHandler.hasStaticAttributes(entry);
   }

   public boolean hasStaticAttributes()
   {
      return hasStaticAttributes(getSourceEntry());
   }

   public boolean equals(Object obj)
   {
      if ( ! (obj instanceof ClassInfoImpl) )
         return false;
      return ((ClassInfoImpl) obj).sourceEntry.getFullName().equals(sourceEntry.getFullName());
   }

   public String toString()
   {
      return "[ClassInfo: "+sourceEntry.getFullName()+"]";
   }
}


