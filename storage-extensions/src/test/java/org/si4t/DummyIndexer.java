package org.si4t;

import com.tridion.configuration.Configuration;
import com.tridion.configuration.ConfigurationException;
import com.tridion.storage.si4t.BaseIndexData;
import com.tridion.storage.si4t.BinaryIndexData;
import com.tridion.storage.si4t.IndexingException;
import com.tridion.storage.si4t.SearchIndex;
import com.tridion.storage.si4t.SearchIndexData;

/**
 * DummyIndexer.
 */
public class DummyIndexer implements SearchIndex {

    private Configuration storageConfiguration;

    @Override
    public void configure(final Configuration configuration) throws ConfigurationException {
        this.storageConfiguration = configuration;
    }

    @Override
    public void addItemToIndex(final SearchIndexData data) throws IndexingException {

    }

    @Override
    public void removeItemFromIndex(final BaseIndexData data) throws IndexingException {

    }

    @Override
    public void updateItemInIndex(final SearchIndexData data) throws IndexingException {

    }

    @Override
    public void addBinaryToIndex(final BinaryIndexData data) throws IndexingException {

    }

    @Override
    public void removeBinaryFromIndex(final BaseIndexData data) throws IndexingException {

    }

    @Override
    public void commit(final String publicationId) throws IndexingException {

    }

    @Override
    public void destroy() {

    }
}
