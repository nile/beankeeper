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

package hu.netmind.beankeeper.type.impl;

import java.util.*;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.service.StoreContext;
import hu.netmind.beankeeper.parser.*;
import hu.netmind.beankeeper.model.*;
import hu.netmind.beankeeper.transaction.Transaction;
import hu.netmind.beankeeper.transaction.TransactionTracker;
import org.apache.log4j.Logger;

/**
 * This is a generic container handler. It manages the ContainerImpl interface.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public abstract class ContainerHandler extends AbstractTypeHandler
{
   private static Logger logger = Logger.getLogger(ContainerHandler.class);

   private StoreContext context = null; // Injected
   private TransactionTracker transactionTracker = null; // Injected
   
   /**
    * Get the attribute types that are representing this type
    * in the original object.
    */
   public Map getAttributeTypes(String attributeName)
   {
      // Return the attributes in the original object.
      HashMap attributeTypes = new HashMap();
      attributeTypes.put(attributeName,Long.class);
      attributeTypes.put(attributeName+"_itemclass",String.class);
      return attributeTypes;
   }

   /**
    * Save the collection.
    */
   public Object save(ClassInfo classInfo, Object current, String attributeName,
         Transaction transaction, Long currentSerial,
         Object newValue, Set waitingObjects, Set saveTables, Set removeTables,
         List events, Map changedAttributes, Map dbAttributes)
   {
      Long lastSerial = (Long) dbAttributes.get(attributeName);
      // Determine what to do with the null-indicator inside the
      // parent object. Keep in mind, that something changed!
      if ( lastSerial == null )
      {
         // If current list is null, we assume that
         // the new list is not null, so insert current serial as last
         // to indicate the list will be non-null, and to remember version.
         // If the new list is also null, the next condition will
         // catch it.
         changedAttributes.put(attributeName,currentSerial);
      }
      if ( newValue == null )
      {
         // If list is null, we assume that
         // previously it was non-null so insert 'null'
         // to indicate it should be null.
         changedAttributes.put(attributeName,null);
         changedAttributes.put(attributeName+"_itemclass",null);
      }
      // Save collection
      String itemClassName = (String) dbAttributes.get(attributeName+"_itemclass");
      if ( newValue == null )
      {
         logger.debug("saving container, new value is null.");
         // If new value is null, then set the attribute to null
         // on tracker, and clear all current values.
         if ( lastSerial != null )
         {
            // Clean all values of the current (obsolate list)
            Map pseudoAttributes = new HashMap();
            pseudoAttributes.put(attributeName,currentSerial);
            pseudoAttributes.put(attributeName+"_itemclass",itemClassName);
            TimeControl currentListTimeControl =
               new TimeControl(currentSerial,transaction.getSerial(),true);
            Container currentContainer = (Container) 
               unmarshallType(classInfo,current,attributeName,pseudoAttributes,currentListTimeControl);
            currentContainer.clear();
            currentContainer.save(transaction,currentSerial,waitingObjects,
                  saveTables, removeTables, events);
         }
      } else if ( (newValue instanceof Container) && (((Container)newValue).getParent()==current) && 
            (((Container)newValue).getLastSerial().equals(lastSerial)) ) {
         logger.debug("saving container, value is current.");
         // If the value is the original value, and it is current,
         // then simply save. Attribute stays the same.
         ((Container)newValue).save(transaction,currentSerial,waitingObjects,
            saveTables, removeTables, events);
         changedAttributes.put(attributeName,currentSerial);
         changedAttributes.put(attributeName+"_itemclass",((Container)newValue).getItemClassName());
      } else {
         if ( logger.isDebugEnabled() )
            if ( newValue instanceof Container )
               logger.debug("diffing container, because stayed by parent: "+
                     (((Container)newValue).getParent()==current)+", serials "+lastSerial+" vs. "+((Container)newValue).getLastSerial());
            else
               logger.debug("diffing container, because new value class: "+newValue.getClass().getName());
         // We must load the current list. The current list is an empty list,
         // even if the real current list would be null.
         Map pseudoAttributes = new HashMap();
         pseudoAttributes.put(attributeName,currentSerial);
         pseudoAttributes.put(attributeName+"_itemclass",itemClassName);
         TimeControl currentListTimeControl =
            new TimeControl(currentSerial,transaction.getSerial(),true);
         Container currentList = (Container) unmarshallType(classInfo,current,attributeName,pseudoAttributes,currentListTimeControl);
         if ( logger.isDebugEnabled() )
            logger.debug("saving container, selected current list: "+currentList.size()+", tracked value time control: "+currentListTimeControl);
         // Diff to the current list, and save the changes.
         logger.debug("diffing containers (clean)...");
         if ( lastSerial != null )
            currentList.clear(); // Delete entries if the container was not null
         logger.debug("diffing containers (add)...");
         currentList.addAll(newValue); // Add new entries
         // Save
         logger.debug("diffing containers (save)...");
         currentList.save(transaction,currentSerial,waitingObjects,
               saveTables, removeTables, events);
         // If all went well, update attribute. It is important, that
         // this list will not be referenced prior to the completion of
         // save operation, because inserted item may not yet exist at
         // this point.
         changedAttributes.put(attributeName,currentSerial);
         changedAttributes.put(attributeName+"_itemclass",((Container)currentList).getItemClassName());
         logger.debug("diffing containers finished, final container item class is: "+((Container)currentList).getItemClassName());
         newValue = currentList;
         // Inject list into object, so next modification can be optimized
         classInfo.setAttributeValue(current,attributeName,currentList);
      }
      return newValue;
   }

   public abstract Class getContainerClass();
   
   /**
    * Return a new instance of Container.
    */
   public Object unmarshallType(ClassInfo classInfo, Object obj,
         String attributeName, Map marshalledValues, TimeControl timeControl)
   {
      // First, if the attribute is false, then return null! 
      Long lastSerial = (Long) marshalledValues.get(attributeName);
      String itemClassName = (String) marshalledValues.get(attributeName+"_itemclass");
      if ( lastSerial == null )
         return null;
      // Return with implementation
      try
      {
         Container impl = (Container) getContainerClass().newInstance();
         context.injectServices(impl);
         impl.init(classInfo, obj, attributeName, itemClassName, lastSerial, timeControl);
         return impl;
      } catch ( Throwable e ) {
         throw new StoreException("could not instantiate collection implementation: "+getContainerClass(),e);
      }
   }
   
   /**
    * Determine whether the two values differ.
    */
   public boolean hasChanged(ClassInfo info, Object obj, String attributeName, Map dbAttributes, Long currentSerial)
   {
      logger.debug("determining whether container attribute: "+attributeName+" changed.");
      // First get old and new values, and serial
      Object newValue = info.getAttributeValue(obj,attributeName);
      Long lastSerial = (Long) dbAttributes.get(attributeName);
      // If the new value is a Container, we can use a lot of extra
      // information, so treat it differently.
      if ( newValue instanceof Container )
      {
         // Not changed if:
         // - new value is for the 'parent' (max. another instance, or version)
         // - value is current (same as in db)
         // - has not changed internally
         boolean result = ! ((((Container)newValue).getParent()==obj) && 
           ( ((Container)newValue).getLastSerial().equals(lastSerial) ) &&
           ( ! ((Container)newValue).hasChanged() ));
         logger.debug("new value is also container, changed: "+result);
         return result;
      } else {
         // Handle nulls
         if ( lastSerial==null )
         {
            logger.debug("last serial is null, new value is null: "+(newValue==null));
            return newValue!=null;
         }
         // New value is not a container, so we must compare to the current
         // container to know for sure.
         Transaction tx = transactionTracker.getTransaction(TransactionTracker.TX_OPTIONAL);
         Long txSerial = null;
         if ( tx != null )
            txSerial = tx.getSerial();
         TimeControl currentControl = new TimeControl(currentSerial,txSerial,txSerial==null?false:true);
         Container currentImpl = (Container) unmarshallType(info,obj,attributeName,dbAttributes,currentControl);
         boolean result = ! currentImpl.equals(newValue);
         logger.debug("new value is not a container impl, compared to current container, changed: "+result);
         return result;
      }
   }
   
   public void postSave(Object value)
   {
      if ( value != null )
         ((Container) value).reload();
   }
}


