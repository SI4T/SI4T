using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Web;
using System.Xml;
using Tridion.ContentManager.ContentManagement.Fields;
using Tridion.ContentManager.Templating;

namespace SI4T.Templating
{
    /// <summary>
    /// Generic Field Processing Class - all methods are public virtual to allow this 
    /// to be subclassed and behaviour altered/extended with ease
    /// </summary>
    public class FieldProcessor
    {
        public FieldProcessorSettings DefaultSettings { get; set; }
        public XmlDocument IndexData { get; set; }
        public string Title { get; set; }
        public int MinimumComponentTemplatePrio { get; set; }
        private List<string> _processedComponents = new List<string>();

        public FieldProcessor()
        {
            IndexData = new XmlDocument();
            IndexData.LoadXml("<data><body></body><custom></custom></data>");
            DefaultSettings = new FieldProcessorSettings { ExcludeByDefault = false, ManagedFields = new List<string>(), FieldMap = new Dictionary<string, IndexField>() };
        }

        /// <summary>
        /// Initialize settings from templating package variables
        /// </summary>
        /// <param name="package">The templating package</param>
        public virtual void Initialize(Package package)
        {
            string excludeFields = package.GetValue(Constants.FIELD_INCLUDEEXCLUDE);
            this.DefaultSettings.ExcludeByDefault = (excludeFields!=null && excludeFields.ToLower().Contains(Constants.FIELDVALUE_EXCLUDE)) ? true : false;
            this.DefaultSettings.ManagedFields = package.GetValue(Constants.FIELD_MANAGEDFIELDS) == null ? new List<string>() : package.GetValue(Constants.FIELD_MANAGEDFIELDS).Split(',').ToList();
            this.DefaultSettings.SetFieldMap(package.GetValue(Constants.FIELD_CUSTOMFIELDMAP));
            this.DefaultSettings.SetLinkFieldsToEmbedFields(package.GetValue(Constants.FIELD_LINKFIELDSTOEMBED));
            string prioString = package.GetValue(Constants.FIELD_MIN_CT_PRIO);
            int prio = 0;
            if (Int32.TryParse(prioString, out prio))
            {
                MinimumComponentTemplatePrio = prio;
            }
            else
            {
                MinimumComponentTemplatePrio = 0;
            }
        }

        /// <summary>
        /// Mark a component as being processed - to avoid processing the same component twice
        /// </summary>
        /// <param name="componentUri">the component tcm uri</param>
        public virtual void SetComponentAsProcessed(string componentUri)
        {
            if (!_processedComponents.Contains(componentUri))
            {
                _processedComponents.Add(componentUri);
            }
        }

        /// <summary>
        /// Check if a component has already been processed - to avoid processing the same component twice
        /// </summary>
        /// <param name="componentUri">the component tcm uri</param>
        /// <returns>true if component has previously been marked as processed</returns>
        public virtual bool IsComponentAlreadyProcessed(string componentUri)
        {
            return _processedComponents.Contains(componentUri);
        }


        /// <summary>
        /// Process field data building up index data in XML format
        /// </summary>
        /// <param name="fields">Fields to process</param>
        /// <param name="settings">Custom settings for this set of fields (if not null overrides default settings)</param>
        public virtual void ProcessData(ItemFields fields, FieldProcessorSettings settings = null)
        {
            if (settings == null)
            {
                settings = DefaultSettings;
            }
            //use defaults if fields are not explicitly included/excluded
            if (settings.ExcludeByDefault == null)
            {
                settings.ExcludeByDefault = DefaultSettings.ExcludeByDefault;
                if (settings.ManagedFields == null)
                {
                    settings.ManagedFields = DefaultSettings.ManagedFields;
                }
            }
            if (settings.ManagedFields == null)
            {
                settings.ManagedFields = new List<string>();
            }
            if (settings.FieldMap == null)
            {
                settings.FieldMap = DefaultSettings.FieldMap;
            }
            if (settings.LinkFieldsToEmbed == null)
            {
                settings.LinkFieldsToEmbed = DefaultSettings.LinkFieldsToEmbed;
            }
            ProcessFields(fields, settings);
        }

        /// <summary>
        /// Add custom field values to the index data XML
        /// </summary>
        /// <param name="fieldName">Custom field name</param>
        /// <param name="values">values to add</param>
        /// <param name="encoded">set to true if values are already XML encoded (for example RTF content)</param>
        public virtual void SetCustomFieldValues(string fieldName, List<object> values, bool encoded = false)
        {
            foreach (var val in values)
            {
                IndexData.SelectSingleNode("/*/*[local-name()='custom']").AppendChild(CreateElement(fieldName,encoded,val.ToString()));
            }
        }

        /// <summary>
        /// Add custom field value to the index data XML
        /// </summary>
        /// <param name="fieldName">Custom field name</param>
        /// <param name="values">value to add</param>
        /// <param name="encoded">set to true if values are already XML encoded (for example RTF content)</param>
        public virtual void SetCustomFieldValue(string fieldName, object value, bool encoded = false)
        {
            SetCustomFieldValues(fieldName, new List<object> { value }, encoded);
        }

        /// <summary>
        /// Get the catch all default index data element
        /// </summary>
        /// <returns></returns>
        public virtual XmlElement GetCatchAllElement()
        {
            return IndexData.SelectSingleNode("/*/*[local-name()='body']") as XmlElement;
        }

        /// <summary>
        /// Get a custom index data element
        /// </summary>
        /// <returns></returns>
        public virtual XmlElement GetCustomElement()
        {
            return IndexData.SelectSingleNode("/*/*[local-name()='custom']") as XmlElement;
        }

