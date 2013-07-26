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

import hu.netmind.beankeeper.serial.Serial;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Keeps an exact moment in time for the query to run.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class TimeControl
{
   private static Logger logger = Logger.getLogger(TimeControl.class);
   
   private Long serial;
   private Long txSerial;
   private boolean applyTransaction = false;

   public TimeControl(TimeControl timeControl)
   {
      this.serial=timeControl.serial;
      this.txSerial=timeControl.txSerial;
      this.applyTransaction=timeControl.applyTransaction;
   }
   
   public TimeControl(Long serial, Long txSerial, boolean applyTransaction)
   {
      this.serial=serial;
      this.txSerial=txSerial;
      this.applyTransaction=applyTransaction;
   }
   
   public boolean isApplyTransaction()
   {
      return applyTransaction;
   }
   public void setApplyTransaction(boolean applyTransaction)
   {
      this.applyTransaction=applyTransaction;
   }

   public Long getSerial()
   {
      return serial;
   }
   public void setSerial(Long serial)
   {
      this.serial=serial;
   }

   public Long getTxSerial()
   {
      return txSerial;
   }
   public void setTxSerial(Long txSerial)
   {
      this.txSerial=txSerial;
   }

   public void setQueryDate(Date date)
   {
      serial = Serial.getSerial(date).getValue();
      if ( (txSerial!=null) && (serial.longValue() < txSerial.longValue() ) )
      {
         // Serial is before the tx's first modification took place,
         // so transaction conditions are not necessary
         setApplyTransaction(false);
      }
   }

   /**
    * Apply this time control to the table given.
    */
   public void apply(Expression expression, TableTerm term)
   {
      Expression timeExpr = new Expression();
      Long safeSerial = txSerial;
      if ( (safeSerial == null) || (safeSerial.longValue()>serial.longValue()) )
         safeSerial = serial;
      if ( logger.isDebugEnabled() )
         logger.debug("applying time control to: "+term+", safe serial: "+safeSerial+", txSerial: "+txSerial);
      // Apply the constraints, first the start serial.
      Expression partExpr = new Expression();
      if ( applyTransaction )
      {
         // If entry is inside the transaction, then we allow entries
         // inside the serial boundary.
         partExpr.add(new ReferenceTerm(term,"persistence_txstartid"));
         partExpr.add("=");
         partExpr.add(new ConstantTerm(txSerial));
         partExpr.add("and");
         partExpr.add(new ReferenceTerm(term,"persistence_txstart"));
         partExpr.add("<=");
         partExpr.add(new ConstantTerm(serial));
         partExpr.add("or");
      }
      partExpr.add(new ReferenceTerm(term,"persistence_start"));
      partExpr.add("<=");
      partExpr.add(new ConstantTerm(safeSerial));
      timeExpr.add(partExpr);
      timeExpr.add("and");
      // Now add end constraints. This is a little different, because
      // the end serials need to be enforced.
      partExpr = new Expression();
      partExpr.add(new ReferenceTerm(term,"persistence_end"));
      partExpr.add(">");
      partExpr.add(new ConstantTerm(safeSerial));
      if ( applyTransaction )
      {
         // If entry is in current transaction, then the constraints
         // are stricter. Disallow entries with endid lower then serial.
         partExpr.add("and");
         Expression subExpr = new Expression();
         subExpr.add(new ReferenceTerm(term,"persistence_txendid"));
         subExpr.add("<>");
         subExpr.add(new ConstantTerm(txSerial));
         subExpr.add("or");
         subExpr.add(new ReferenceTerm(term,"persistence_txend"));
         subExpr.add(">");
         subExpr.add(new ConstantTerm(serial));
         partExpr.add(subExpr);
      }
      timeExpr.add(partExpr);
      // Add all constraints
      expression.addAll(timeExpr);
   }

   public String toString()
   {
      return "["+serial+","+txSerial+","+applyTransaction+"]";
   }
}


