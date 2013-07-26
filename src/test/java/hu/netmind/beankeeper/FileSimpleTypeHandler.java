/**
 * Copyright (C) 2010 NetMind Consulting Bt.
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

package hu.netmind.beankeeper;

import java.io.File;
import hu.netmind.beankeeper.type.impl.SimpleTypeHandler;

/**
 * Handles "File" objects as attributes.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class FileSimpleTypeHandler extends SimpleTypeHandler
{
   public Class getPrimitiveType()
   {
      return String.class;
   }

   public Object unmarshallType(Object primitiveValue)
   {
      return new File((String) primitiveValue);
   }

   public Object marshallType(Object obj)
   {
      return ((File) obj).getAbsolutePath();
   }
}


