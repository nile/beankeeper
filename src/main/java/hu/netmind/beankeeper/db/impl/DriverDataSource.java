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

package hu.netmind.beankeeper.db.impl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.PrintWriter;
import hu.netmind.beankeeper.common.StoreException;

/**
 * This is a data source for backwards compatibility. Older JDBC implementations
 * only supported driver based connection establishing, this is a wrapper
 * to make data source out of a driver.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class DriverDataSource implements DataSource
{
   private String url;
   
   public DriverDataSource(String driverClass, String url)
   {
      try
      {
         // Register driver
         Class.forName(driverClass);
         // Remember url
         this.url=url;
      } catch ( Exception e ) {
         throw new StoreException("could not allocate driver",e);
      }
   }

   /**
    * Get the connection using the url specified in constructor.
    * @return A connection from the DriverManager.
    */
   public Connection getConnection()
      throws SQLException
   {
      return DriverManager.getConnection(url);
   }
   
   /**
    * Get the connection using the url specified in constructor.
    * @param params Additional parameters.
    * @return A connection from the DriverManager.
    */
   public Connection getConnection(String params)
      throws SQLException
   {
      return DriverManager.getConnection(url+";"+params);
   }
   
   /**
    * Get the connection using the url specified in constructor, with
    * username and password given.
    * @param username The username.
    * @param password The password.
    * @return A connection from the DriverManager.
    */
   public Connection getConnection(String username, String password)
      throws SQLException
   {
      return DriverManager.getConnection(url,username,password);
   }

   /**
    * Get the login timeout.
    */
   public int getLoginTimeout()
   {
      return DriverManager.getLoginTimeout();
   }

   /**
    * Get PrintWriter.
    */
   public PrintWriter getLogWriter()
   {
      return DriverManager.getLogWriter();
   }

   /**
    * Set the login timeout.
    */
   public void setLoginTimeout(int timeout)
   {
      DriverManager.setLoginTimeout(timeout);
   }

   /**
    * Set PrintWriter.
    */
   public void setLogWriter(PrintWriter writer)
   {
      DriverManager.setLogWriter(writer);
   }

   public boolean isWrapperFor(Class c)
   {
      return false;
   }

   public Object unwrap(Class c)
      throws SQLException
   {
      throw new SQLException("DriverDataSource is not a wrapper for a specific object.");
   }
}


