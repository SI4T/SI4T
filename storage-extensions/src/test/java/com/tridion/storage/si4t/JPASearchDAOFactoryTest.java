package com.tridion.storage.si4t;

import com.tridion.broker.StorageException;
import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.configuration.ConfigurationHelper;
import com.tridion.configuration.XMLConfigurationReader;
import com.tridion.storage.DAOFactory;
import com.tridion.storage.StorageConstants;
import com.tridion.storage.configuration.StorageFactoryConfigurationLoader;
import com.tridion.storage.management.StorageRegistry;
import com.tridion.storage.management.StorageRegistryFactory;
import com.tridion.storage.util.SpringContextDynamicConfigurer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * JPASearchDAOFactoryTest.
 */
public class JPASearchDAOFactoryTest {

    private static final String SPRING_APP_CONTEXT = "spring-app-context-test.xml";
    private static final String STORAGE_CONF_TEST = "cd_storage_conf.xml";

    private ClassPathXmlApplicationContext applicationContext;
    private StorageFactoryConfigurationLoader storageFactoryConfigurationLoader;
    private StorageRegistryFactory storageRegistryFactory;

    @Before
    public void setUp() {
        applicationContext = new ClassPathXmlApplicationContext(SPRING_APP_CONTEXT);
        storageFactoryConfigurationLoader = applicationContext.getBean(StorageFactoryConfigurationLoader.class);
        storageRegistryFactory = applicationContext.getBean(StorageRegistryFactory.class);
    }

    @After
    public void tearDown() {
        if (applicationContext != null && applicationContext.isActive()) {
            applicationContext.close();
        }
    }

    @Test
    public void testMultipleJPAFactories() throws StorageException {
        StorageRegistry registry = storageRegistryFactory.getRegistry("DAO_FACTORY_REGISTRY");
        assertEquals(2, registry.values().size());

        assertTrue(registry.contains("defaultdb"));
        assertTrue(registry.contains("otherdatastoreid"));

    }

    private DAOFactory initAndGetDaoFactory(String id, String configFile) throws ConfigurationException,
            StorageException {
        Configuration jpaConfig = getJPAConfig(id, configFile);
        SpringContextDynamicConfigurer.configureDatasource(applicationContext, jpaConfig);
        storageFactoryConfigurationLoader.configureStorage(jpaConfig, null);
        StorageRegistry<DAOFactory> daoFactoryRegistry =
                storageRegistryFactory.getRegistry(StorageConstants.DAO_FACTORY_REGISTRY);
        return daoFactoryRegistry.get(id);
    }


    private Configuration getJPAConfig(String storageID, String configFile) throws ConfigurationException {
        return ConfigurationHelper.getConfiguration(new XMLConfigurationReader().readConfiguration(configFile),
                "//Storage[@Id='" + storageID + "']");
    }
}