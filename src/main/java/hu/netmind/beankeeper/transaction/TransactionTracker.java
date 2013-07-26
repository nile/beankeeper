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

import hu.netmind.beankeeper.service.Service;
import java.util.LinkedList;
import java.util.Iterator;
import java.sql.Connection;
import org.apache.log4j.Logger;
import java.util.HashSet;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * This service is responsible for keeping track of transactions currently
 * running in the system.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public interface TransactionTracker extends Service
{
   int TX_REQUIRED = 1;
   int TX_NEW = 2;
   int TX_OPTIONAL = 3;

   /**
    * Get a transaction. Following modes are supported:
    * <ul>
    *    <li>TX_REQUIRED: A new transaction is allocated if no current
    *    transaction exists, otherwise the current transaction is returned.</li>
    *    <li>TX_NEW: A new transaction is allocated either way.</li>
    *    <li>TX_OPTIONAL: If there is a current transaction, that is returned,
    *    null otherwise.</li>
    * </ul>
    * Note, that each transaction can support multiple levels of begin-commit
    * blocks. Each transaction only commits/rollsback is the most outer
    * block is commited/rolled back.
    */
   Transaction getTransaction(int mode);

   /**
    * Determines whether the given transaction is a valid open transaction
    * in this tracker or not.
    */
   boolean hasTransaction(Long serial);
}


