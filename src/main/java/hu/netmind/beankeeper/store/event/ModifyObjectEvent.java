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

package hu.netmind.beankeeper.store.event;

import hu.netmind.beankeeper.model.ClassInfo;
import hu.netmind.beankeeper.model.ClassTracker;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.parser.TimeControl;
import hu.netmind.beankeeper.object.ObjectTracker;
import hu.netmind.beankeeper.type.TypeHandlerTracker;
import hu.netmind.beankeeper.query.QueryService;
import java.util.Map;
import java.util.Arrays;
import org.apache.log4j.Logger;

/**
 * This event is generated if an object's attributes changed.
 * @author Brautigam Robert
 * @version CVS Revision: $Revision$
 */
public class ModifyObjectEvent extends ObjectEvent
{
   private static Logger logger = Logger.getLogger(ModifyObjectEvent.class);

   private TimeControl timeControl = null;
   private Object originalObject = null;
   private Map originalAttributes;

   private ClassTracker classTracker = null;
   private ObjectTracker objectTracker = null;
   private TypeHandlerTracker typeHandlerTracker = null;
   private QueryService queryService = null;

   public ModifyObjectEvent(ClassTracker classTracker, ObjectTracker objectTracker, 
         TypeHandlerTracker typeHandlerTracker, QueryService queryService,
         Map originalAttributes, TimeControl timeControl, Object object)
   {
      super(object);
      this.originalAttributes=originalAttributes;
      this.classTracker=classTracker;
      this.objectTracker=objectTracker;
      this.timeControl=new TimeControl(timeControl);
      this.queryService=queryService;
      this.typeHandlerTracker=typeHandlerTracker;
      // Set the time control to the previous instant, because
      // in the given instant the object is already modified!
      this.timeControl.setSerial(new Long(this.timeControl.getSerial().longValue()-1));
   }

   /**
    * Determine whether an original attribute was equals to the given
    * value. If the value is not a primitive type, it is considered
    * the old value if they have the same identity. This method is
    * inexpensive unlike <code>getOriginalObject()</code>, so use
    * this if possible.
    * @param attributeName The attribute to check, case insensitive.
    * @param value The value.
    * @throws StoreException If container types are tried to be compared.
    */
   public boolean isOriginalValue(String attributeName, Object value)
   {
      ClassInfo info = classTracker.getClassInfo(getObject().getClass(),getObject());
      Object originalValue = originalAttributes.get(attributeName.toLowerCase());
      // Check nulls
      if ( (originalValue==null) && (value==null) )
         return true;
      if ( ((originalValue!=null) && (value==null)) ||
           ((originalValue==null) && (value!=null)) )
         return false;
      // Check handled types
      Class type = info.getAttributeType(attributeName);
      if ( typeHandlerTracker.isHandled(type) )
         throw new StoreException("container types can not be compared against their original values, try getOriginalObject() for that");
      // Check byte array
      if ( originalValue instanceof byte[] )
      {
         boolean result = Arrays.equals((byte[]) originalValue, (byte[]) value);
         return result;
      }
      // Check custom objects, in which case the originalValue is a persistence_id,
      // and the 'value' is an object.
      if ( classTracker.getType(value.getClass()) == ClassTracker.ClassType.TYPE_OBJECT )
         return objectTracker.getIdentifier(value).equals(originalValue);
      // Fallback to equals()
      return value.equals(originalValue);
   }

   /**
    * Get the original object before the change occured. Note, that
    * this method selects the object from database, so this should
    * be considered an expensive operation. Use with caution.
    */
   public Object getOriginalObject()
   {
      if ( originalObject == null )
      {
         logger.debug("selecting original object at "+timeControl);
         originalObject = queryService.findSingle("find obj("+getObject().getClass().getName()+") where obj = ? at ?",
               new Object[] { getObject(), timeControl });
      }
      return originalObject;

   }
}


