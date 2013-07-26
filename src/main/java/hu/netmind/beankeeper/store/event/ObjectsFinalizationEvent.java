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

package hu.netmind.beankeeper.store.event;

import hu.netmind.beankeeper.object.PersistenceMetaData;
import hu.netmind.beankeeper.event.PersistenceEvent;
import java.util.List;

/**
 * This event is delivered when the changed objects are finalized in the database
 * as part of the commit sequence. At this point there is still a chance that
 * the commit will fail. This event is delivered on all nodes for all changes.
 * @author Robert Brautigam
 * @version CVS Revision: $Revision$
 */
public class ObjectsFinalizationEvent implements PersistenceEvent
{
   private List<PersistenceMetaData> metas = null;
   private Long txSerial = null;
   private Long serial = null;

   public ObjectsFinalizationEvent(List<PersistenceMetaData> metas, Long serial, Long txSerial)
   {
      this.metas=metas;
      this.serial=serial;
      this.txSerial=txSerial;
   }

   public List<PersistenceMetaData> getMetas()
   {
      return metas;
   }

   public Long getSerial()
   {
      return serial;
   }

   public Long getTxSerial()
   {
      return txSerial;
   }
}

