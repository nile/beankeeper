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

import org.apache.log4j.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.model.*;

/**
 * Null handler is a class handler for interfaces and reserved classes.
 * It contains no attributes, so all methods return default values 
 * statically.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class StrictNullHandler implements StrictClassHandler
{
   private static Logger logger = Logger.getLogger(StrictNullHandler.class);
   
   private ClassEntry sourceEntry;

   StrictNullHandler(ClassEntry sourceEntry)
   {
      // Init
      this.sourceEntry=sourceEntry;
   }

   public ClassEntry getSourceEntry()
   {
      return sourceEntry;
   }

   public boolean hasChanged()
   {
      return false;
   }

   public void update()
   {
   }

   public Map getAttributeTypes()
   {
      return new HashMap();
   }

   public List getAttributeNames()
   {
      return new ArrayList();
   }

   /**
    * Always throws exception.
    */
   public Object getAttributeValue(Object obj, String attributeName)
   {
      throw new StoreException("object value cannot be get, name: "+attributeName+" in nullhandler for: "+sourceEntry);
   }

   /**
    * Always returns exception.
    */
   public void setAttributeValue(Object obj, String attributeName, Object value)
   {
      throw new StoreException("object value cannot be set, objectclass: "+obj.getClass()+" name: "+attributeName+" on nullhandler for type: "+sourceEntry);
   }

}


