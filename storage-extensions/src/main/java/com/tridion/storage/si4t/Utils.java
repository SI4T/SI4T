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

import java.util.Arrays;
import java.util.regex.Pattern;


/**
 * Utils.
 * 
 * Helper functions
 * 
 * @author R.S. Kempees
 * @version 1.20
 * @since 1.00
 */
public class Utils
{

	public static String NEWLINE = System.getProperty("line.separator");

	public static String EscapeRegex(String input)
	{
		return input.replace("\\", "\\\\").replace("[", "\\[").replace("^", "\\^").replace("]", "\\]").replace("$", "\\$").replace(".", "\\.").replace("|", "\\|").replace("?", "\\?").replace("+", "\\+").replace("{", "\\{").replace("}", "\\}").replace("(", "\\(").replace(")", "\\)").replace("-", "\\-").replace("*", "\\*").replace("=", "\\=").replace("<", "\\<");
	}

	public static String UnescapeRegex(String input)
	{
		return input.replace("\\\\", "\\").replace("\\[", "[").replace("\\^", "^").replace("\\]", "]").replace("\\$", "$").replace("\\.", ".").replace("\\|", "|").replace("\\?", "?").replace("\\+", "+").replace("\\{", "{").replace("\\}", "}").replace("\\(", "(").replace("\\)", ")").replace("\\-", "-").replace("\\*", "*").replace("\\=", "=").replace("\\<", "<");
	}

	public static String RegexReplaceFirst(String pattern, String input, String replacement)
	{
		if (!StringIsNullOrEmpty(input) && !StringIsNullOrEmpty(pattern))
		{
			String i = EscapeRegex(input);
			i = Pattern.quote(i);
			String result = i.replaceFirst(pattern, EscapeRegex(replacement));
			result = UnescapeRegex(result);
			result = result.substring(2);
			return result.substring(0, result.lastIndexOf("\\E"));
		}
		if (!StringIsNullOrEmpty(input))
		{
			return input;
		}
		return "";
	}

	public static String RegexReplaceAll(String pattern, String input, String replacement)
	{
		if (!StringIsNullOrEmpty(input) && !StringIsNullOrEmpty(pattern))
		{
			String i = EscapeRegex(input);
			i = Pattern.quote(i);
			String result = i.replaceAll(pattern, EscapeRegex(replacement));
			result = UnescapeRegex(result);

			result = result.substring(2);
			return result.substring(0, result.lastIndexOf("\\E"));
		}
		if (!StringIsNullOrEmpty(input))
		{
			return input;
		}
		return "";
	}

	public static String GetBinaryFileExtension(String filepath)
	{
		int dotPos = filepath.lastIndexOf(".");
		if (dotPos < 0)
			return new String("");
		return filepath.substring(dotPos + 1);
	}

	public static String GetBinaryFileName(String filepath)
	{
		// Can't use File.pathSeparator as Tridion gives / in paths

		String separator = "";
		if (filepath.contains("\\"))
		{
			separator = "\\";
		}
		else if (filepath.contains("/"))
		{
			separator = "/";
		}
		else
		{
			return filepath;
		}

		int slashPos = filepath.lastIndexOf(separator);
		if (slashPos < 0)
			return filepath;

		String fullFileName = filepath.substring(slashPos + 1);

		return fullFileName.substring(0, fullFileName.lastIndexOf("."));
	}

	public static boolean StringIsNullOrEmpty(String input)
	{
		return !(input != null && input.trim().length() > 0);
	}

	public static boolean StringArrayContains(String[] stringArray, String testString)
	{
		if (stringArray == null || stringArray.length == 0 || Utils.StringIsNullOrEmpty(testString))
		{
			return false;
		}
		return Arrays.asList(stringArray).contains(testString);
	}

	public static String convertTransactionIdToPath(String id)
	{
		return id.replace(":", "_");
	}

	public static String stacktraceToString(StackTraceElement[] s)
	{
		StringBuilder toReturn = new StringBuilder();
		for (StackTraceElement e : s)
		{
			toReturn.append(e.getClassName() + " - " + e.getLineNumber() + " : " + e.getMethodName());
			toReturn.append(NEWLINE);
		}
		return toReturn.toString();
	}

	public static String GetItemIdFromFilepath(String filepath, String extension)
	{
		int slashPos = filepath.lastIndexOf("/");
		if (slashPos < 0)
			slashPos = 0;
		int dotPos = filepath.lastIndexOf(extension) - 1;
		if (dotPos < 0 || dotPos <= slashPos)
		{
			dotPos = filepath.length() - 1;
		}
		if (filepath.startsWith("/"))
			filepath = filepath.replaceFirst("/", "");
		return filepath.substring(slashPos, dotPos);
	}

	public static String RemoveLineBreaks(String input)
	{
		if (!Utils.StringIsNullOrEmpty(input))
		{
			return input.replaceAll(NEWLINE, "");
		}
		return input;
	}
}