        /// <summary>
        /// Process item fields
        /// </summary>
        /// <param name="fields">Fields to process</param>
        /// <param name="settings">Field processor settings</param>
        public virtual void ProcessFields(ItemFields fields, FieldProcessorSettings settings)
        {
            foreach (var field in fields)
            {
                if (field is EmbeddedSchemaField)
                {
                    foreach (var subfields in (field as EmbeddedSchemaField).Values)
                    {
                        ProcessFields(subfields, settings);
                    }
                }
                else
                {
                    //A field is indexed if we are excluding by default and the field is in the set of managed fields,
                    //OR we are including by default and the field is NOT in the set of managed fields
                    //OR there is a mapping for it in the custom field map
                    if ((settings.ExcludeByDefault == settings.ManagedFields.Contains(field.Name)) || settings.FieldMap.ContainsKey(field.Name))
                    {
                        ProcessField(field, settings);
                    }
                }
            }
        }

        /// <summary>
        /// Process an individual field
        /// </summary>
        /// <param name="field">The field to process</param>
        /// <param name="settings">field processor settings</param>
        public virtual void ProcessField(ItemField field, FieldProcessorSettings settings)
        {
            if (settings.HasCustomMapping(field.Name))
            {
                ProcessCustomField(field, settings);
            }
            else
            {
                ProcessCatchAllField(field, settings);
            }
        }

        /// <summary>
        /// Process a field into the catchall index data
        /// </summary>
        /// <param name="field">The field to process</param>
        /// <param name="settings">field processor settings</param>
        public virtual void ProcessCatchAllField(ItemField field, FieldProcessorSettings settings = null)
        {
            //only process text fields
            if (field is TextField)
            {
                AddToData(((TextField)field).Values, (field is XhtmlField));
            }
            if (field is ComponentLinkField && settings.IsLinkToBeFollowed(field.Name))
            {
                foreach (var comp in ((ComponentLinkField)field).Values)
                {
                    //avoid circular links, and indexing the items that are linked more than once
                    if (!IsComponentAlreadyProcessed(comp.Id))
                    {
                        SetComponentAsProcessed(comp.Id);
                        ProcessData(new ItemFields(comp.Content, comp.Schema), settings);
                    }
                }
            }
        }

        /// <summary>
        /// Process a field into a custom index data element
        /// </summary>
        /// <param name="field">The field to process</param>
        /// <param name="settings">field processor settings</param>
        public virtual void ProcessCustomField(ItemField field, FieldProcessorSettings settings = null)
        {
            IndexField targetField = settings.CustomFieldTarget(field.Name);
            //indexing behaviour is more specific for custom fields:
            //1. For keyword or component link fields, the kw or linked component item id is indexed
            //2. For date fields, the date (in standard format) is indexed
            //3. For number fields the number is indexed
            //4. For text fields, the text content (no markup) is indexed
            IList<string> values = new List<string>();
            if (field is KeywordField)
            {
                values = ((KeywordField)field).Values.Select(k => k.Id.ItemId.ToString()).ToList();
            }
            else if (field is ComponentLinkField)
            {
                values = ((ComponentLinkField)field).Values.Select(c => c.Id.ItemId.ToString()).ToList();
            }
            else if (field is DateField)
            {
                values = ((DateField)field).Values.Select(d => d.ToString("o") + "Z").ToList();
            }
            else if (field is NumberField)
            {
                values = ((NumberField)field).Values.Select(n => n.ToString()).ToList();
            }
            else if (field is TextField)
            {
                values = ((TextField)field).Values;
            }
            AddToData(values, (field is XhtmlField), targetField);
        }

        /// <summary>
        /// Add field values into the index data 
        /// </summary>
        /// <param name="values">Values to add</param>
        /// <param name="encoded">set to true if values are already XML encoded (for example RTF content)</param>
        /// <param name="targetField">Configuration for the target (search index) field</param>
        public virtual void AddToData(IList<string> values, bool encoded, IndexField targetField = null)
        {
            foreach (var value in values)
            {
                bool processed = false;
                //Title is a special case
                if (targetField!=null && targetField.Name == "title")
                {
                    if (String.IsNullOrEmpty(this.Title))
                    {
                        this.Title = value;
                        processed = true;
                    }
                }
                else if (targetField != null && (targetField.IsMultiValue || IndexData.SelectSingleNode("/*/*[local-name()='custom']/*[local-name()='" + targetField.Name + "']") == null))
                {
                    SetCustomFieldValue(targetField.Name, value, encoded);
                    processed = true;
                }
                if (!processed)
                {
                    IndexData.SelectSingleNode("*/*[local-name()='body']").AppendChild(CreateElement(null, encoded, value));
                }
            }
        }

        /// <summary>
        /// Convert XHTML to text
        /// </summary>
        /// <param name="xhtml">XHTML in string format</param>
        /// <returns>text without HTML tags and sentence ends added for line breaks closing divs and paragraphs</returns>
        public virtual string XhtmlToText(string xhtml)
        {
            string res = Regex.Replace(xhtml, "</td>|</th>", " ");
            res = Regex.Replace(xhtml, "</tr>|</p>|</div>|<br/>", ". ");
            res = Regex.Replace(res, @"\<[^\>]*\>", "");
            res = Regex.Replace(res, @"\s+", " ");
            return HttpUtility.HtmlDecode(res.Trim());
        }

        public virtual XmlNode CreateElement(string targetFieldName, bool encoded, string value)
        {
            if (encoded)
            {
                value = XhtmlToText(value);
            }
            if (targetFieldName == null)
            {
                return IndexData.CreateTextNode(value + ". ");
            }
            else
            {
                XmlElement field = IndexData.CreateElement(targetFieldName);
                field.InnerText = value;
                return field;
            }
        }
    }
}
