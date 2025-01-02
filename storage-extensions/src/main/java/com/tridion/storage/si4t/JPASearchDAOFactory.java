/**
 * Copyright 2011-2013 Radagio & SDL
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridion.storage.si4t;

import com.tridion.broker.StorageException;
import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.persistence.JPADAOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * An extended factory class responsible for deploying Tridion Items 
 * and indexing content. 
 *
 * Used in case Tridion JPA storage is configured in the storage layer. 
 * This class hooks into the Spring Loader, which is used by Tridion to
 * create JPA based DAO objects.
 *
 * @author R.S. Kempees
 */
@Component ("JPASearchDAOFactory")
@Qualifier ("JPASearchDAOFactory")
@Scope ("prototype")
// Primary hides the other JPA factories. Hence, see this.configure()
@Primary
public class JPASearchDAOFactory extends JPADAOFactory implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(JPASearchDAOFactory.class);
    private String storageId = "";
    private SearchIndexProcessor searchIndexProcessor;
    private boolean isExtendedDaoFactory = true;
    private static final String DAO_FACTORY_CLASS_ATTRIBUTE = "Class";

    /*
     * Spring specific, thanks to DN
     */
    private static ApplicationContext APPLICATION_CONTEXT;

    public JPASearchDAOFactory () {
        super(null, "MSSQL");// not important what we sent. This instance is
        // never going to be used
        LOG.trace("Spring Constructor init.");
    }

    /*
     * (non-Javadoc)
     * @see
     * com.tridion.storage.persistence.JPADAOFactory#configureBundle(com.tridion
     * .configuration.Configuration)
     */
    @Override
    public void configureBundle (Configuration storageDAOBundleConfiguration) throws ConfigurationException {
        // first set the right value for the private field called
        // 'applicationContext'.
        try {
            setPrivateField(this, APPLICATION_CONTEXT);
        } catch (IllegalAccessException e) {
            LOG.error(e.getMessage());
        }

        // configure the bundle like we normally do
        super.configureBundle(storageDAOBundleConfiguration);
    }

	/*
     * (non-Javadoc)
	 * @see
	 * org.springframework.context.ApplicationContextAware#setApplicationContext
	 * (org.springframework.context.ApplicationContext)
	 */

    @Override
    public void setApplicationContext (final ApplicationContext applicationContext) throws BeansException {
        APPLICATION_CONTEXT = applicationContext;
        LOG.trace("Setting app context from spring.");

    }

    private static void setPrivateField (final Object fieldOwner, final Object value) throws IllegalAccessException {
        final Field privateField = getPrivateFieldRec(fieldOwner.getClass(), "applicationContext");

        if (privateField != null) {

            final boolean accessible = privateField.isAccessible();
            privateField.setAccessible(true);

            privateField.set(fieldOwner, value);

            privateField.setAccessible(accessible);
        }
    }

    private static Field getPrivateFieldRec (final Class<?> clazz, final String fieldName) {
        for (Field field : clazz.getDeclaredFields()) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }
        final Class<?> superClazz = clazz.getSuperclass();

        if (superClazz != null) {
            return getPrivateFieldRec(superClazz, fieldName);
        }

        return null;
    }

    public JPASearchDAOFactory (String storageId, String dialect) {
        super(storageId, dialect);
        // Needed to correctly instantiate a searchindexer
        this.storageId = storageId;
        LOG.info("Storage Id is: {}",this.storageId);
    }

    /*
     * End Spring specific
     */
	/*
	 * Extension specific
	 */
	/*
	 * (non-Javadoc)
	 * @see com.tridion.storage.persistence.JPADAOFactory#configure(com.tridion.
	 * configuration.Configuration)
	 */
    @Override
    public void configure (Configuration storageDAOBundleConfiguration) throws ConfigurationException {
        super.configure(storageDAOBundleConfiguration);

        final String daoFactoryClassName = storageDAOBundleConfiguration.getAttribute(DAO_FACTORY_CLASS_ATTRIBUTE);

        if (daoFactoryClassName.equalsIgnoreCase(JPADAOFactory.class.getCanonicalName())) {
            LOG.info("This seems to be a normal JPADAOFactory ( {} ) for Storage Id: '{}', so not triggering and configuring the extension.", daoFactoryClassName, this.storageId);
            this.isExtendedDaoFactory = false;
            return;
        }


        // Get the instance here, because Spring instantiates the JPADAOFactory
        // twice.
        LOG.trace("Fetching SearchProcessor instance.");
        searchIndexProcessor = SearchIndexProcessor.getInstance();
        searchIndexProcessor.configureStorageInstance(storageId, storageDAOBundleConfiguration);
        LOG.trace("Instances of Search Index: ");
        searchIndexProcessor.logSearchIndexInstances();
    }

    /*
     * Overridden entry point for Tridion deploy commits
     *
     * (non-Javadoc)
     * @see
     * com.tridion.storage.persistence.JPADAOFactory#commitTransaction(java.
     * lang.String)
     */
    @Override
    public void commitTransaction (String transactionId) throws StorageException {
        try {
            if (this.isExtendedDaoFactory) {
                LOG.info("Start committing transaction: " + transactionId);
                long start = System.currentTimeMillis();
                super.commitTransaction(transactionId);
                long searchStart = System.currentTimeMillis();
                LOG.debug("Commit Indexing Start");
                searchIndexProcessor.triggerIndexing(transactionId, this.storageId);
                LOG.info("End committing transaction: " + transactionId);
                LOG.info("Committing Search took: " + (System.currentTimeMillis() - searchStart) + " ms.");
                LOG.info("Total Commit Time was: " + (System.currentTimeMillis() - start) + " ms.");
            } else {
                LOG.info("Not triggering any special stuff, as this instance for storage Id '{}' needs to behave like the normal DAOFactory",this.storageId);
                super.commitTransaction(transactionId);
            }
        } catch (StorageException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw e;
        } catch (IndexingException e) {
            LOG.error(e.getLocalizedMessage(), e);
            SearchIndexProcessor.cleanupRegister(transactionId, storageId);
            throw new StorageException(e);

        } finally {
            SearchIndexProcessor.debugLogRegister();
        }
    }

    @Override
    public void shutdownFactory () {
        super.shutdownFactory();
    }
}
