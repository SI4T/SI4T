/**
 * Copyright 2011-2013 Radagio & SDL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tridion.storage.si4t;

import org.slf4j.Logger;

import com.tridion.storage.BinaryContent;
import com.tridion.storage.services.LocalThreadTransaction;

/**
 * TridionBinaryProcessor.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class TridionBinaryProcessor extends TridionBaseItemProcessor
{

	/**
	 * Register addition of a Binary item.
	 * 
	 * @param binaryContent
	 * @param originalRelativePath
	 * @param newRelativePath
	 * @param log
	 * @param storageId
	 */
	public static void registerAddition(BinaryContent binaryContent, String originalRelativePath, String newRelativePath, Logger log, String storageId)
	{
		String indexId = "binary:" + Integer.toString(binaryContent.getPublicationId()) + "-" + Integer.toString(binaryContent.getBinaryId());
		String fileSize = Integer.toString(binaryContent.getObjectSize());
		String path = originalRelativePath;
		String fileExtension = Utils.GetBinaryFileExtension(newRelativePath);
		BinaryIndexData data = new BinaryIndexData(FactoryAction.PERSIST, IndexType.BINARY, Integer.toString(binaryContent.getPublicationId()), storageId);

		data.setContent(binaryContent);
		data.setUniqueIndexId(indexId);
		data.setFileName(Utils.GetBinaryFileName(path) + "." + Utils.GetBinaryFileExtension(path));
		data.setFileSize(fileSize);
		data.setFileType(fileExtension);
		data.setIndexUrl(newRelativePath);
		SearchIndexProcessor.registerAction(LocalThreadTransaction.getTransactionId(), data, log);
	}	
}
