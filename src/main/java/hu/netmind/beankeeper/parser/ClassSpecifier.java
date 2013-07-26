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

package hu.netmind.beankeeper.parser;

/**
 * This class represents a class identifier.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class ClassSpecifier
{
   private String alias;
   private String className;

   public ClassSpecifier(String className, String alias)
   {
      setClassName(className);
      setAlias(alias);
   }

   public String getAlias()
   {
      return alias;
   }
   public void setAlias(String alias)
   {
      this.alias=alias;
   }

   public String getClassName()
   {
      return className;
   }
   public void setClassName(String className)
   {
      this.className=className;
   }

   public String toString()
   {
      return "[ClassSpecifier: "+className+" ("+(alias==null?"no alias":("'"+alias+"'"))+")]";
   }


}


