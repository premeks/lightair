package net.sf.lightair.internal.dbunit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.sf.lightair.exception.CreateDatabaseConnectionException;
import net.sf.lightair.exception.DatabaseAccessException;
import net.sf.lightair.exception.DatabaseDriverClassNotFoundException;
import net.sf.lightair.internal.factory.Factory;
import net.sf.lightair.internal.properties.PropertiesProvider;
import net.sf.lightair.internal.properties.PropertyKeys;

import org.apache.commons.lang.time.StopWatch;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around DbUnit.
 */
public class DbUnitWrapper implements PropertyKeys {

	private final Logger log = LoggerFactory.getLogger(DbUnitWrapper.class);

	/**
	 * Create a DbUnit connection for a given schema.
	 * <p>
	 * Pass schema <code>null</code> to use the default schema from properties.
	 * 
	 * @param schemaName
	 *            Schema to connect to, or <code>null</code> to use default
	 *            schema
	 * @return DbUnit connection
	 * @throws DatabaseDriverClassNotFoundException
	 *             When database driver class cannot be loaded
	 * @throws CreateDatabaseConnectionException
	 *             When {@link DriverManager} cannot open connection to database
	 * @throws DatabaseAccessException
	 *             When DbUnit cannot establish itself on the database
	 *             connection, typically when schema does not exist
	 */
	public IDatabaseConnection createConnection(String schemaName)
			throws DatabaseDriverClassNotFoundException,
			CreateDatabaseConnectionException, DatabaseAccessException {
		if (null == schemaName) {
			schemaName = getProperty(DEFAULT_SCHEMA);
		}
		log.info("Creating database connection for schema {}", schemaName);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		String driverClassName = getProperty(DRIVER_CLASS_NAME);
		try {
			Class.forName(driverClassName);
			Connection connection = factory.getConnection(
					getProperty(CONNECTION_URL), getProperty(USER_NAME),
					getProperty(PASSWORD));
			IDatabaseConnection dbConnection = factory
					.createDatabaseConnection(connection, schemaName);
			stopWatch.stop();
			log.debug("Created database connection for schema {} in {} ms.",
					schemaName, stopWatch.getTime());
			setDatabaseDialect(dbConnection);
			setFeaturesAndProperties(dbConnection);
			return dbConnection;
		} catch (ClassNotFoundException e) {
			throw new DatabaseDriverClassNotFoundException(driverClassName, e);
		} catch (SQLException e) {
			throw new CreateDatabaseConnectionException(e);
		} catch (DatabaseUnitException e) {
			throw new DatabaseAccessException(e);
		}
	}

	private String getProperty(String key) {
		return propertiesProvider.getProperty(key);
	}

	private void setDatabaseDialect(IDatabaseConnection dbConnection) {
		DatabaseConfig config = dbConnection.getConfig();

		String dbUnitName = "http://www.dbunit.org/properties/datatypeFactory";
		String dialect = propertiesProvider.getProperty(DATABASE_DIALECT);
		String className = DATATYPE_FACTORIES.get(dialect);
		Object value = convertPropertyValue(className);
		log.debug(
				"Setting database dialect to {}, DbUnit datatype factory will be {}.",
				dialect, className);
		config.setProperty(dbUnitName, value);
	}

	private static final Map<String, String> DATATYPE_FACTORIES = new HashMap<String, String>();
	{
		DATATYPE_FACTORIES.put("h2", "org.dbunit.ext.h2.H2DataTypeFactory");
		DATATYPE_FACTORIES.put("oracle",
				"org.dbunit.ext.oracle.OracleDataTypeFactory");
		DATATYPE_FACTORIES.put("oracle9",
				"org.dbunit.ext.oracle.OracleDataTypeFactory");
		DATATYPE_FACTORIES.put("oracle10",
				"org.dbunit.ext.oracle.OracleDataTypeFactory");
		DATATYPE_FACTORIES.put("hsqldb",
				"org.dbunit.ext.hsqldb.HsqldbDataTypeFactory");
		DATATYPE_FACTORIES.put("mysql",
				"org.dbunit.ext.mysql.MySqlDataTypeFactory");
		DATATYPE_FACTORIES.put("db2", "org.dbunit.ext.db2.Db2DataTypeFactory");
		DATATYPE_FACTORIES.put("postgresql",
				"org.dbunit.dataset.datatype.DefaultDataTypeFactory");
		DATATYPE_FACTORIES.put("derby",
				"org.dbunit.dataset.datatype.DefaultDataTypeFactory");
		DATATYPE_FACTORIES.put("mssql",
				"org.dbunit.ext.mssql.MsSqlDataTypeFactory");
	}

	private void setFeaturesAndProperties(IDatabaseConnection dbConnection) {
		DatabaseConfig config = dbConnection.getConfig();

		Set<String> featureNames = propertiesProvider.getDbUnitFeatureNames();
		for (String featureName : featureNames) {
			String dbUnitName = "http://www.dbunit.org/features/"
					+ featureName.substring(16);
			Boolean value = Boolean.valueOf(propertiesProvider
					.getProperty(featureName));
			log.debug("Setting DbUnit feature {} to {}.", dbUnitName, value);
			config.setProperty(dbUnitName, value);
		}

		Set<String> propertyNames = propertiesProvider.getDbUnitPropertyNames();
		for (String propertyName : propertyNames) {
			String dbUnitName = "http://www.dbunit.org/properties/"
					+ propertyName.substring(18);
			String string = propertiesProvider.getProperty(propertyName);
			Object value = convertPropertyValue(string);
			log.debug("Setting DbUnit property {} to {}.", dbUnitName, value);
			config.setProperty(dbUnitName, value);
		}
	}

	private Object convertPropertyValue(String value) {
		try {
			return Class.forName(value).newInstance();
		} catch (Exception e) {
			return value;
		}
	}

	// beans and their setters;

	private PropertiesProvider propertiesProvider;

	/**
	 * Set property provider.
	 * 
	 * @param propertiesProvider
	 *            Property provider
	 */
	public void setPropertiesProvider(PropertiesProvider propertiesProvider) {
		this.propertiesProvider = propertiesProvider;
	}

	private Factory factory;

	/**
	 * Set factory.
	 * 
	 * @param factory
	 *            Factory
	 */
	public void setFactory(Factory factory) {
		this.factory = factory;
	}

}
