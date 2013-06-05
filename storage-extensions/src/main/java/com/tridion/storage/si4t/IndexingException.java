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

/**
 * IndexingException.
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class IndexingException extends Exception
{

	private static final long serialVersionUID = 7380395957713446857L;

	public IndexingException()
	{
		
	}

	public IndexingException(String message)
	{
		super(message);
	}

	public IndexingException(Throwable cause)
	{
		super(cause);
	}

	public IndexingException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
