using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;
using Tridion.ContentManager.ContentManagement.Fields;
using Tridion.ContentManager.Templating;

namespace SI4T.Templating
{
    public class FieldProcessor
    {
        public FieldProcessorSettings DefaultSettings { get; set; }
        public XmlDocument IndexData { get; set; }
        public string Title { get; set; }
        public int MinimumComponentTemplatePrio { get; set; }

        public FieldProcessor()
        {
            IndexData = new XmlDocument();
            IndexData.LoadXml("<data><body></body><custom></custom></data>");
            DefaultSettings = new FieldProcessorSettings { ExcludeByDefault = false, ManagedFields = new List<string>(), FieldMap = new Dictionary<string, string>() };
        }

        public void Initialize(Package package)
        {
            string excludeFields = package.GetValue(Constants.PACKAGE_ITEM_EXCLUDE_FIELDS_BY_DEFAULT);
            this.DefaultSettings.ExcludeByDefault = (excludeFields!=null && excludeFields.ToLower() == "true") ? true : false;
            this.DefaultSettings.ManagedFields = package.GetValue(Constants.PACKAGE_ITEM_MANAGED_FIELDS) == null ? new List<string>() : package.GetValue(Constants.PACKAGE_ITEM_MANAGED_FIELDS).Split(',').ToList();
            this.DefaultSettings.SetFieldMap(package.GetValue(Constants.PACKAGE_ITEM_CUSTOMFIELDS));
            string prioString = package.GetValue(Constants.PACKAGE_ITEM_MIN_TEMPLATE_PRIO_FOR_INDEXING);
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

        public void ProcessData(ItemFields fields, FieldProcessorSettings settings)
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
            ProcessFields(fields, settings);
        }

        public void SetCustomFields(string fieldName, List<object> values)
        {
            foreach (var val in values)
            {
                XmlElement custom = IndexData.CreateElement(fieldName);
                custom.InnerText = val.ToString();
                IndexData.SelectSingleNode("/data/custom").AppendChild(custom);
            }
        }

        public XmlElement GetCatchAllElement()
        {
            return IndexData.SelectSingleNode("/data/body") as XmlElement;
        }

        public XmlElement GetCustomElement()
        {
            return IndexData.SelectSingleNode("/data/custom") as XmlElement;
        }


        protected void ProcessFields(ItemFields fields, FieldProcessorSettings settings)
        {
            foreach (var field in fields)
            {
                //A field is indexed if we are excluding by default and the field is in the set of managed fields,
                //OR we are including by default and the field is NOT in the set of managed fields
                if ((settings.ExcludeByDefault == settings.ManagedFields.Contains(field.Name)))
                {
                    if (field is TextField)
                    {
                        ProcessField(field, settings);
                    }
                    if (field is EmbeddedSchemaField)
                    {
                        foreach (var subfields in (field as EmbeddedSchemaField).Values)
                        {
                            ProcessFields(subfields, settings);
                        }
                    }
                }
            }
        }

        protected void ProcessField(ItemField field, FieldProcessorSettings settings)
        {
            bool encoded = false;
            if (field is XhtmlField)
            {
                encoded = true;
            }
            if (field is TextField)
            {
                ProcessTextField(field as TextField, encoded, settings);
            }
        }

        protected void ProcessTextField(TextField textField, bool encoded, FieldProcessorSettings settings)
        {
            foreach (var value in textField.Values)
            {
                AddToData(textField.Name, value, encoded, settings);
            }
        }

        protected void AddToData(string fieldname, string value, bool encoded, FieldProcessorSettings settings)
        {
            bool processed = false;
            if (settings.FieldMap.ContainsKey(fieldname))
            {
                string targetFieldName = settings.FieldMap[fieldname];
                //Title is a special case
                if (targetFieldName == "title")
                {
                    if (String.IsNullOrEmpty(this.Title))
                    {
                        this.Title = value;
                        processed = true;
                    }
                }
                else if (IndexData.SelectSingleNode("/data/custom/" + targetFieldName) == null)
                {
                    IndexData.SelectSingleNode("/data/custom").AppendChild(CreateElement(targetFieldName, encoded, value));
                    processed = true;
                }
            }
            if (!processed)
            {
                IndexData.SelectSingleNode("data/body").AppendChild(CreateElement("p",encoded,value));
            }
        }

        protected XmlElement CreateElement(string targetFieldName, bool encoded, string value)
        {
            XmlElement field = IndexData.CreateElement(targetFieldName);
            if (encoded)
            {
                field.InnerXml = XhtmlToText(value);
            }
            else
            {
                field.InnerText = value;
            }
            return field;
        }

        public static string XhtmlToText(string xhtml)
        {
            string res = Regex.Replace(xhtml, "</p>|</div>|<br/>", ". ");
            res = Regex.Replace(res,@"\<[^\>]*\>", "");
            res = Regex.Replace(res,@"\s+", " ");
            return res;
        }
    }

    public class FieldProcessorSettings
    {
        public bool? ExcludeByDefault { get; set; }
        public List<string> ManagedFields { get; set; }
        public Dictionary<string, string> FieldMap { get; set; }
        public void SetManagedFields(string managedFields)
        {
            if (managedFields != null)
            {
                ManagedFields = new List<string>();
                foreach (string token in managedFields.Split(','))
                {
                    ManagedFields.Add(token.Trim());
                }
            }
        }
        public void SetFieldMap(string customFields)
        {
            if (customFields != null)
            {
                FieldMap = new Dictionary<string, string>();
                foreach (string token in customFields.Split('|'))
                {
                    string[] items = token.Split(':');
                    string target = items[0];
                    if (items.Length > 1)
                    {
                        foreach (string source in items[1].Split(','))
                        {
                            if (!FieldMap.ContainsKey(source))
                            {
                                FieldMap.Add(source, target);
                            }
                        }
                    }
                    else
                    {
                        FieldMap.Add(target, target);
                    }
                }
            }
        }
    }
}
