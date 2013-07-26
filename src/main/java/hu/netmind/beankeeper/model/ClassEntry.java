/**
 * Copyright (C) 2007 NetMind Consulting Bt.
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

import java.lang.reflect.Modifier;
import java.io.Serializable;

/**
 * This class describes a persistable class. This is a finer grained
 * thing than static classes, because the library allows for dynamic
 * classes (classnames) to be created. So an unambigous description of
 * a persistable class is it's class, and the dynamic name.<br>
 * If the dynamic name is not given, the class is considered a static
 * class, these are the normal Java classes. If the dynamic name is
 * given, the class is considered a dynamic class, which has no
 * actual class in the JVM, but it's also considered a subclass of the
 * root static class contained.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ClassEntry implements Serializable
{
   private Class clazz;
   private String dynamicName;
   private String fullName;

   public ClassEntry(Object obj)
   {
      this(obj.getClass(),(obj instanceof DynamicObject)?
            ((DynamicObject)obj).getPersistenceDynamicName():null);
   }

   public ClassEntry(Class clazz, String dynamicName)
   {
      this.clazz=clazz;
      this.dynamicName=dynamicName;
      if ( (!DynamicObject.class.isAssignableFrom(clazz)) || 
            (dynamicName==null) || (dynamicName.length()==0) ||
            (!Character.isLetter(dynamicName.charAt(0))) )
         this.dynamicName=null;
      if ( dynamicName == null )
         fullName = clazz.getName();
      else
         fullName = clazz.getName()+"."+dynamicName;
   }

   public ClassEntry getSuperEntry()
   {
      ClassEntry superEntry = null;
      if ( getDynamicName() != null )
      {
         // Dynamic class
         superEntry = new ClassEntry(getSourceClass(),null);
      } else {
         if ( getSourceClass().getSuperclass() != null )
            superEntry = new ClassEntry(getSourceClass().getSuperclass(),null);
      }
      return superEntry;
   }

   public Class getSourceClass()
   {
      return clazz;
   }

   public String getDynamicName()
   {
      return dynamicName;
   }

   public String getFullName()
   {
      return fullName;
   }

   public String toString()
   {
      return "[ClassEntry: "+fullName+"]";
   }

   public int hashCode()
   {
      return fullName.hashCode();
   }

   public boolean equals(Object obj)
   {
      if ( ! (obj instanceof ClassEntry) )
         return false;
      return ((ClassEntry)obj).getFullName().equals(getFullName());
   }
}

