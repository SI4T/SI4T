using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Xml;
using Tridion.ContentManager.CommunicationManagement;
using Tridion.ContentManager.ContentManagement;

namespace SI4T.Templating
{
    /// <summary>
    /// Extension methods for TOM.NET classes to control indexing behaviour
    /// </summary>
    public static class ExtensionMethods
    {
        /// <summary>
        /// Check if an item (typically a page, CT or PT) is indexed by looking for standard metadata fields
        /// </summary>
        /// <param name="item">Item to be indexed</param>
        /// <returns>true if it should be indexed</returns>
        public static bool IsIndexed(this RepositoryLocalObject item)
        {
            if (item.Metadata != null)
            {
                string noIndex = GetFieldValue(item.Metadata,Constants.FIELD_NOINDEX);
                if (noIndex != null && noIndex.ToLower() == "yes")
                {
                    return false;
                }
            }
            //Recursive check for Structure Groups and Folders
            if (item is OrganizationalItem && item.OrganizationalItem != null && item.OrganizationalItem.Id.ItemId != item.Id.ItemId)
            {
                return item.OrganizationalItem.IsIndexed();
            }
            return true;
        }

        /// <summary>
        /// Check if a Component Presentation using a given CT should be indexed
        /// </summary>
        /// <param name="template">The CT used by the CP</param>
        /// <param name="minPriority">The minimum template priority to consider for indexing (default 200=medium)</param>
        /// <returns>true if it should be indexed</returns>
        public static bool IsIndexed(this ComponentTemplate template, int minPriority = 200)
        {
            //The logic for deciding to index a CP is likely to vary depending on the implementation
            //You can just not put the search indexing TBBs into the (dynamic) CTs that you do not 
            //want to index, but provided here is some additional default behaviour:

            //Only index the CP if its metadata allows so AND it is:
            //a) Not using a dynamic template
            //b) Using a dynamic template but its not possible to use it on a page
            //c) Using a dynamic template with above a certain priority (200 = medium)
            if (!template.IsRepositoryPublishable || template.AllowOnPage == false || template.Priority >= minPriority)
            {
                return (template as RepositoryLocalObject).IsIndexed();
            }
            return false;
        }

        /// <summary>
        /// Load field processor settings from template metadata
        /// </summary>
        /// <param name="template">The template to process</param>
        /// <returns>field processor setting specific to this template</returns>
        public static FieldProcessorSettings GetFieldProcessorSettings(this Template template)
        {
            FieldProcessorSettings settings = new FieldProcessorSettings();
            if (template.Metadata != null)
            {
                settings.SetFieldMap(GetFieldValue(template.Metadata,Constants.FIELD_CUSTOMFIELDMAP));
                settings.SetManagedFields(GetFieldValue(template.Metadata, Constants.FIELD_MANAGEDFIELDS));
                settings.SetLinkFieldsToEmbedFields(GetFieldValue(template.Metadata, Constants.FIELD_LINKFIELDSTOEMBED));
                string includeExclude = GetFieldValue(template.Metadata, Constants.FIELD_INCLUDEEXCLUDE);
                if (includeExclude!=null)
                {
                    if (includeExclude.ToLower().Contains(Constants.FIELDVALUE_INCLUDE))
                    {
                        settings.ExcludeByDefault = false;
                    }
                    else if (includeExclude.ToLower().Contains(Constants.FIELDVALUE_EXCLUDE))
                    {
                        settings.ExcludeByDefault = true;
                    }
                }
            }
            return settings;
        }

        private static string GetFieldValue(XmlElement fieldXml, string fieldname)
        {
            XmlNode node = fieldXml.SelectSingleNode(String.Format("//*[local-name()='{0}']", fieldname));
            return node == null ? null : node.InnerText;
        }
    }
}
