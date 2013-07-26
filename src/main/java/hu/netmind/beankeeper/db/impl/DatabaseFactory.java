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

import hu.netmind.beankeeper.service.ServiceFactory;
import hu.netmind.beankeeper.service.StoreContext;
import hu.netmind.beankeeper.service.Service;
import hu.netmind.beankeeper.common.StoreException;
import hu.netmind.beankeeper.db.*;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import org.apache.log4j.Logger;
import hu.netmind.beankeeper.config.ConfigurationTracker;
import hu.netmind.beankeeper.logging.SnapshotLogger;
import java.util.Map;

/**
 * Database factory determines which database instance to use,
 * and instantiates it.
 * @author Brautigam Robert
 * @version Revision: $Revision$
 */
public class DatabaseFactory implements ServiceFactory
{
   private static Logger logger = Logger.getLogger(DatabaseFactory.class);

   private Object instance = null;

   private ConfigurationTracker configurationTracker = null; // Injected
   private SnapshotLogger snapshotLogger = null; // Injected

   /**
    * Return the service instance created by this factory.
    */
   public Service getService()
   {
      return (Service) instance;
   }

   public void release()
   {
   }

   /**
    * Get the database instance for the datasource provided. This method
    * tries to guess the backend software specific settings and
    * algorithms, and instantiate a specific database implementation.
    * @return The database instance.
    */
   public void init(Map parameters)
   {
      try
      {
         // Get the datasource
         DataSource source = (DataSource) parameters.get(StoreContext.PARAM_DATASOURCE);
         if ( source == null )
         {
            String driver = (String) parameters.get(StoreContext.PARAM_DRIVERCLASS);
            String url = (String) parameters.get(StoreContext.PARAM_DRIVERURL);
            if ( (driver==null) || (url==null) )
               throw new StoreException("Database can not be initialized, no datasource or driver parameters given.");
            source = new DriverDataSource(driver,url);
         }
         // Determine database meta-data
         Connection conn = source.getConnection();
         DatabaseMetaData databaseMetaData = conn.getMetaData();
         logger.info("got data source to: "+databaseMetaData.getDatabaseProductName()+
               " ("+databaseMetaData.getDatabaseProductVersion()+") through driver: "+
               databaseMetaData.getDriverName()+" ("+databaseMetaData.getDriverVersion()+")");
         String databaseName = databaseMetaData.getDatabaseProductName();
         conn.close();
         // Create connection pool and add to parameters
         ConnectionSource connectionSource = new ConnectionSourceImpl(configurationTracker,snapshotLogger,source);
         parameters.put(StoreContext.PARAM_CONNECTIONSOURCE,connectionSource);
         // Create and return database implementation
         if ( databaseName.equalsIgnoreCase("postgresql") )
         {
            logger.debug("selecting postgres implementation.");
            instance = new PostgresDatabaseImpl();
         } else if ( databaseName.equalsIgnoreCase("mysql") ) {
            logger.debug("selecting mysql implementation.");
            instance = new MysqlDatabaseImpl();
         } else if ( databaseName.equalsIgnoreCase("oracle") ) {
            logger.debug("selecting oracle implementation.");
            instance = new OracleDatabaseImpl();
         } else if ( databaseName.equalsIgnoreCase("hsql database engine") ) {
            logger.debug("selecting hsql implementation.");
            instance = new HSQLDatabaseImpl();
         } else {
            logger.fatal("unknown database '"+databaseName+"', can not support promised features, so bailing out.");
            throw new StoreException("Unknown database encountered: "+databaseName+
                  ", cannot continue. For a list of supported databases consult the documentation.");
         }
      } catch ( StoreException e ) {
         throw e;
      } catch ( Exception e ) {
         throw new StoreException("could not instantiate database implementation",e);
      }
   }

}



