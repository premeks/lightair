package net.sf.lightair.internal.junit;

import java.lang.reflect.Method;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.lightair.annotation.Setup;
import net.sf.lightair.internal.unitils.UnitilsWrapper;

public class SetupExecutor {

	private final Logger log = LoggerFactory.getLogger(SetupExecutor.class);

	public void execute(Setup setup, Method testMethod) {
		String[] fileNames = setup.value();
		String profile = setup.profile();
		log.info("Setting up database for test method {} " + "and profile {} with configured file names {}.",
				testMethod, profile, fileNames);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		unitilsWrapper.setup(testMethod, profile, fileNames);
		stopWatch.stop();
		log.debug("Database set up in {} ms.", stopWatch.getTime());
	}

	// beans and their setters:

	protected UnitilsWrapper unitilsWrapper;

	/**
	 * Set Unitils wrapper.
	 * 
	 * @param unitilsWrapper
	 *            Unitils wrapper
	 */
	public void setUnitilsWrapper(UnitilsWrapper unitilsWrapper) {
		this.unitilsWrapper = unitilsWrapper;
	}

}
