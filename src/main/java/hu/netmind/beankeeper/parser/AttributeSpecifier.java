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
 * Specifies a simple attribute (part of a term). It has three attributes:
 * <ul>
 *    <li><strong>identifier</strong>: The identifier of the attribute (name).</li>
 *    <li><strong>className</strong>: The class of this attribute as given
 *    by the term.</li>
 *    <li><strong>keyname</strong>: If this attribute is a map, then there
 *    can be a key name to this attribute.</li>
 * </ul>
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class AttributeSpecifier
{
   private String identifier;
   private String className;
   private String keyname;

   public AttributeSpecifier(String identifier, String className, String keyname)
   {
      setIdentifier(identifier);
      setClassName(className);
      setKeyname(keyname);
   }

   public String getIdentifier()
   {
      return identifier;
   }
   public void setIdentifier(String identifier)
   {
      this.identifier=identifier;
   }

   public String getClassName()
   {
      return className;
   }
   public void setClassName(String className)
   {
      this.className=className;
   }

   public String getKeyname()
   {
      return keyname;
   }
   public void setKeyname(String keyname)
   {
      this.keyname=keyname;
   }

}


