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

package hu.netmind.beankeeper.transaction;

/**
 * Keeps statistics of database usage in the transaction.<br>
 * Note: Operations are thread-safe.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class TransactionStatistics
{
   private int updateCount;
   private int insertCount;
   private int selectCount;
   private int deleteCount;
   private int schemaCount;

   private long updateTime;
   private long insertTime;
   private long selectTime;
   private long deleteTime;
   private long schemaTime;

   public TransactionStatistics()
   {
      reset();
   }

   public TransactionStatistics(int updateCount, int insertCount,
         int selectCount, int deleteCount, int schemaCount)
   {
      this.updateCount=updateCount;
      this.insertCount=insertCount;
      this.selectCount=selectCount;
      this.deleteCount=deleteCount;
      this.schemaCount=schemaCount;
   }

   public synchronized void add(TransactionStatistics stats)
   {
      this.updateCount+=stats.updateCount;
      this.insertCount+=stats.insertCount;
      this.selectCount+=stats.selectCount;
      this.deleteCount+=stats.deleteCount;
      this.schemaCount+=stats.schemaCount;
      this.updateTime+=stats.updateTime;
      this.insertTime+=stats.insertTime;
      this.selectTime+=stats.selectTime;
      this.deleteTime+=stats.deleteTime;
      this.schemaTime+=stats.schemaTime;
   }

   public synchronized void substract(TransactionStatistics stats)
   {
      this.updateCount-=stats.updateCount;
      this.insertCount-=stats.insertCount;
      this.selectCount-=stats.selectCount;
      this.deleteCount-=stats.deleteCount;
      this.schemaCount-=stats.schemaCount;
      this.updateTime-=stats.updateTime;
      this.insertTime-=stats.insertTime;
      this.selectTime-=stats.selectTime;
      this.deleteTime-=stats.deleteTime;
      this.schemaTime-=stats.schemaTime;
   }

   public synchronized void reset()
   {
      updateCount=0;
      insertCount=0;
      selectCount=0;
      deleteCount=0;
      schemaCount=0;
   }

   public int getTotalCount()
   {
      return getUpdateCount()+getInsertCount()+getSelectCount()+
         getDeleteCount()+getSchemaCount();
   }

   public int getUpdateCount()
   {
      return updateCount;
   }
   public void setUpdateCount(int updateCount)
   {
      this.updateCount=updateCount;
   }

   public int getInsertCount()
   {
      return insertCount;
   }
   public void setInsertCount(int insertCount)
   {
      this.insertCount=insertCount;
   }

   public int getSelectCount()
   {
      return selectCount;
   }
   public void setSelectCount(int selectCount)
   {
      this.selectCount=selectCount;
   }

   public int getDeleteCount()
   {
      return deleteCount;
   }
   public void setDeleteCount(int deleteCount)
   {
      this.deleteCount=deleteCount;
   }

   public int getSchemaCount()
   {
      return schemaCount;
   }
   public void setSchemaCount(int schemaCount)
   {
      this.schemaCount=schemaCount;
   }

   public long getUpdateTime()
   {
      return updateTime;
   }
   public void setUpdateTime(long updateTime)
   {
      this.updateTime=updateTime;
   }

   public long getInsertTime()
   {
      return insertTime;
   }
   public void setInsertTime(long insertTime)
   {
      this.insertTime=insertTime;
   }

   public long getSelectTime()
   {
      return selectTime;
   }
   public void setSelectTime(long selectTime)
   {
      this.selectTime=selectTime;
   }

   public long getDeleteTime()
   {
      return deleteTime;
   }
   public void setDeleteTime(long deleteTime)
   {
      this.deleteTime=deleteTime;
   }

   public long getSchemaTime()
   {
      return schemaTime;
   }
   public void setSchemaTime(long schemaTime)
   {
      this.schemaTime=schemaTime;
   }

   public String toString()
   {
      return "[Stats: U:"+updateCount+" I:"+insertCount+" D:"+deleteCount+" T:"+schemaCount+" S:"+selectCount+"]";
   }
}


