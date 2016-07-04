package com.tridion.storage.si4t.dao;

import com.tridion.broker.StorageException;
import com.tridion.storage.Schema;
import com.tridion.storage.persistence.JPASchemaDAO;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * storage-extensions
 *
 * @author R. Kempees
 * @since 03/12/14.
 */
public class JPASearchMetaDAO extends JPASchemaDAO {

	public JPASearchMetaDAO (final String storageId, final EntityManagerFactory entityManagerFactory, final String storageType) {
		super(storageId, entityManagerFactory, storageType);
	}

	public JPASearchMetaDAO (final String storageId, final EntityManagerFactory entityManagerFactory, final EntityManager entityManager, final String storageType) {
		super(storageId, entityManagerFactory, entityManager, storageType);
	}

	@Override public void remove (final int publicationId, final int schemaId) throws StorageException {
		super.remove(publicationId, schemaId);
	}

	@Override public void store (final Schema schema) throws StorageException {
		super.store(schema);
	}

	@Override public void update (final Schema schema) throws StorageException {
		super.update(schema);
	}
}
